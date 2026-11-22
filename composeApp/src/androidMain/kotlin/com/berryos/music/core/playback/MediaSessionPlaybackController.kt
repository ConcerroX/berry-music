package com.berryos.music.core.playback

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.berryos.music.core.model.SourceKey
import com.berryos.music.core.model.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MediaSessionPlaybackController(context: Context) : PlaybackController {
    private val appContext = context.applicationContext
    private val sessionToken = SessionToken(
        appContext,
        ComponentName(appContext, PlaybackService::class.java),
    )
    private val controllerFuture: ListenableFuture<MediaController> =
        MediaController.Builder(appContext, sessionToken).buildAsync()

    private var controller: MediaController? = null
    private var tracks: List<Track> = emptyList()
    private var entryIds: List<Long> = emptyList()
    private var nextEntryId = 0L
    private var source: PlaybackSource = PlaybackSource.Queue
    private var pendingPlay: PendingPlay? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionJob: Job? = null
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()
    private val _position = MutableStateFlow(PlaybackPosition())
    override val position: StateFlow<PlaybackPosition> = _position.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState()
            updatePosition()
            syncPositionUpdates()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateState()
            updatePosition()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            updatePosition()
        }
    }

    init {
        controllerFuture.addListener(
            {
                val mediaController = controllerFuture.get()
                controller = mediaController
                mediaController.addListener(listener)
                pendingPlay?.let { play(it.tracks, it.startIndex, it.source) }
                pendingPlay = null
                updateState()
                updatePosition()
                syncPositionUpdates()
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    override fun play(tracks: List<Track>, startIndex: Int, source: PlaybackSource) {
        val mediaController = controller
        if (mediaController == null) {
            pendingPlay = PendingPlay(tracks, startIndex, source)
            return
        }
        val playable = tracks.filter { it.source == SourceKey.Local || it.streamUrl != null }
        if (playable.isEmpty()) return
        val index = startIndex.coerceIn(0, playable.lastIndex)
        if (playable == this.tracks && source == this.source) {
            // Replaying the same collection keeps the queue identity, so queue entry ids and the
            // list keys built from them stay stable instead of rebuilding every row.
            mediaController.seekTo(index, 0L)
            mediaController.play()
        } else {
            this.tracks = playable
            this.entryIds = playable.map { nextEntryId++ }
            this.source = source
            mediaController.setMediaItems(playable.map(::playbackMediaItem), index, 0L)
            mediaController.prepare()
            mediaController.play()
        }
        updateState()
        updatePosition()
    }

    override fun playNext(track: Track) {
        val mediaController = controller ?: return
        if (track.source == SourceKey.Local || track.streamUrl != null) {
            val currentIndex = mediaController.currentMediaItemIndex
            if (tracks.isEmpty() || currentIndex !in tracks.indices) {
                play(listOf(track), 0, PlaybackSource.Queue)
            } else {
                val insertIndex = (currentIndex + 1).coerceAtMost(tracks.size)
                tracks = tracks.toMutableList().apply { add(insertIndex, track) }
                entryIds = entryIds.toMutableList().apply { add(insertIndex, nextEntryId++) }
                source = PlaybackSource.Queue
                mediaController.addMediaItem(insertIndex, playbackMediaItem(track))
                updateState()
                updatePosition()
            }
        }
    }

    override fun togglePlayPause() {
        val mediaController = controller ?: return
        if (mediaController.playWhenReady) mediaController.pause() else mediaController.play()
    }

    override fun next() {
        controller?.seekToNextMediaItem()
        controller?.play()
    }

    override fun previous() {
        controller?.seekToPreviousMediaItem()
        controller?.play()
    }

    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        updatePosition()
    }

    fun release() {
        positionJob?.cancel()
        scope.cancel()
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
        controller = null
    }

    private fun updateState() {
        val mediaController = controller ?: return
        _state.value = PlaybackState(
            queue = tracks,
            entryIds = entryIds,
            currentIndex = mediaController.currentMediaItemIndex,
            // The intent, not the transient isPlaying flag: buffering after a seek would
            // otherwise flip the icon and the header back to the paused state.
            isPlayRequested = mediaController.playWhenReady && mediaController.playbackState != Player.STATE_ENDED,
            source = source,
        )
    }

    private fun updatePosition() {
        val mediaController = controller ?: return
        val duration = mediaController.duration
            .takeIf { it != C.TIME_UNSET && it > 0L }
            ?: tracks.getOrNull(mediaController.currentMediaItemIndex)?.durationMs
            ?: 0L
        _position.value = PlaybackPosition(
            positionMs = mediaController.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
        )
    }

    /** Media3 has no position callback, so the position is sampled while playback runs. */
    private fun syncPositionUpdates() {
        val mediaController = controller ?: return
        if (mediaController.playWhenReady && mediaController.playbackState != Player.STATE_ENDED) {
            if (positionJob == null) {
                positionJob = scope.launch {
                    while (isActive) {
                        updatePosition()
                        delay(POSITION_INTERVAL_MILLIS)
                    }
                }
            }
        } else {
            positionJob?.cancel()
            positionJob = null
        }
    }

    companion object {
        private const val POSITION_INTERVAL_MILLIS = 500L
    }

    private data class PendingPlay(val tracks: List<Track>, val startIndex: Int, val source: PlaybackSource)
}

private fun playbackMediaItem(track: Track): MediaItem = MediaItem.Builder()
    .setMediaId("${track.source}:${track.id}")
    .setUri(track.streamUrl?.let(Uri::parse) ?: localUri(track))
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setDurationMs(track.durationMs)
            .setArtworkUri(track.coverUrl?.let(Uri::parse))
            .build(),
    )
    .build()

private fun localUri(track: Track): Uri {
    val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
    return ContentUris.withAppendedId(base, track.id.toLong())
}

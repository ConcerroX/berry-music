package com.berryos.music.core.playback

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.berryos.music.core.model.SourceKey
import com.berryos.music.core.model.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private var pendingPlay: Pair<List<Track>, Int>? = null
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateState()
        }
    }

    init {
        controllerFuture.addListener(
            {
                val mediaController = controllerFuture.get()
                controller = mediaController
                mediaController.addListener(listener)
                pendingPlay?.let { (queuedTracks, index) -> play(queuedTracks, index) }
                pendingPlay = null
                updateState()
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    override fun play(tracks: List<Track>, startIndex: Int) {
        val mediaController = controller
        if (mediaController == null) {
            pendingPlay = tracks to startIndex
            return
        }
        val playable = tracks.filter { it.source == SourceKey.Local || it.streamUrl != null }
        if (playable.isEmpty()) return
        this.tracks = playable
        val items = playable.map { track ->
            MediaItem.Builder()
                .setMediaId("${track.source}:${track.id}")
                .setUri(track.streamUrl?.let(Uri::parse) ?: localUri(track))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setDurationMs(track.durationMs)
                        .setArtworkUri(
                            if (track.source == SourceKey.Local) albumArtUri(track.albumId) else null,
                        )
                        .build(),
                )
                .build()
        }
        val index = startIndex.coerceIn(0, items.lastIndex)
        mediaController.setMediaItems(items, index, 0L)
        mediaController.prepare()
        mediaController.play()
        updateState()
    }

    override fun togglePlayPause() {
        val mediaController = controller ?: return
        if (mediaController.isPlaying) mediaController.pause() else mediaController.play()
    }

    override fun next() {
        controller?.seekToNextMediaItem()
        controller?.play()
    }

    override fun previous() {
        controller?.seekToPreviousMediaItem()
        controller?.play()
    }

    fun release() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
        controller = null
    }

    private fun updateState() {
        val mediaController = controller ?: return
        _state.value = PlaybackState(
            queue = tracks,
            currentIndex = mediaController.currentMediaItemIndex,
            isPlaying = mediaController.isPlaying,
        )
    }

    private fun localUri(track: Track): Uri {
        val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        return ContentUris.withAppendedId(base, track.id.toLong())
    }

    private fun albumArtUri(albumId: String): Uri =
        ContentUris.withAppendedId(Uri.parse(ALBUM_ART_URI), albumId.toLong())

    companion object {
        private const val ALBUM_ART_URI = "content://media/external/audio/albumart"
    }
}

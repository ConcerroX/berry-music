package com.berryos.music.core.playback

import com.berryos.music.core.model.Track
import kotlinx.coroutines.flow.StateFlow

/** Where the current queue came from; the id is kept for titles, restore, and deep links. */
sealed interface PlaybackSource {
    data object Queue : PlaybackSource
    data object Songs : PlaybackSource
    data object Search : PlaybackSource
    data class Album(val albumId: String) : PlaybackSource
    data class Playlist(val playlistId: String) : PlaybackSource
    data class Artist(val artistId: String) : PlaybackSource
}

data class PlaybackState(
    val queue: List<Track> = emptyList(),
    /** Unique per queue entry, so a track added twice still has two distinct list identities. */
    val entryIds: List<Long> = emptyList(),
    val currentIndex: Int = -1,
    val isPlayRequested: Boolean = false,
    val source: PlaybackSource = PlaybackSource.Queue,
) {
    val currentTrack: Track?
        get() = queue.getOrNull(currentIndex)
}

/** How a page's playback object relates to the current playback session. */
enum class PlaybackStatus {
    Playing,
    Paused,

    /** The session belongs to another object; the current track's membership does not matter. */
    NotPlaying,
}

fun PlaybackState.playbackStatus(source: PlaybackSource): PlaybackStatus = when {
    this.source != source -> PlaybackStatus.NotPlaying
    isPlayRequested -> PlaybackStatus.Playing
    else -> PlaybackStatus.Paused
}

/**
 * Playhead position and track duration, sampled separately so the high-frequency updates stay out
 * of [PlaybackState].
 */
data class PlaybackPosition(val positionMs: Long = 0L, val durationMs: Long = 0L)

interface PlaybackController {
    val state: StateFlow<PlaybackState>
    val position: StateFlow<PlaybackPosition>

    fun play(tracks: List<Track>, startIndex: Int, source: PlaybackSource = PlaybackSource.Queue)
    fun playNext(track: Track)
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
}

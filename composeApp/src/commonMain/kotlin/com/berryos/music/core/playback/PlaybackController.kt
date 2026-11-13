package com.berryos.music.core.playback

import com.berryos.music.core.model.Track
import kotlinx.coroutines.flow.StateFlow

data class PlaybackState(
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
) {
    val currentTrack: Track?
        get() = queue.getOrNull(currentIndex)
}

interface PlaybackController {
    val state: StateFlow<PlaybackState>

    fun play(tracks: List<Track>, startIndex: Int)
    fun togglePlayPause()
    fun next()
    fun previous()
}

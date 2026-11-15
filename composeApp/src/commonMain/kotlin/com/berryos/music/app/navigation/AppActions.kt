package com.berryos.music.app.navigation

import com.berryos.music.core.model.Track
import com.berryos.music.core.playback.PlaybackSource

class AppActions(
    val open: (AppRoute) -> Unit,
    val back: () -> Unit,
    val play: (List<Track>, Int, PlaybackSource) -> Unit,
    val playNext: (Track) -> Unit,
    val togglePlayPause: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
    val seekTo: (Long) -> Unit,
)

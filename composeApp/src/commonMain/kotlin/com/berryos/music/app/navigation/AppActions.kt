package com.berryos.music.app.navigation

import com.berryos.music.core.model.Track

class AppActions(
    val open: (AppRoute) -> Unit,
    val back: () -> Unit,
    val play: (List<Track>, Int) -> Unit,
    val togglePlayPause: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
)

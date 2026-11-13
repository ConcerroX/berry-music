package com.berryos.music.feature.playlist

import androidx.compose.runtime.Composable
import com.berryos.music.core.designsystem.PlaceholderScreen

@Composable
fun PlaylistScreen(playlistId: String) {
    PlaceholderScreen(title = "Playlist", subtitle = "Playlist id: $playlistId")
}

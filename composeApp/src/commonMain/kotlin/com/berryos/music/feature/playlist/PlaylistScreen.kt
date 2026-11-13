package com.berryos.music.feature.playlist

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.PlaceholderScreen

@Composable
fun PlaylistScreen(playlistId: String, actions: AppActions) {
    PlaceholderScreen(title = "Playlist", subtitle = "Playlist id: $playlistId") {
        TextButton(onClick = actions.back) { Text("Back") }
    }
}

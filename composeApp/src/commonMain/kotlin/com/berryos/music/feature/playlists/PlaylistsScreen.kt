package com.berryos.music.feature.playlists

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.PlaylistRoute
import com.berryos.music.core.designsystem.PlaceholderScreen

@Composable
fun PlaylistsScreen(actions: AppActions) {
    PlaceholderScreen(title = "Playlists", subtitle = "Playlist list.") {
        TextButton(onClick = { actions.open(PlaylistRoute("demo-playlist")) }) { Text("Playlist detail") }
    }
}

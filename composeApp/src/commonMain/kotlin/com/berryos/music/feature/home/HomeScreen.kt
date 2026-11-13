package com.berryos.music.feature.home

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.ArtistRoute
import com.berryos.music.app.navigation.PlaylistRoute
import com.berryos.music.core.designsystem.PlaceholderScreen

@Composable
fun HomeScreen(actions: AppActions) {
    PlaceholderScreen(title = "Home", subtitle = "Recent music, suggestions, and shortcuts.") {
        TextButton(onClick = { actions.open(AlbumRoute("demo-album")) }) { Text("Album detail") }
        TextButton(onClick = { actions.open(ArtistRoute("demo-artist")) }) { Text("Artist detail") }
        TextButton(onClick = { actions.open(PlaylistRoute("demo-playlist")) }) { Text("Playlist detail") }
    }
}

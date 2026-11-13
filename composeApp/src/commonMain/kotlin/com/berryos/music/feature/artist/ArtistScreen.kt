package com.berryos.music.feature.artist

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.PlaceholderScreen

@Composable
fun ArtistScreen(artistId: String, actions: AppActions) {
    PlaceholderScreen(title = "Artist", subtitle = "Artist id: $artistId") {
        TextButton(onClick = { actions.open(AlbumRoute("demo-album")) }) { Text("Album detail") }
    }
}

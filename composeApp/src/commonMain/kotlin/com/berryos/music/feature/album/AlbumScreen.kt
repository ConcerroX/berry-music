package com.berryos.music.feature.album

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.ArtistRoute
import com.berryos.music.core.designsystem.PlaceholderScreen

@Composable
fun AlbumScreen(albumId: String, actions: AppActions) {
    PlaceholderScreen(title = "Album", subtitle = "Album id: $albumId") {
        TextButton(onClick = actions.back) { Text("Back") }
        TextButton(onClick = { actions.open(ArtistRoute("demo-artist")) }) { Text("Artist detail") }
    }
}

package com.berryos.music.feature.search

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.PlaceholderScreen

@Composable
fun SearchScreen(actions: AppActions) {
    PlaceholderScreen(title = "Search", subtitle = "Local and Subsonic search results.") {
        TextButton(onClick = { actions.open(AlbumRoute("demo-album")) }) { Text("Album detail") }
    }
}

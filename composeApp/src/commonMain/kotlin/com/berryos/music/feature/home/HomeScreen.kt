package com.berryos.music.feature.home

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.SearchRoute
import com.berryos.music.app.navigation.SettingsRoute
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.library.LibraryUiState

@Composable
fun HomeScreen(actions: AppActions, state: LibraryUiState) {
    PlaceholderScreen(
        title = "Home",
        subtitle = when (state) {
            LibraryUiState.Loading -> "Loading local library..."

            is LibraryUiState.Error -> state.message

            is LibraryUiState.Ready ->
                "${state.snapshot.tracks.size} tracks · " +
                    "${state.snapshot.albums.size} albums · " +
                    "${state.snapshot.artists.size} artists"
        },
    ) {
        TextButton(onClick = { actions.open(SearchRoute) }) { Text("Search") }
        TextButton(onClick = { actions.open(SettingsRoute) }) { Text("Settings") }
    }
}

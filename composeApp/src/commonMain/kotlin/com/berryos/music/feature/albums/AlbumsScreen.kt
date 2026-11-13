package com.berryos.music.feature.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.LibraryLoading
import com.berryos.music.core.designsystem.LibraryMessage
import com.berryos.music.core.library.LibraryUiState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AlbumsScreen(state: LibraryUiState, actions: AppActions, onRefresh: () -> Unit) {
    when (state) {
        LibraryUiState.Loading -> LibraryLoading()

        is LibraryUiState.Error -> LibraryMessage("Couldn't load music", state.message, onRefresh)

        is LibraryUiState.Ready -> {
            val albums = state.snapshot.albums
            if (albums.isEmpty()) {
                LibraryMessage("No albums found", "Scanned music albums will appear here.", onRefresh)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(albums) { album ->
                        ListItem(
                            modifier = Modifier.clickable { actions.open(AlbumRoute(album.id)) },
                            text = { Text(album.title) },
                            secondaryText = { Text("${album.artist} · ${album.trackCount} tracks") },
                        )
                    }
                }
            }
        }
    }
}

package com.berryos.music.feature.artists

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.berryos.music.core.designsystem.LibraryLoading
import com.berryos.music.core.designsystem.LibraryMessage
import com.berryos.music.core.library.LibraryUiState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ArtistsScreen(state: LibraryUiState, onRefresh: () -> Unit) {
    when (state) {
        LibraryUiState.Loading -> LibraryLoading()

        is LibraryUiState.Error -> LibraryMessage("Couldn't load music", state.message, onRefresh)

        is LibraryUiState.Ready -> {
            val artists = state.snapshot.artists
            if (artists.isEmpty()) {
                LibraryMessage("No artists found", "Scanned artists will appear here.", onRefresh)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(artists) { artist ->
                        ListItem(
                            text = { Text(artist.name) },
                            secondaryText = {
                                Text("${artist.albumCount} albums · ${artist.trackCount} tracks")
                            },
                        )
                    }
                }
            }
        }
    }
}

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
import com.berryos.music.resources.Res
import com.berryos.music.resources.artist_summary
import com.berryos.music.resources.could_not_load_music
import com.berryos.music.resources.no_artists_found
import com.berryos.music.resources.no_artists_message
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ArtistsScreen(state: LibraryUiState, onRefresh: () -> Unit) {
    when (state) {
        LibraryUiState.Loading -> LibraryLoading()

        is LibraryUiState.Error -> LibraryMessage(
            stringResource(Res.string.could_not_load_music),
            state.message,
            onRefresh,
        )

        is LibraryUiState.Ready -> {
            val artists = state.snapshot.artists
            if (artists.isEmpty()) {
                LibraryMessage(
                    stringResource(Res.string.no_artists_found),
                    stringResource(Res.string.no_artists_message),
                    onRefresh,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(artists) { artist ->
                        ListItem(
                            text = { Text(artist.name) },
                            secondaryText = {
                                Text(
                                    stringResource(
                                        Res.string.artist_summary,
                                        artist.albumCount,
                                        artist.trackCount,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

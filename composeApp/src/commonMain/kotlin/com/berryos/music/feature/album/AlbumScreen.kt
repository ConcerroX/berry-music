package com.berryos.music.feature.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.designsystem.formatDuration
import com.berryos.music.core.library.LibraryUiState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AlbumScreen(albumId: String, state: LibraryUiState, actions: AppActions) {
    when (state) {
        LibraryUiState.Loading -> PlaceholderScreen("Album", "Loading library...")

        is LibraryUiState.Error -> PlaceholderScreen("Album", state.message)

        is LibraryUiState.Ready -> {
            val album = state.snapshot.albums.firstOrNull { it.id == albumId }
            if (album == null) {
                PlaceholderScreen("Album", "Album not found.")
            } else {
                val tracks = state.snapshot.tracks
                    .filter { it.source == album.source && it.albumId == album.id }
                    .sortedBy { it.trackNumber }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = album.title, style = MaterialTheme.typography.h5)
                            Text(
                                text = "${album.artist} · ${album.year}",
                                style = MaterialTheme.typography.body2,
                            )
                            Text(
                                text = "${tracks.size} tracks",
                                style = MaterialTheme.typography.caption,
                            )
                        }
                    }
                    itemsIndexed(tracks) { index, track ->
                        ListItem(
                            modifier = Modifier.clickable { actions.play(tracks, index) },
                            text = { Text(track.title) },
                            trailing = { Text(formatDuration(track.durationMs)) },
                        )
                    }
                }
            }
        }
    }
}

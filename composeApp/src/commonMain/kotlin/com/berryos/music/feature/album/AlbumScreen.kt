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
import com.berryos.music.resources.Res
import com.berryos.music.resources.album
import com.berryos.music.resources.album_artist_year
import com.berryos.music.resources.album_not_found
import com.berryos.music.resources.loading_library
import com.berryos.music.resources.track_count
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AlbumScreen(albumId: String, state: LibraryUiState, actions: AppActions) {
    when (state) {
        LibraryUiState.Loading -> PlaceholderScreen(
            stringResource(Res.string.album),
            stringResource(Res.string.loading_library),
        )

        is LibraryUiState.Error -> PlaceholderScreen(stringResource(Res.string.album), state.message)

        is LibraryUiState.Ready -> {
            val album = state.snapshot.albums.firstOrNull { it.id == albumId }
            if (album == null) {
                PlaceholderScreen(stringResource(Res.string.album), stringResource(Res.string.album_not_found))
            } else {
                val tracks = state.snapshot.tracks
                    .filter { it.source == album.source && it.albumId == album.id }
                    .sortedBy { it.trackNumber }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = album.title, style = MaterialTheme.typography.h5)
                            Text(
                                text = stringResource(Res.string.album_artist_year, album.artist, album.year),
                                style = MaterialTheme.typography.body2,
                            )
                            Text(
                                text = stringResource(Res.string.track_count, tracks.size),
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

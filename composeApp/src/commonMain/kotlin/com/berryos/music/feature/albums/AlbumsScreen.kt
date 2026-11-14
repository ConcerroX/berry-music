package com.berryos.music.feature.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.CoverTile
import com.berryos.music.core.designsystem.LibraryLoading
import com.berryos.music.core.designsystem.LibraryMessage
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.resources.Res
import com.berryos.music.resources.album_summary
import com.berryos.music.resources.could_not_load_music
import com.berryos.music.resources.no_albums_found
import com.berryos.music.resources.no_albums_message
import org.jetbrains.compose.resources.stringResource

@Composable
fun AlbumsScreen(state: LibraryUiState, actions: AppActions) {
    when (state) {
        LibraryUiState.Loading -> LibraryLoading()

        is LibraryUiState.Error -> LibraryMessage(
            stringResource(Res.string.could_not_load_music),
            state.message,
        )

        is LibraryUiState.Ready -> {
            val albums = state.snapshot.albums
            if (albums.isEmpty()) {
                LibraryMessage(
                    stringResource(Res.string.no_albums_found),
                    stringResource(Res.string.no_albums_message),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(albums, key = { it.id }) { album ->
                        CoverTile(
                            coverUrl = album.coverUrl,
                            title = album.title,
                            text = stringResource(Res.string.album_summary, album.artist, album.trackCount),
                        ) {
                            actions.open(AlbumRoute(album.id))
                        }
                    }
                }
            }
        }
    }
}

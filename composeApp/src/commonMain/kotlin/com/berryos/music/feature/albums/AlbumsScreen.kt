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
import com.berryos.music.resources.Res
import com.berryos.music.resources.album_summary
import com.berryos.music.resources.could_not_load_music
import com.berryos.music.resources.no_albums_found
import com.berryos.music.resources.no_albums_message
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AlbumsScreen(state: LibraryUiState, actions: AppActions, onRefresh: () -> Unit) {
    when (state) {
        LibraryUiState.Loading -> LibraryLoading()

        is LibraryUiState.Error -> LibraryMessage(
            stringResource(Res.string.could_not_load_music),
            state.message,
            onRefresh,
        )

        is LibraryUiState.Ready -> {
            val albums = state.snapshot.albums
            if (albums.isEmpty()) {
                LibraryMessage(
                    stringResource(Res.string.no_albums_found),
                    stringResource(Res.string.no_albums_message),
                    onRefresh,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(albums) { album ->
                        ListItem(
                            modifier = Modifier.clickable { actions.open(AlbumRoute(album.id)) },
                            text = { Text(album.title) },
                            secondaryText = {
                                Text(
                                    stringResource(
                                        Res.string.album_summary,
                                        album.artist,
                                        album.trackCount,
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

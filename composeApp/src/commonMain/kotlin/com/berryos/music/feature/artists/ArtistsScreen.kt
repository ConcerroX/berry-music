package com.berryos.music.feature.artists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.container.rContainerTransformSource
import com.berryos.music.app.ContainerTransformController
import com.berryos.music.app.artistTransformKey
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.ArtistRoute
import com.berryos.music.app.rememberContainerTransformHold
import com.berryos.music.core.designsystem.CoverTile
import com.berryos.music.core.designsystem.LibraryLoading
import com.berryos.music.core.designsystem.LibraryMessage
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.resources.Res
import com.berryos.music.resources.artist_summary
import com.berryos.music.resources.could_not_load_music
import com.berryos.music.resources.no_artists_found
import com.berryos.music.resources.no_artists_message
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ArtistsScreen(state: LibraryUiState, actions: AppActions, transform: ContainerTransformController) {
    val scope = rememberCoroutineScope()
    rememberContainerTransformHold(active = transform.activeKey != null)
    when (state) {
        LibraryUiState.Loading -> LibraryLoading()

        is LibraryUiState.Error -> LibraryMessage(
            stringResource(Res.string.could_not_load_music),
            state.message,
        )

        is LibraryUiState.Ready -> {
            val artists = state.snapshot.artists
            if (artists.isEmpty()) {
                LibraryMessage(
                    stringResource(Res.string.no_artists_found),
                    stringResource(Res.string.no_artists_message),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(artists, key = { "${it.source}:${it.id}" }) { artist ->
                        val key = artistTransformKey(artist.source, artist.id)
                        val sourceState = transform.sourceState(key)
                        CoverTile(
                            coverUrl = artist.coverUrl,
                            title = artist.name,
                            text = stringResource(Res.string.artist_summary, artist.albumCount, artist.trackCount),
                            modifier = Modifier.rContainerTransformSource(
                                state = sourceState,
                                hidden = transform.hiddenKey == key,
                            ),
                        ) {
                            if (transform.activeKey == null) {
                                transform.request(key)
                                scope.launch {
                                    withTimeoutOrNull(SOURCE_RECORD_TIMEOUT_MILLIS) {
                                        snapshotFlow { sourceState.recorded }.first { it }
                                    }
                                    actions.open(ArtistRoute(artist.source, artist.id))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val SOURCE_RECORD_TIMEOUT_MILLIS = 250L

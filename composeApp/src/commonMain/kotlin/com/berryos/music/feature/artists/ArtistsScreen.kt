package com.berryos.music.feature.artists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.container.rContainerTransformSource
import art.rippledesign.compose.rui.navigation3.RContainerTransformNavController
import art.rippledesign.compose.rui.navigation3.rememberRContainerTransformHold
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.ArtistRoute
import com.berryos.music.app.navigation.ContainerTransformKey
import com.berryos.music.app.navigation.artistTransformKey
import com.berryos.music.core.designsystem.ContainerTransformSpec
import com.berryos.music.core.designsystem.CoverTile
import com.berryos.music.core.designsystem.LibraryLoading
import com.berryos.music.core.designsystem.LibraryMessage
import com.berryos.music.core.designsystem.reportPageScrollable
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
internal fun ArtistsScreen(
    state: LibraryUiState,
    actions: AppActions,
    transform: RContainerTransformNavController<ContainerTransformKey>,
) {
    val scope = rememberCoroutineScope()
    var viewportBounds by remember { mutableStateOf<Rect?>(null) }
    val gridState = rememberLazyGridState()
    gridState.reportPageScrollable()
    // Keeps this grid composed while the transform draws it; Nav3 would otherwise drop it as soon
    // as its own scene transition settles.
    rememberRContainerTransformHold(
        active = transform.activeKey != null,
        durationMillis = ContainerTransformSpec.enterDurationMillis,
    )
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
                    state = gridState,
                    modifier = Modifier.fillMaxSize().onGloballyPositioned { viewportBounds = it.boundsInWindow() },
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
                                // Read in the draw phase so the tile can reappear in the same frame
                                // the transform entry is disposed.
                                hidden = { transform.hiddenKey == key },
                                // Items scrolled half out of the grid start from their visible part.
                                clipBounds = { viewportBounds },
                            ),
                        ) {
                            if (transform.activeKey == null) {
                                transform.request(key)
                                scope.launch {
                                    // Wait one frame for the card snapshot; navigating earlier would
                                    // start the transform without a source.
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

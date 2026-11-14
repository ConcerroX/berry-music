package com.berryos.music.feature.artists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.RTile
import art.rippledesign.compose.rui.RTileMode
import art.rippledesign.compose.rui.RTilePosition
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.ArtistRoute
import com.berryos.music.core.designsystem.AlbumCover
import com.berryos.music.core.designsystem.LibraryLoading
import com.berryos.music.core.designsystem.LibraryMessage
import com.berryos.music.core.designsystem.rememberAlbumCoverColors
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.model.Artist
import com.berryos.music.resources.Res
import com.berryos.music.resources.artist_summary
import com.berryos.music.resources.could_not_load_music
import com.berryos.music.resources.ic_more_vert
import com.berryos.music.resources.more_options
import com.berryos.music.resources.no_artists_found
import com.berryos.music.resources.no_artists_message
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ArtistsScreen(state: LibraryUiState, actions: AppActions) {
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
                        ArtistTile(artist) { actions.open(ArtistRoute(artist.id)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistTile(artist: Artist, onClick: () -> Unit) {
    val coverColors = rememberAlbumCoverColors(artist.coverUrl)
    RTile(
        media = {
            AlbumCover(artist.coverUrl, coverColors::onImage)
        },
        backgroundColor = coverColors.colors?.background ?: Color.Transparent,
        contentColor = coverColors.colors?.contentColor,
        supportingContentColor = coverColors.colors?.supportingColor,
        onClick = onClick,
        mode = RTileMode.Outside,
        position = RTilePosition.Footer,
        title = artist.name,
        text = stringResource(Res.string.artist_summary, artist.albumCount, artist.trackCount),
        action = {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(Res.drawable.ic_more_vert),
                    contentDescription = stringResource(Res.string.more_options),
                )
            }
        },
    )
}

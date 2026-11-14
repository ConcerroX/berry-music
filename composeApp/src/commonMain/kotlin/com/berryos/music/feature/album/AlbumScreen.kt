package com.berryos.music.feature.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.RTile
import art.rippledesign.compose.rui.RTileMode
import art.rippledesign.compose.rui.RTilePosition
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.AlbumCover
import com.berryos.music.core.designsystem.CoverColors
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.designsystem.coverColorsOf
import com.berryos.music.core.designsystem.formatDuration
import com.berryos.music.core.designsystem.rememberDefaultCoverColors
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.model.Album
import com.berryos.music.resources.Res
import com.berryos.music.resources.album
import com.berryos.music.resources.album_artist_year
import com.berryos.music.resources.album_not_found
import com.berryos.music.resources.ic_more_vert
import com.berryos.music.resources.loading_library
import com.berryos.music.resources.more_options
import com.berryos.music.resources.track_count
import org.jetbrains.compose.resources.painterResource
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
                        AlbumHeader(album, tracks.size)
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

@Composable
private fun AlbumHeader(album: Album, trackCount: Int) {
    val fallbackColors = rememberDefaultCoverColors()
    var coverColors by remember(album.coverUrl) { mutableStateOf<CoverColors?>(null) }
    val colors = coverColors ?: fallbackColors
    Column {
        RTile(
            media = {
                AlbumCover(album.coverUrl) { image ->
                    coverColors = image?.coverColorsOf()
                }
            },
            backgroundColor = colors?.background ?: Color.Transparent,
            contentColor = colors?.contentColor,
            supportingContentColor = colors?.supportingColor,
            mode = RTileMode.Outside,
            position = RTilePosition.Footer,
            title = album.title,
            text = stringResource(Res.string.album_artist_year, album.artist, album.year),
            action = {
                IconButton(onClick = {}) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_more_vert),
                        contentDescription = stringResource(Res.string.more_options),
                    )
                }
            },
        )
        Text(
            text = stringResource(Res.string.track_count, trackCount),
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

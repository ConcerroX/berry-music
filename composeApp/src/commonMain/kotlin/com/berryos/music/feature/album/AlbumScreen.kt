package com.berryos.music.feature.album

import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.playback.PlaybackSource
import com.berryos.music.core.playback.PlaybackState
import com.berryos.music.core.playback.playbackStatus
import com.berryos.music.feature.detail.MediaDetailScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.album
import com.berryos.music.resources.album_not_found
import com.berryos.music.resources.loading_library
import org.jetbrains.compose.resources.stringResource

@Composable
fun AlbumScreen(
    albumId: String,
    state: LibraryUiState,
    actions: AppActions,
    playbackState: PlaybackState,
    collapseOnPause: Boolean,
) {
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
                val playbackSource = PlaybackSource.Album(album.id)
                MediaDetailScreen(
                    title = album.title,
                    subtitle = album.artist,
                    coverUrl = album.coverUrl,
                    tracks = tracks,
                    playbackSource = playbackSource,
                    playbackStatus = playbackState.playbackStatus(playbackSource),
                    collapseOnPause = collapseOnPause,
                    actions = actions,
                )
            }
        }
    }
}

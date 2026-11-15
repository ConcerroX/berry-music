package com.berryos.music.feature.artist

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.model.SourceKey
import com.berryos.music.core.model.artist
import com.berryos.music.core.playback.PlaybackPosition
import com.berryos.music.core.playback.PlaybackSource
import com.berryos.music.core.playback.PlaybackState
import com.berryos.music.core.playback.playbackStatus
import com.berryos.music.feature.detail.MediaDetailScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.artist
import com.berryos.music.resources.artist_not_found
import com.berryos.music.resources.artist_summary
import com.berryos.music.resources.loading_library
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

@Composable
fun ArtistScreen(
    source: SourceKey,
    artistId: String,
    state: LibraryUiState,
    actions: AppActions,
    playbackState: PlaybackState,
    playbackPosition: StateFlow<PlaybackPosition>,
    collapseOnPause: Boolean,
    topBar: @Composable (Color?) -> Unit = {},
) {
    when (state) {
        LibraryUiState.Loading -> PlaceholderScreen(
            stringResource(Res.string.artist),
            stringResource(Res.string.loading_library),
        )

        is LibraryUiState.Error -> PlaceholderScreen(stringResource(Res.string.artist), state.message)

        is LibraryUiState.Ready -> {
            val artist = state.snapshot.artist(source, artistId)
            if (artist == null) {
                PlaceholderScreen(stringResource(Res.string.artist), stringResource(Res.string.artist_not_found))
            } else {
                val tracks = state.snapshot.tracks
                    .filter { it.source == artist.source && it.artistId == artist.id }
                    .sortedWith(compareBy({ it.album }, { it.trackNumber }))
                val playbackSource = PlaybackSource.Artist(artist.id)
                MediaDetailScreen(
                    title = artist.name,
                    subtitle = stringResource(Res.string.artist_summary, artist.albumCount, artist.trackCount),
                    coverUrl = artist.coverUrl,
                    tracks = tracks,
                    playbackSource = playbackSource,
                    playbackStatus = playbackState.playbackStatus(playbackSource),
                    playbackPosition = playbackPosition,
                    collapseOnPause = collapseOnPause,
                    actions = actions,
                    topBar = topBar,
                )
            }
        }
    }
}

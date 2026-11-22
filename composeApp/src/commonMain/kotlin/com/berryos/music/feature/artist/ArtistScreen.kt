package com.berryos.music.feature.artist

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.model.SourceKey
import com.berryos.music.core.model.artist
import com.berryos.music.core.playback.PlaybackPosition
import com.berryos.music.core.playback.PlaybackSource
import com.berryos.music.core.playback.PlaybackState
import com.berryos.music.feature.detail.DetailStateContent
import com.berryos.music.feature.detail.MediaDetailScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.artist
import com.berryos.music.resources.artist_not_found
import com.berryos.music.resources.artist_summary
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
    bottomInset: Dp = 0.dp,
    topBar: @Composable (Color?) -> Unit = {},
) {
    val title = stringResource(Res.string.artist)
    DetailStateContent(title, state) { snapshot ->
        val artist = snapshot.artist(source, artistId)
        if (artist == null) {
            PlaceholderScreen(title, stringResource(Res.string.artist_not_found))
        } else {
            val tracks = snapshot.tracks
                .filter { track ->
                    track.source == artist.source && track.artistRefs.any { it.id == artist.id }
                }
                .sortedWith(compareBy({ it.album }, { it.trackNumber }))
            val playbackSource = PlaybackSource.Artist(artist.id)
            MediaDetailScreen(
                title = artist.name,
                subtitle = stringResource(Res.string.artist_summary, artist.albumCount, artist.trackCount),
                coverUrl = artist.coverUrl,
                tracks = tracks,
                playbackSource = playbackSource,
                playbackState = playbackState,
                playbackPosition = playbackPosition,
                collapseOnPause = collapseOnPause,
                actions = actions,
                bottomInset = bottomInset,
                topBar = topBar,
            )
        }
    }
}

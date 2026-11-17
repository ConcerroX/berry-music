package com.berryos.music.feature.album

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.model.SourceKey
import com.berryos.music.core.model.album
import com.berryos.music.core.playback.PlaybackPosition
import com.berryos.music.core.playback.PlaybackSource
import com.berryos.music.core.playback.PlaybackState
import com.berryos.music.feature.detail.DetailStateContent
import com.berryos.music.feature.detail.MediaDetailScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.album
import com.berryos.music.resources.album_not_found
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

@Composable
fun AlbumScreen(
    source: SourceKey,
    albumId: String,
    state: LibraryUiState,
    actions: AppActions,
    playbackState: PlaybackState,
    playbackPosition: StateFlow<PlaybackPosition>,
    collapseOnPause: Boolean,
    bottomInset: Dp = 0.dp,
    topBar: @Composable (Color?) -> Unit = {},
) {
    val title = stringResource(Res.string.album)
    DetailStateContent(title, state) { snapshot ->
        val album = snapshot.album(source, albumId)
        if (album == null) {
            PlaceholderScreen(title, stringResource(Res.string.album_not_found))
        } else {
            val tracks = snapshot.tracks
                .filter { it.source == album.source && it.albumId == album.id }
                .sortedBy { it.trackNumber }
            val playbackSource = PlaybackSource.Album(album.id)
            MediaDetailScreen(
                title = album.title,
                subtitle = album.artist,
                coverUrl = album.coverUrl,
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

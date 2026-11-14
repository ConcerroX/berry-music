package com.berryos.music.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.Image
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.AlbumCover
import com.berryos.music.core.designsystem.MEDIUM_ALPHA
import com.berryos.music.core.designsystem.formatDuration
import com.berryos.music.core.designsystem.rememberAlbumCoverColors
import com.berryos.music.core.model.Track
import com.berryos.music.core.playback.PlaybackSource
import com.berryos.music.core.playback.PlaybackStatus

@Composable
fun MediaDetailScreen(
    title: String,
    subtitle: String,
    coverUrl: String?,
    tracks: List<Track>,
    playbackSource: PlaybackSource,
    playbackStatus: PlaybackStatus,
    collapseOnPause: Boolean,
    actions: AppActions,
) {
    val coverColors = rememberAlbumCoverColors(coverUrl)
    val listState = remember(playbackSource) { LazyListState() }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            MediaImageHeader(coverUrl, coverColors::onImage)
        }
        item {
            // Reset the header state when the sheet swaps to another collection.
            key(playbackSource) {
                MediaPlaybackHeader(
                    title = title,
                    subtitle = subtitle,
                    colors = coverColors.colors,
                    onPlay = {
                        if (playbackStatus == PlaybackStatus.Paused) {
                            actions.togglePlayPause()
                        } else {
                            actions.play(tracks, 0, playbackSource)
                        }
                    },
                    onTogglePlayPause = actions.togglePlayPause,
                    onPrevious = actions.previous,
                    onNext = actions.next,
                    playbackStatus = playbackStatus,
                    collapseOnPause = collapseOnPause,
                )
            }
        }
        itemsIndexed(tracks) { index, track ->
            TrackRow(track, onClick = { actions.play(tracks, index, playbackSource) })
        }
    }
}

@Composable
private fun MediaImageHeader(coverUrl: String?, onImage: (Image?) -> Unit) {
    Box {
        AlbumCover(coverUrl, onImage)
        MediaImageScrim()
    }
}

@Composable
private fun BoxScope.MediaImageScrim() {
    val statusBarHeight = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(statusBarHeight + APP_BAR_HEIGHT)
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colors.onSurface.copy(alpha = SCRIM_ALPHA), Color.Transparent),
                ),
            ),
    )
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = track.trackNumber.toString(),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface.copy(alpha = MEDIUM_ALPHA),
            modifier = Modifier.width(TRACK_NUMBER_WIDTH),
        )
        Text(
            text = track.title,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface.copy(alpha = MEDIUM_ALPHA),
        )
    }
}

private val APP_BAR_HEIGHT = 56.dp
private val TRACK_NUMBER_WIDTH = 56.dp
private const val SCRIM_ALPHA = 0.30f

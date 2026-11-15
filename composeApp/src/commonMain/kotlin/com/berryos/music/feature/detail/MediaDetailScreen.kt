package com.berryos.music.feature.detail

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.Image
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.AlbumCover
import com.berryos.music.core.designsystem.AlbumCoverColorsState
import com.berryos.music.core.designsystem.MEDIUM_ALPHA
import com.berryos.music.core.designsystem.formatDuration
import com.berryos.music.core.designsystem.rememberAlbumCoverColors
import com.berryos.music.core.model.Track
import com.berryos.music.core.playback.PlaybackPosition
import com.berryos.music.core.playback.PlaybackSource
import com.berryos.music.core.playback.PlaybackState
import com.berryos.music.core.playback.PlaybackStatus
import com.berryos.music.core.playback.playbackStatus
import com.berryos.music.resources.Res
import com.berryos.music.resources.ic_more_vert
import com.berryos.music.resources.ic_volume_up
import com.berryos.music.resources.more_options
import com.berryos.music.resources.track_summary
import com.berryos.music.resources.up_next_duration
import com.berryos.music.resources.volume_up
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun MediaDetailScreen(
    title: String,
    subtitle: String,
    coverUrl: String?,
    tracks: List<Track>,
    playbackSource: PlaybackSource,
    playbackState: PlaybackState,
    playbackPosition: StateFlow<PlaybackPosition>,
    collapseOnPause: Boolean,
    actions: AppActions,
    topBar: @Composable (Color?) -> Unit = {},
) {
    val coverColors = rememberAlbumCoverColors(coverUrl)
    val pageBackground = coverColors.colors?.background ?: MaterialTheme.colors.surface
    val listState = remember(playbackSource) { LazyListState() }
    val playbackStatus = playbackState.playbackStatus(playbackSource)
    val currentTrack = playbackState.currentTrack.takeIf { playbackStatus != PlaybackStatus.NotPlaying }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val appBarBottomPx = WindowInsets.statusBars.getTop(density) + with(density) { APP_BAR_HEIGHT.roundToPx() }
    val expansionState = remember(scope, listState, appBarBottomPx) {
        TrackListExpansionState(scope, listState, appBarBottomPx)
    }
    LaunchedEffect(expansionState.expandable) {
        expansionState.collapseIfNotExpandable()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
            .onSizeChanged { expansionState.containerHeight = it.height }
            .nestedScroll(expansionState.connection),
    ) {
        TrackListLayout(
            expansion = expansionState.expansion,
            appBarBottomPx = appBarBottomPx,
            modifier = Modifier.fillMaxSize(),
            header = {
                MediaHeaderSection(
                    title = title,
                    subtitle = subtitle,
                    coverUrl = coverUrl,
                    coverColors = coverColors,
                    playbackSource = playbackSource,
                    playbackStatus = playbackStatus,
                    playbackPosition = playbackPosition,
                    tracks = tracks,
                    collapseOnPause = collapseOnPause,
                    actions = actions,
                    modifier = Modifier.onSizeChanged { expansionState.headerHeight = it.height },
                )
            },
            card = {
                TrackListCard(
                    tracks = tracks,
                    listState = listState,
                    playbackStatus = playbackStatus,
                    currentTrack = currentTrack,
                    playbackSource = playbackSource,
                    actions = actions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { expansionState.cardHeight = it.size.height },
                )
            },
        )
        topBar(coverColors.colors?.contentColor)
    }
}

@Composable
private fun MediaHeaderSection(
    title: String,
    subtitle: String,
    coverUrl: String?,
    coverColors: AlbumCoverColorsState,
    playbackSource: PlaybackSource,
    playbackStatus: PlaybackStatus,
    playbackPosition: StateFlow<PlaybackPosition>,
    tracks: List<Track>,
    collapseOnPause: Boolean,
    actions: AppActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MediaImageHeader(coverUrl, coverColors::onImage)
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
                onSeek = actions.seekTo,
                playbackStatus = playbackStatus,
                playbackPosition = playbackPosition,
                collapseOnPause = collapseOnPause,
            )
        }
    }
}

@Composable
private fun TrackListLayout(
    expansion: Float,
    appBarBottomPx: Int,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    card: @Composable () -> Unit,
) {
    Layout(
        contents = listOf(header, card),
        modifier = modifier,
    ) { (headerContent, cardContent), constraints ->
        val headerPlaceables = headerContent.map {
            it.measure(Constraints(maxWidth = constraints.maxWidth))
        }
        val headerHeight = headerPlaceables.maxOfOrNull { it.height } ?: 0
        val available = (constraints.maxHeight - headerHeight).coerceAtLeast(0)
        val shift = (((headerHeight - appBarBottomPx).coerceAtLeast(0)) * expansion).roundToInt()
        val cardPlaceables = cardContent.map {
            it.measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = available + shift))
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            headerPlaceables.forEach { it.placeRelative(0, 0) }
            cardPlaceables.forEach { it.placeRelative(0, headerHeight - shift) }
        }
    }
}

@Composable
private fun TrackListCard(
    tracks: List<Track>,
    listState: LazyListState,
    playbackStatus: PlaybackStatus,
    currentTrack: Track?,
    playbackSource: PlaybackSource,
    actions: AppActions,
    modifier: Modifier = Modifier,
) {
    val showCurrentTrack = playbackStatus != PlaybackStatus.NotPlaying && currentTrack != null
    val cardHeight by animateDpAsState(
        targetValue = trackListHeight(tracks.size, showCurrentTrack) + CARD_VERTICAL_PADDING * 2,
        animationSpec = tween(CARD_SIZE_CHANGE_MILLIS, easing = FastOutSlowInEasing),
        label = "trackListHeight",
    )
    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = cardHeight)
                .padding(start = 16.dp, end = 16.dp),
            elevation = 2.dp,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = CARD_VERTICAL_PADDING),
            ) {
                if (showCurrentTrack && currentTrack != null) {
                    item {
                        CurrentTrackSection(
                            track = currentTrack,
                            showAlbum = playbackSource !is PlaybackSource.Album,
                            totalDurationMs = tracks.sumOf { it.durationMs },
                        )
                    }
                }
                itemsIndexed(tracks) { index, track ->
                    Column {
                        TrackRow(
                            track = track,
                            showAlbum = playbackSource !is PlaybackSource.Album,
                            onClick = { actions.play(tracks, index, playbackSource) },
                        )
                        if (index < tracks.lastIndex) {
                            Divider(modifier = Modifier.padding(start = TRACK_DIVIDER_INSET))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentTrackSection(track: Track, showAlbum: Boolean, totalDurationMs: Long) {
    Column {
        TrackRow(track = track, showAlbum = showAlbum, showVolumeIcon = true, onClick = null)
        Divider(modifier = Modifier.padding(top = CURRENT_TRACK_DIVIDER_TOP_PADDING))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(UP_NEXT_HEADER_HEIGHT)
                .padding(start = TRACK_DIVIDER_INSET),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(Res.string.up_next_duration, formatDuration(totalDurationMs)),
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.primary,
            )
        }
    }
}

private fun trackListHeight(trackCount: Int, showCurrentTrack: Boolean): Dp {
    val tracksHeight = if (trackCount == 0) {
        0.dp
    } else {
        TRACK_ROW_HEIGHT * trackCount + TRACK_DIVIDER_HEIGHT * (trackCount - 1)
    }
    val currentHeight = if (showCurrentTrack) {
        TRACK_ROW_HEIGHT + CURRENT_TRACK_DIVIDER_TOP_PADDING + TRACK_DIVIDER_HEIGHT + UP_NEXT_HEADER_HEIGHT
    } else {
        0.dp
    }
    return tracksHeight + currentHeight
}

@Composable
private fun MediaImageHeader(coverUrl: String?, onImage: (Image?) -> Unit) {
    Box {
        AlbumCover(coverUrl, onImage = onImage)
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
private fun TrackRow(track: Track, showAlbum: Boolean, onClick: (() -> Unit)?, showVolumeIcon: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .height(TRACK_ROW_HEIGHT)
            .padding(start = TRACK_ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(TRACK_NUMBER_WIDTH),
            contentAlignment = Alignment.Center,
        ) {
            if (showVolumeIcon) {
                Icon(
                    painter = painterResource(Res.drawable.ic_volume_up),
                    contentDescription = stringResource(Res.string.volume_up),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = MEDIUM_ALPHA),
                )
            } else {
                Text(
                    text = track.trackNumber.toString(),
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurface.copy(alpha = MEDIUM_ALPHA),
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = Modifier.width(TRACK_TEXT_GAP))
        TrackTexts(
            title = track.title,
            subtitle = if (showAlbum) {
                stringResource(Res.string.track_summary, track.artist, track.album)
            } else {
                stringResource(Res.string.track_summary, track.artist, formatDuration(track.durationMs))
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        // A 48dp IconButton centers its 24dp icon, so 4dp puts the icon 16dp from the edge.
        IconButton(onClick = {}, modifier = Modifier.padding(end = 4.dp)) {
            Icon(
                painter = painterResource(Res.drawable.ic_more_vert),
                contentDescription = stringResource(Res.string.more_options),
                tint = MaterialTheme.colors.onSurface.copy(alpha = MEDIUM_ALPHA),
            )
        }
    }
}

/** Places the two lines by baseline: 32dp from the row top, then 20dp below the first baseline. */
@Composable
private fun TrackTexts(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Layout(
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = MEDIUM_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val titlePlaceable = measurables[0].measure(constraints)
        val subtitlePlaceable = measurables[1].measure(constraints)
        val firstBaseline = TITLE_BASELINE_FROM_TOP.roundToPx()
        val secondBaseline = firstBaseline + SUBTITLE_BASELINE_GAP.roundToPx()
        layout(constraints.maxWidth, constraints.maxHeight) {
            titlePlaceable.placeRelative(0, firstBaseline - titlePlaceable[FirstBaseline])
            subtitlePlaceable.placeRelative(0, secondBaseline - subtitlePlaceable[FirstBaseline])
        }
    }
}

private val APP_BAR_HEIGHT = 56.dp
private val CARD_VERTICAL_PADDING = 8.dp
private val TRACK_ROW_HEIGHT = 72.dp
private val UP_NEXT_HEADER_HEIGHT = 48.dp
private val TRACK_DIVIDER_HEIGHT = 1.dp
private val CURRENT_TRACK_DIVIDER_TOP_PADDING = 8.dp
private val TRACK_ROW_PADDING = 16.dp
private val TRACK_NUMBER_WIDTH = 24.dp
private val TRACK_TEXT_GAP = 16.dp
private val TRACK_DIVIDER_INSET = TRACK_ROW_PADDING + TRACK_NUMBER_WIDTH + TRACK_TEXT_GAP
private val TITLE_BASELINE_FROM_TOP = 32.dp
private val SUBTITLE_BASELINE_GAP = 20.dp
private const val SCRIM_ALPHA = 0.30f

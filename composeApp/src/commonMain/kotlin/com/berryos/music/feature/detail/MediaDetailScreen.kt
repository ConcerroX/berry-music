@file:Suppress("TooManyFunctions")

package com.berryos.music.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.RListItemInsertion
import art.rippledesign.compose.rui.RListItemInsertionStyle
import art.rippledesign.compose.rui.rememberRListItemInsertionState
import coil3.Image
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.AlbumCover
import com.berryos.music.core.designsystem.AlbumCoverColorsState
import com.berryos.music.core.designsystem.LOW_ALPHA
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
import com.berryos.music.resources.play_next
import com.berryos.music.resources.track_summary
import com.berryos.music.resources.up_next_duration
import com.berryos.music.resources.volume_up
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
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
    entryIds: List<Long>? = null,
    bottomInset: Dp = 0.dp,
    topBar: @Composable (Color?) -> Unit = {},
) {
    val coverColors = rememberAlbumCoverColors(coverUrl)
    val listState = remember(playbackSource) { LazyListState() }
    val playbackStatus = playbackState.playbackStatus(playbackSource)
    val currentTrack = playbackState.currentTrack.takeIf { playbackStatus != PlaybackStatus.NotPlaying }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val appBarBottomPx = WindowInsets.statusBars.getTop(density) + with(density) { APP_BAR_HEIGHT.roundToPx() }
    // Only inset from the system bar when the page itself reaches the window bottom; detail
    // pages above the mini player already sit above the system bars.
    val navigationBarBottom = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val cardContentBottomInset = if (bottomInset > 0.dp) 0.dp else navigationBarBottom
    val expansionState =
        remember(scope, listState, appBarBottomPx) { TrackListExpansionState(scope, listState, appBarBottomPx) }
    SyncCardContentHeight(expansionState, tracks.size, currentTrack != null)
    LaunchedEffect(expansionState.expandable) { expansionState.collapseIfNotExpandable() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(coverColors.colors?.background ?: MaterialTheme.colors.surface)
            .padding(bottom = bottomInset)
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
                    currentTrack = currentTrack,
                    playbackSource = playbackSource,
                    actions = actions,
                    entryIds = entryIds,
                    contentBottomInset = cardContentBottomInset,
                    cardExpanded = expansionState.expansion > 0f,
                    currentIndex = playbackState.currentIndex,
                )
            },
        )
        // Back closes the expanded card first; the playback header takes the next press.
        BackHandler(enabled = expansionState.expansion > 0f) { expansionState.collapse() }
        topBar(coverColors.colors?.contentColor)
    }
}

/** The card fills to the bottom, so expansion depends on the natural height of its content. */
@Composable
private fun SyncCardContentHeight(
    expansionState: TrackListExpansionState,
    trackCount: Int,
    showCurrentTrack: Boolean,
) {
    val density = LocalDensity.current
    val contentHeight = trackListHeight(trackCount, showCurrentTrack) + CARD_VERTICAL_PADDING * 2
    SideEffect { expansionState.contentHeight = with(density) { contentHeight.roundToPx() } }
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

@Suppress("LongMethod")
@Composable
private fun TrackListCard(
    tracks: List<Track>,
    listState: LazyListState,
    currentTrack: Track?,
    playbackSource: PlaybackSource,
    actions: AppActions,
    entryIds: List<Long>?,
    contentBottomInset: Dp,
    cardExpanded: Boolean,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    val showCurrentTrack = currentTrack != null
    val insertionState = rememberRListItemInsertionState(playbackSource)
    // The playback band expands while the card is at rest; let the band carry the vertical motion.
    val topItemStyle = if (cardExpanded) RListItemInsertionStyle.Rise else RListItemInsertionStyle.Fade
    // The current track section is inserted at the top once playback starts; reveal it at once,
    // because the playback band is expanding at the same time and two motions would fight.
    LaunchedEffect(showCurrentTrack) {
        if (showCurrentTrack) listState.scrollToItem(0)
    }
    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp),
            shape = RectangleShape,
            elevation = 2.dp,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // Padding inside the scroll container keeps the overscroll glow at the card edge.
                contentPadding = PaddingValues(
                    top = CARD_VERTICAL_PADDING,
                    bottom = CARD_VERTICAL_PADDING + contentBottomInset,
                ),
            ) {
                if (showCurrentTrack && currentTrack != null) {
                    item(key = CURRENT_TRACK_ITEM_KEY) {
                        RListItemInsertion(
                            state = insertionState,
                            itemKey = CURRENT_TRACK_ITEM_KEY,
                            style = topItemStyle,
                            modifier = Modifier.animateItem(fadeInSpec = null),
                        ) {
                            CurrentTrackSection(
                                track = currentTrack,
                                showAlbum = playbackSource !is PlaybackSource.Album,
                                totalDurationMs = tracks.sumOf { it.durationMs },
                                onPlayNext = actions.playNext,
                            )
                        }
                    }
                }
                itemsIndexed(
                    tracks,
                    key = { index, track -> entryIds?.getOrNull(index) ?: trackKey(track) },
                ) { index, track ->
                    val itemKey = entryIds?.getOrNull(index) ?: trackKey(track)
                    // Album and artist pages also act as the playback queue while they are the
                    // session, so the page order maps to the queue order; only queue pages show
                    // the relative number, the others keep the track number.
                    val queueOffset = if (showCurrentTrack && currentIndex >= 0) {
                        index - currentIndex
                    } else {
                        null
                    }
                    val played = queueOffset != null && queueOffset < 0
                    RListItemInsertion(
                        state = insertionState,
                        itemKey = itemKey,
                        index = index,
                        modifier = Modifier.animateItem(fadeInSpec = null),
                    ) {
                        Column {
                            TrackRow(
                                track = track,
                                showAlbum = playbackSource !is PlaybackSource.Album,
                                number = if (playbackSource is PlaybackSource.Queue) queueOffset else null,
                                played = played,
                                selected = queueOffset == 0,
                                onClick = { actions.play(tracks, index, playbackSource) },
                                onPlayNext = actions.playNext,
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
}

@Composable
private fun CurrentTrackSection(track: Track, showAlbum: Boolean, totalDurationMs: Long, onPlayNext: (Track) -> Unit) {
    Column {
        TrackRow(
            track = track,
            showAlbum = showAlbum,
            showVolumeIcon = true,
            // Tappable for consistency with the other rows; replaying the same track is a no-op.
            onClick = {},
            onPlayNext = onPlayNext,
        )
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
private fun TrackRow(
    track: Track,
    showAlbum: Boolean,
    number: Int? = null,
    played: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)?,
    onPlayNext: (Track) -> Unit,
    showVolumeIcon: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) {
                    MaterialTheme.colors.primary.copy(alpha = SELECTED_ALPHA)
                } else {
                    Color.Transparent
                },
            )
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
                    text = (number ?: track.trackNumber).toString(),
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurface.copy(
                        alpha = if (played) LOW_ALPHA else MEDIUM_ALPHA,
                    ),
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
            played = played,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        TrackRowMenu(track, onPlayNext)
    }
}

@Composable
private fun TrackRowMenu(track: Track, onPlayNext: (Track) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        // A 48dp IconButton centers its 24dp icon, so 4dp puts the icon 16dp from the edge.
        IconButton(onClick = { expanded = true }, modifier = Modifier.padding(end = 4.dp)) {
            Icon(
                painter = painterResource(Res.drawable.ic_more_vert),
                contentDescription = stringResource(Res.string.more_options),
                tint = MaterialTheme.colors.onSurface.copy(alpha = MEDIUM_ALPHA),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    onPlayNext(track)
                },
            ) {
                Text(stringResource(Res.string.play_next))
            }
        }
    }
}

/** Places the two lines by baseline: 32dp from the row top, then 20dp below the first baseline. */
@Composable
private fun TrackTexts(title: String, subtitle: String, played: Boolean, modifier: Modifier = Modifier) {
    Layout(
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                color = if (played) {
                    MaterialTheme.colors.onSurface.copy(alpha = LOW_ALPHA)
                } else {
                    MaterialTheme.colors.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(
                    alpha = if (played) LOW_ALPHA else MEDIUM_ALPHA,
                ),
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

private fun trackKey(track: Track): String = "${track.source}:${track.id}"

private const val CURRENT_TRACK_ITEM_KEY = "current-track"
private const val SELECTED_ALPHA = 0.12f

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

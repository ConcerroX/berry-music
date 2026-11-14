package com.berryos.music.feature.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.RCircularReveal
import art.rippledesign.compose.rui.RPlayPauseIcon
import coil3.Image
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.AlbumCover
import com.berryos.music.core.designsystem.CoverColors
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.designsystem.cacheCoverColors
import com.berryos.music.core.designsystem.cachedCoverColors
import com.berryos.music.core.designsystem.coverColorsOf
import com.berryos.music.core.designsystem.formatDuration
import com.berryos.music.core.designsystem.rememberDefaultCoverColors
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.model.Album
import com.berryos.music.core.model.Track
import com.berryos.music.resources.Res
import com.berryos.music.resources.album
import com.berryos.music.resources.album_not_found
import com.berryos.music.resources.loading_library
import com.berryos.music.resources.pause
import com.berryos.music.resources.play
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
                AlbumDetails(album, tracks, actions)
            }
        }
    }
}

@Composable
private fun AlbumDetails(album: Album, tracks: List<Track>, actions: AppActions) {
    val fallbackColors = rememberDefaultCoverColors()
    var coverColors by remember(album.coverUrl) {
        mutableStateOf(album.coverUrl?.let(::cachedCoverColors))
    }
    val colors = coverColors ?: fallbackColors
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            AlbumImageHeader(album.coverUrl) { image ->
                val coverUrl = album.coverUrl
                if (coverColors == null && image != null && coverUrl != null) {
                    val computed = image.coverColorsOf()
                    coverColors = computed
                    if (computed != null) {
                        cacheCoverColors(coverUrl, computed)
                    }
                }
            }
        }
        item {
            AlbumTitleBar(
                album = album,
                colors = colors,
                onPlay = { actions.play(tracks, 0) },
                onTogglePlayPause = actions.togglePlayPause,
                onPrevious = actions.previous,
                onNext = actions.next,
            )
        }
        itemsIndexed(tracks) { index, track ->
            TrackRow(track, onClick = { actions.play(tracks, index) })
        }
    }
}

@Composable
private fun AlbumImageHeader(coverUrl: String?, onImage: (Image?) -> Unit) {
    Box {
        AlbumCover(coverUrl, onImage)
        TopScrim()
    }
}

@Composable
private fun AlbumTitleBar(
    album: Album,
    colors: CoverColors?,
    onPlay: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val state = rememberAlbumPlaybackHeaderState()
    val fabColor = colors?.accentColor ?: MaterialTheme.colors.secondary
    val fabContentColor = colors?.accentContentColor ?: MaterialTheme.colors.onSecondary
    val density = LocalDensity.current
    val revealOriginY = with(density) { FAB_TARGET_TOP.toPx() }
    val fabStart = with(density) {
        Offset(state.bandSize.width - (FAB_TRAILING_MARGIN + FAB_RADIUS).toPx(), 0f)
    }
    val fabTarget = Offset(state.bandSize.width / 2f, revealOriginY)
    val fabTranslation = arcTranslation(fabStart, fabTarget, state.fabMoveProgress.value)
    val fabPosition = fabStart + fabTranslation
    var naturalHeight by remember { mutableStateOf(0.dp) }
    val expandedHeight = REVEAL_RECT_HEIGHT + BAND_BOTTOM_EXTRA
    val bandHeight = naturalHeight + (expandedHeight - naturalHeight) * state.bandExpandProgress.value
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (naturalHeight > 0.dp) bandHeight else Dp.Unspecified)
            .onSizeChanged {
                state.bandSize = it
                if (naturalHeight == 0.dp) {
                    naturalHeight = with(density) { it.height.toDp() }
                }
            },
    ) {
        AlbumTitleLayers(
            album = album,
            colors = colors,
            fabColor = fabColor,
            revealed = state.revealed,
            revealOrigin = fabPosition,
            bandWidth = state.bandSize.width.toFloat(),
            controlsProgress = state.controlsProgress.value,
            volumeAlpha = state.volumeProgress.value,
            topStripProgress = state.topStripProgress.value,
            titleAlpha = 1f - state.titleFadeProgress.value,
            bottomTextAlpha = state.bottomTextProgress.value,
            onPrevious = onPrevious,
            onNext = onNext,
        )
        AlbumPlayContainer(
            visible = state.containerVisible,
            color = fabColor,
            rippleColor = fabContentColor,
            translation = fabTranslation,
            onClick = { state.start(onPlay) },
        )
        AlbumPlayIcon(
            playing = state.iconPlaying,
            contentColor = fabContentColor,
            moved = state.fabDone,
            translation = fabTranslation,
            onClick = { state.collapse(onTogglePlayPause) },
        )
    }
}

@Composable
private fun BoxScope.AlbumTitleLayers(
    album: Album,
    colors: CoverColors?,
    fabColor: Color,
    revealed: Boolean,
    revealOrigin: Offset,
    bandWidth: Float,
    controlsProgress: Float,
    volumeAlpha: Float,
    topStripProgress: Float,
    titleAlpha: Float,
    bottomTextAlpha: Float,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(colors?.background ?: Color.Transparent),
    )
    AlbumTitleText(album, colors, titleAlpha)
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .height(REVEAL_RECT_HEIGHT)
            .clipToBounds(),
    ) {
        RCircularReveal(
            revealed = revealed,
            modifier = Modifier.fillMaxSize(),
            startRadius = FAB_RADIUS,
            durationMillis = REVEAL_MILLIS,
            origin = { revealOrigin },
            color = fabColor,
            elevation = FAB_ELEVATION,
        )
    }
    val startGap = with(density) { CONTROL_START_GAP.toPx() }
    val finalInset = with(density) { (CONTROL_EDGE_MARGIN + CONTROL_SIZE / 2).toPx() }
    val finalY = with(density) { FAB_TARGET_TOP.toPx() }
    val volumeY = finalY + with(density) { VOLUME_CENTER_DISTANCE.toPx() }
    AlbumPlaybackControls(
        colors = colors,
        previousCenter = lerp(
            Offset(revealOrigin.x - startGap, revealOrigin.y),
            Offset(finalInset, finalY),
            controlsProgress,
        ),
        nextCenter = lerp(
            Offset(revealOrigin.x + startGap, revealOrigin.y),
            Offset(bandWidth - finalInset, finalY),
            controlsProgress,
        ),
        volumeDownCenter = Offset(finalInset, volumeY),
        volumeUpCenter = Offset(bandWidth - finalInset, volumeY),
        controlsAlpha = controlsProgress,
        volumeAlpha = volumeAlpha,
        modifier = Modifier.matchParentSize(),
        onPrevious = onPrevious,
        onNext = onNext,
    )
    AlbumTopStrip(topStripProgress)
    AlbumBottomText(album, colors, bottomTextAlpha)
}

@Composable
private fun BoxScope.AlbumTitleText(album: Album, colors: CoverColors?, titleAlpha: Float) {
    val layerAlpha = titleAlpha
    Column(
        modifier = Modifier
            // Bottom-anchored so the title follows the growing band while it fades out.
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .graphicsLayer { this.alpha = layerAlpha }
            .padding(start = 72.dp, end = 16.dp, top = 48.dp, bottom = 48.dp),
    ) {
        Text(
            text = album.title,
            style = MaterialTheme.typography.h4,
            color = colors?.contentColor ?: Color.Unspecified,
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.subtitle1,
            color = colors?.contentColor ?: Color.Unspecified,
        )
    }
}

@Composable
private fun BoxScope.AlbumPlayContainer(
    visible: Boolean,
    color: Color,
    rippleColor: Color,
    translation: Offset,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(end = FAB_TRAILING_MARGIN)
            .offset(y = FAB_OFFSET)
            .size(FAB_SIZE)
            .graphicsLayer {
                translationX = translation.x
                translationY = translation.y
                alpha = if (visible) 1f else 0f
            }
            .shadow(elevation = FAB_ELEVATION, shape = CircleShape)
            .background(color = color, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = rippleColor),
                onClick = onClick,
            ),
    )
}

@Composable
private fun BoxScope.AlbumPlayIcon(
    playing: Boolean,
    contentColor: Color,
    moved: Boolean,
    translation: Offset,
    onClick: () -> Unit,
) {
    val modifier = if (moved) {
        Modifier
            .align(Alignment.TopCenter)
            .offset(y = FAB_TARGET_TOP - FAB_RADIUS)
            .size(FAB_SIZE)
    } else {
        Modifier
            .align(Alignment.TopEnd)
            .padding(end = FAB_TRAILING_MARGIN)
            .offset(y = FAB_OFFSET)
            .size(FAB_SIZE)
            .graphicsLayer {
                translationX = translation.x
                translationY = translation.y
            }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (moved) {
            IconButton(onClick = onClick) {
                RPlayPauseIcon(
                    playing = playing,
                    contentDescription = stringResource(if (playing) Res.string.pause else Res.string.play),
                    tint = contentColor,
                    durationMillis = ICON_MORPH_MILLIS,
                )
            }
        } else {
            RPlayPauseIcon(
                playing = playing,
                contentDescription = stringResource(if (playing) Res.string.pause else Res.string.play),
                tint = contentColor,
                durationMillis = ICON_MORPH_MILLIS,
            )
        }
    }
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

@Composable
private fun BoxScope.TopScrim() {
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

private val APP_BAR_HEIGHT = 56.dp
private val FAB_OFFSET = (-28).dp
private val FAB_RADIUS = 28.dp
private val FAB_SIZE = 56.dp
private val FAB_ELEVATION = 6.dp
private val FAB_TARGET_TOP = 48.dp
private val REVEAL_BOTTOM_GAP = 36.dp

// Bottom of the volume row plus REVEAL_BOTTOM_GAP.
private val REVEAL_RECT_HEIGHT = FAB_TARGET_TOP + VOLUME_CENTER_DISTANCE + VOLUME_ICON_SIZE / 2 + REVEAL_BOTTOM_GAP

// Extra band below the reveal rectangle that hosts the bottom title text.
private val BAND_BOTTOM_EXTRA = 64.dp
private val FAB_TRAILING_MARGIN = 16.dp
private val TRACK_NUMBER_WIDTH = 56.dp
private const val SCRIM_ALPHA = 0.30f
private const val MEDIUM_ALPHA = 0.54f

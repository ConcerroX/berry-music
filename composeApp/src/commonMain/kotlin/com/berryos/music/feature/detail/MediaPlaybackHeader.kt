package com.berryos.music.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.RCircularReveal
import art.rippledesign.compose.rui.RPlayPauseIcon
import com.berryos.music.core.designsystem.CoverColors
import com.berryos.music.core.playback.PlaybackStatus
import com.berryos.music.resources.Res
import com.berryos.music.resources.pause
import com.berryos.music.resources.play
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MediaPlaybackHeader(
    title: String,
    subtitle: String,
    colors: CoverColors?,
    onPlay: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    playbackStatus: PlaybackStatus,
    collapseOnPause: Boolean,
) {
    val state = rememberMediaPlaybackHeaderState(playbackStatus, collapseOnPause)
    // The icon shows the playback object's state; the header phase only controls presentation.
    val playing = playbackStatus == PlaybackStatus.Playing
    val fabColor = colors?.accentColor ?: MaterialTheme.colors.secondary
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
        MediaHeaderLayers(
            title = title,
            subtitle = subtitle,
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
            revealShadow = state.revealShadowProgress.value,
            onPrevious = onPrevious,
            onNext = onNext,
        )
        PlayFabs(
            state = state,
            playing = playing,
            colors = colors,
            translation = fabTranslation,
            onPlay = { state.start(onPlay) },
            onTogglePlayPause = onTogglePlayPause,
        )
    }
}

@Composable
private fun BoxScope.MediaHeaderLayers(
    title: String,
    subtitle: String,
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
    revealShadow: Float,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(colors?.background ?: Color.Transparent),
    )
    // Below the reveal so the expanded player covers it.
    BandTopEdge()
    MediaTitleText(title, subtitle, colors, titleAlpha)
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .height(REVEAL_RECT_HEIGHT)
            .shadow(elevation = REVEAL_SHADOW_ELEVATION * revealShadow, shape = RectangleShape)
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
    val centers = controlCenters(revealOrigin, bandWidth, startGap, finalInset, finalY, controlsProgress)
    MediaPlaybackControls(
        colors = colors,
        centers = centers,
        muteCenter = Offset(finalInset, volumeY),
        volumeUpCenter = Offset(bandWidth - finalInset, volumeY),
        controlsAlpha = controlsProgress,
        volumeAlpha = volumeAlpha,
        modifier = Modifier.matchParentSize(),
        onPrevious = onPrevious,
        onNext = onNext,
    )
    val progressColor = colors?.let {
        it.accentContentColor.copy(alpha = PROGRESS_CONTENT_ALPHA).compositeOver(it.accentColor)
    } ?: Color.White
    MediaTopStrip(topStripProgress, progressColor)
    MediaBottomText(title, subtitle, colors, bottomTextAlpha)
}

@Composable
private fun BoxScope.BandTopEdge() {
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .height(TOP_EDGE_HEIGHT)
            .background(Color.White.copy(alpha = TOP_EDGE_ALPHA)),
    )
}

@Composable
private fun BoxScope.MediaTitleText(title: String, subtitle: String, colors: CoverColors?, titleAlpha: Float) {
    val layerAlpha = titleAlpha
    Column(
        modifier = Modifier
            // Bottom-anchored so the title follows the growing band while it fades out.
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .graphicsLayer { this.alpha = layerAlpha }
            .padding(start = 72.dp, end = 72.dp, top = 48.dp, bottom = 48.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.h4,
            color = colors?.contentColor ?: Color.Unspecified,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.subtitle1,
            color = colors?.contentColor ?: Color.Unspecified,
        )
    }
}

@Composable
private fun BoxScope.PlayFabContainer(
    visible: Boolean,
    enabled: Boolean,
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
                enabled = visible && enabled,
                onClick = onClick,
            ),
    )
}

@Composable
private fun BoxScope.PlayFabs(
    state: MediaPlaybackHeaderState,
    playing: Boolean,
    colors: CoverColors?,
    translation: Offset,
    onPlay: () -> Unit,
    onTogglePlayPause: () -> Unit,
) {
    val color = colors?.accentColor ?: MaterialTheme.colors.secondary
    val contentColor = colors?.accentContentColor ?: MaterialTheme.colors.onSecondary
    PlayFabContainer(
        visible = state.containerVisible,
        enabled = state.phase == HeaderPhase.Collapsed,
        color = color,
        rippleColor = contentColor,
        translation = translation,
        onClick = onPlay,
    )
    PlayFabIcon(
        playing = playing,
        contentColor = contentColor,
        moved = state.fabArrived,
        translation = translation,
    )
    if (state.phase != HeaderPhase.Collapsed) {
        PlayFabExpanded(
            playing = playing,
            background = contentColor,
            contentColor = color,
            state = state,
            onClick = onTogglePlayPause,
        )
    }
}

@Composable
private fun BoxScope.PlayFabIcon(playing: Boolean, contentColor: Color, moved: Boolean, translation: Offset) {
    if (moved) return
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(end = FAB_TRAILING_MARGIN)
            .offset(y = FAB_OFFSET)
            .size(FAB_SIZE)
            .graphicsLayer {
                translationX = translation.x
                translationY = translation.y
            },
        contentAlignment = Alignment.Center,
    ) {
        RPlayPauseIcon(
            playing = playing,
            contentDescription = stringResource(if (playing) Res.string.pause else Res.string.play),
            tint = contentColor,
            durationMillis = ICON_MORPH_MILLIS,
        )
    }
}

@Composable
private fun BoxScope.PlayFabExpanded(
    playing: Boolean,
    background: Color,
    contentColor: Color,
    state: MediaPlaybackHeaderState,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val playFabVisible = state.playFabAlpha.value > 0.5f
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = FAB_TARGET_TOP - FAB_RADIUS)
            .size(FAB_SIZE)
            .graphicsLayer {
                scaleX = state.playFabScale.value
                scaleY = state.playFabScale.value
                alpha = state.playFabAlpha.value
            }
            .shadow(elevation = FAB_ELEVATION, shape = CircleShape)
            .background(color = background, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = contentColor),
                enabled = state.phase == HeaderPhase.Expanded && playFabVisible,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        RPlayPauseIcon(
            playing = playing,
            contentDescription = stringResource(if (playing) Res.string.pause else Res.string.play),
            tint = contentColor,
            durationMillis = ICON_MORPH_MILLIS,
            modifier = Modifier
                .size(PLAY_ICON_SIZE)
                .graphicsLayer {
                    val iconScale = state.playFabIconScale.value
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
    }
}

@Composable
private fun rememberMediaPlaybackHeaderState(
    playbackStatus: PlaybackStatus,
    collapseOnPause: Boolean,
): MediaPlaybackHeaderState {
    val scope = rememberCoroutineScope()
    val state = remember(scope) {
        MediaPlaybackHeaderState(scope, playbackStatus != PlaybackStatus.NotPlaying)
    }
    var wasPlaying by remember { mutableStateOf(playbackStatus == PlaybackStatus.Playing) }
    LaunchedEffect(playbackStatus, collapseOnPause) {
        val collapsedToFab = playbackStatus == PlaybackStatus.NotPlaying
        val pausedHere = wasPlaying && playbackStatus == PlaybackStatus.Paused && collapseOnPause
        if ((collapsedToFab || pausedHere) && state.expanded) {
            state.collapse()
        }
        wasPlaying = playbackStatus == PlaybackStatus.Playing
    }
    return state
}

private val FAB_OFFSET = (-28).dp
private val FAB_RADIUS = 28.dp
private val FAB_SIZE = 56.dp
private val PLAY_ICON_SIZE = 24.dp
private val FAB_ELEVATION = 6.dp
private val REVEAL_SHADOW_ELEVATION = 2.dp
private val FAB_TARGET_TOP = 56.dp
private val REVEAL_BOTTOM_GAP = 48.dp
private val FAB_TRAILING_MARGIN = 16.dp

// Bottom of the volume row plus REVEAL_BOTTOM_GAP.
private val REVEAL_RECT_HEIGHT = FAB_TARGET_TOP + VOLUME_CENTER_DISTANCE + VOLUME_ICON_SIZE / 2 + REVEAL_BOTTOM_GAP

// Extra band below the reveal rectangle that hosts the bottom title text.
private val BAND_BOTTOM_EXTRA = 64.dp

// The progress strip is the reveal color tinted with a little of the content color.
private const val PROGRESS_CONTENT_ALPHA = 0.12f

// Bright edge separating the header band from the cover.
private val TOP_EDGE_HEIGHT = 1.dp
private const val TOP_EDGE_ALPHA = 0.12f

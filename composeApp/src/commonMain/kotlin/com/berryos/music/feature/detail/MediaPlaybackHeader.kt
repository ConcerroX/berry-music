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
import androidx.compose.material.IconButton
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
import androidx.compose.ui.geometry.lerp
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
    val iconPlaying = state.expanded && playbackStatus == PlaybackStatus.Playing
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
        PlayFabContainer(
            visible = state.containerVisible,
            color = fabColor,
            rippleColor = fabContentColor,
            translation = fabTranslation,
            onClick = { state.start(onPlay) },
        )
        PlayFabIcon(
            playing = iconPlaying,
            contentColor = fabContentColor,
            moved = state.fabArrived,
            translation = fabTranslation,
            onClick = onTogglePlayPause,
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
    MediaPlaybackControls(
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
                enabled = visible,
                onClick = onClick,
            ),
    )
}

@Composable
private fun BoxScope.PlayFabIcon(
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
    val icon: @Composable () -> Unit = {
        RPlayPauseIcon(
            playing = playing,
            contentDescription = stringResource(if (playing) Res.string.pause else Res.string.play),
            tint = contentColor,
            durationMillis = ICON_MORPH_MILLIS,
        )
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (moved) {
            IconButton(onClick = onClick) { icon() }
        } else {
            icon()
        }
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
private val FAB_ELEVATION = 6.dp
private val REVEAL_SHADOW_ELEVATION = 2.dp
private val FAB_TARGET_TOP = 48.dp
private val REVEAL_BOTTOM_GAP = 36.dp
private val FAB_TRAILING_MARGIN = 16.dp

// Bottom of the volume row plus REVEAL_BOTTOM_GAP.
private val REVEAL_RECT_HEIGHT = FAB_TARGET_TOP + VOLUME_CENTER_DISTANCE + VOLUME_ICON_SIZE / 2 + REVEAL_BOTTOM_GAP

// Extra band below the reveal rectangle that hosts the bottom title text.
private val BAND_BOTTOM_EXTRA = 64.dp

// The progress strip is the reveal color tinted with a little of the content color.
private const val PROGRESS_CONTENT_ALPHA = 0.12f

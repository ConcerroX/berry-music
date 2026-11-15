package com.berryos.music.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.berryos.music.core.designsystem.CoverColors
import com.berryos.music.core.designsystem.formatDuration
import com.berryos.music.core.playback.PlaybackPosition
import com.berryos.music.resources.Res
import com.berryos.music.resources.title_subtitle
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

internal val TOP_STRIP_HEIGHT = 20.dp
private val BOTTOM_TEXT_HEIGHT = 64.dp
private val STRIP_TEXT_MARGIN = 8.dp
private val STRIP_TRAVEL_INSET = 48.dp
private val STRIP_THUMB_WIDTH = 4.dp
private val STRIP_TOUCH_HEIGHT = 48.dp

/** Strip that grows upward from the header; it holds the seek bar and its times. */
@Composable
internal fun BoxScope.MediaTopStrip(
    reveal: Float,
    stripColor: Color,
    contentColor: Color,
    playbackPosition: StateFlow<PlaybackPosition>,
    onSeek: (Long) -> Unit,
) {
    val position by playbackPosition.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val height = TOP_STRIP_HEIGHT * reveal
    val heightPx = with(density) { height.toPx() }
    val touchHalfPx = with(density) { STRIP_TOUCH_HEIGHT.toPx() / 2f }
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset { IntOffset(0, -heightPx.roundToInt()) }
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
            .background(stripColor),
    ) {
        StripVisuals(position, contentColor, interactionSource)
    }
    if (reveal > 0f) {
        StripTouchTarget(
            position = position,
            onSeek = onSeek,
            interactionSource = interactionSource,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(0, -(heightPx / 2f + touchHalfPx).roundToInt()) }
                .fillMaxWidth()
                .height(STRIP_TOUCH_HEIGHT),
        )
    }
}

@Composable
private fun StripVisuals(
    position: PlaybackPosition,
    contentColor: Color,
    interactionSource: MutableInteractionSource,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val travelInset = with(density) { STRIP_TRAVEL_INSET.toPx() }
        val thumbWidth = with(density) { STRIP_THUMB_WIDTH.toPx() }
        val rippleHalfSize = with(density) { TOP_STRIP_HEIGHT.toPx() / 2f }
        val travel = with(density) { maxWidth.toPx() } - travelInset * 2 - thumbWidth
        val thumbX = travelInset + travel * playbackFraction(position)
        // An unbounded ripple expands from the square's center with the default radius (half
        // the diagonal, i.e. its circumscribed circle); the strip clip keeps it inside.
        Box(
            modifier = Modifier
                .offset { IntOffset((thumbX + thumbWidth / 2f - rippleHalfSize).roundToInt(), 0) }
                .size(TOP_STRIP_HEIGHT)
                .indication(interactionSource, ripple(bounded = false, color = contentColor)),
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbX.roundToInt(), 0) }
                .width(STRIP_THUMB_WIDTH)
                .fillMaxHeight()
                .background(contentColor),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = STRIP_TEXT_MARGIN),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatDuration(position.positionMs),
            style = MaterialTheme.typography.caption,
            color = contentColor,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = formatDuration(position.durationMs),
            style = MaterialTheme.typography.caption,
            color = contentColor,
        )
    }
}

@Composable
private fun StripTouchTarget(
    position: PlaybackPosition,
    onSeek: (Long) -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val currentOnSeek by rememberUpdatedState(onSeek)
    val durationMs = position.durationMs
    val density = LocalDensity.current
    val travelInset = with(density) { STRIP_TRAVEL_INSET.toPx() }
    val thumbWidth = with(density) { STRIP_THUMB_WIDTH.toPx() }
    val rippleHalfSize = with(density) { TOP_STRIP_HEIGHT.toPx() / 2f }
    Box(
        modifier = modifier.pointerInput(durationMs, travelInset, thumbWidth, rippleHalfSize) {
            val travel = size.width - travelInset * 2 - thumbWidth
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                // The bounded press starts at the ripple square's center. Unbounded ripples
                // ignore this offset, but the interaction still needs a valid position.
                val press = PressInteraction.Press(Offset(rippleHalfSize, rippleHalfSize))
                interactionSource.tryEmit(press)
                if (durationMs > 0L && travel > 0f) {
                    val fraction = ((down.position.x - travelInset - thumbWidth / 2f) / travel)
                        .coerceIn(0f, 1f)
                    currentOnSeek((fraction * durationMs).toLong())
                }
                var pressed = true
                while (pressed) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    pressed = change != null && !change.changedToUp()
                    if (change != null && pressed && change.positionChanged()) {
                        change.consume()
                        if (durationMs > 0L && travel > 0f) {
                            val fraction = ((change.position.x - travelInset - thumbWidth / 2f) / travel)
                                .coerceIn(0f, 1f)
                            currentOnSeek((fraction * durationMs).toLong())
                        }
                    }
                }
                interactionSource.tryEmit(PressInteraction.Release(press))
            }
        },
    )
}

private fun playbackFraction(position: PlaybackPosition): Float = if (position.durationMs > 0L) {
    (position.positionMs.toFloat() / position.durationMs).coerceIn(0f, 1f)
} else {
    0f
}

@Composable
internal fun BoxScope.MediaBottomText(title: String, subtitle: String, colors: CoverColors?, alpha: Float) {
    val layerAlpha = alpha
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .height(BOTTOM_TEXT_HEIGHT)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(Res.string.title_subtitle, title, subtitle),
            style = MaterialTheme.typography.body1,
            color = colors?.contentColor ?: Color.Unspecified,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer { this.alpha = layerAlpha },
        )
    }
}

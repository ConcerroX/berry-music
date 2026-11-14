package com.berryos.music.feature.album

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.berryos.music.core.designsystem.CoverColors
import com.berryos.music.resources.Res
import com.berryos.music.resources.ic_skip_next
import com.berryos.music.resources.ic_skip_previous
import com.berryos.music.resources.ic_volume_mute
import com.berryos.music.resources.ic_volume_up
import com.berryos.music.resources.mute
import com.berryos.music.resources.next
import com.berryos.music.resources.previous
import com.berryos.music.resources.volume_up
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

internal val CONTROL_SIZE = 48.dp
internal val CONTROL_EDGE_MARGIN = 36.dp
internal val CONTROL_START_GAP = 36.dp
internal val VOLUME_CENTER_DISTANCE = 48.dp

internal val VOLUME_ICON_SIZE = 24.dp
private val SLIDER_GAP = 16.dp
private val SLIDER_HEIGHT = 48.dp
private const val MEDIUM_ALPHA = 0.54f

@Composable
internal fun AlbumPlaybackControls(
    colors: CoverColors?,
    previousCenter: Offset,
    nextCenter: Offset,
    volumeDownCenter: Offset,
    volumeUpCenter: Offset,
    controlsAlpha: Float,
    volumeAlpha: Float,
    modifier: Modifier = Modifier,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val density = LocalDensity.current
    val half = with(density) { CONTROL_SIZE.toPx() / 2f }
    val controlColor = colors?.accentContentColor ?: MaterialTheme.colors.onSecondary
    val mediumColor = controlColor.copy(alpha = MEDIUM_ALPHA)
    val controlsLayerAlpha = controlsAlpha
    val volumeLayerAlpha = volumeAlpha
    val volumeHalf = with(density) { VOLUME_ICON_SIZE.toPx() / 2f }
    val sliderGap = with(density) { SLIDER_GAP.toPx() }
    val sliderLeft = volumeDownCenter.x + volumeHalf + sliderGap
    val sliderRight = volumeUpCenter.x - volumeHalf - sliderGap
    val sliderWidth = (sliderRight - sliderLeft).coerceAtLeast(0f)
    val sliderTop = volumeDownCenter.y - with(density) { SLIDER_HEIGHT.toPx() } / 2f
    var volume by remember { mutableStateOf(1f) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = controlsLayerAlpha },
        ) {
            PlaybackButtons(previousCenter, nextCenter, half, controlColor, onPrevious, onNext)
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = volumeLayerAlpha },
        ) {
            VolumeIcon(
                icon = Res.drawable.ic_volume_mute,
                contentDescription = stringResource(Res.string.mute),
                center = volumeDownCenter,
                tint = mediumColor,
            )
            VolumeIcon(
                icon = Res.drawable.ic_volume_up,
                contentDescription = stringResource(Res.string.volume_up),
                center = volumeUpCenter,
                tint = mediumColor,
            )
            Slider(
                value = volume,
                onValueChange = { volume = it },
                colors = SliderDefaults.colors(
                    thumbColor = controlColor,
                    activeTrackColor = controlColor,
                    inactiveTrackColor = mediumColor,
                ),
                modifier = Modifier
                    .offset { IntOffset(sliderLeft.roundToInt(), sliderTop.roundToInt()) }
                    .width(with(density) { sliderWidth.toDp() }),
            )
        }
    }
}

@Composable
private fun PlaybackButtons(
    previousCenter: Offset,
    nextCenter: Offset,
    half: Float,
    tint: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    ControlButton(
        icon = Res.drawable.ic_skip_previous,
        contentDescription = stringResource(Res.string.previous),
        center = previousCenter,
        half = half,
        tint = tint,
        onClick = onPrevious,
    )
    ControlButton(
        icon = Res.drawable.ic_skip_next,
        contentDescription = stringResource(Res.string.next),
        center = nextCenter,
        half = half,
        tint = tint,
        onClick = onNext,
    )
}

@Composable
private fun VolumeIcon(icon: DrawableResource, contentDescription: String, center: Offset, tint: Color) {
    val density = LocalDensity.current
    val half = with(density) { VOLUME_ICON_SIZE.toPx() / 2f }
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier
            .offset { IntOffset((center.x - half).roundToInt(), (center.y - half).roundToInt()) }
            .size(VOLUME_ICON_SIZE),
    )
}

@Composable
private fun ControlButton(
    icon: DrawableResource,
    contentDescription: String,
    center: Offset,
    half: Float,
    tint: Color,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .offset { IntOffset((center.x - half).roundToInt(), (center.y - half).roundToInt()) }
            .size(CONTROL_SIZE),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}

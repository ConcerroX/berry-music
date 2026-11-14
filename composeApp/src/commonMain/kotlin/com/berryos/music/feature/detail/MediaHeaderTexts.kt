package com.berryos.music.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.berryos.music.core.designsystem.CoverColors
import com.berryos.music.resources.Res
import com.berryos.music.resources.title_subtitle
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

internal val TOP_STRIP_HEIGHT = 16.dp
private val BOTTOM_TEXT_HEIGHT = 64.dp

/** Strip that grows upward from the header; it will become the playback progress bar. */
@Composable
internal fun BoxScope.MediaTopStrip(progress: Float, color: Color) {
    val height = with(LocalDensity.current) { TOP_STRIP_HEIGHT * progress }
    val heightPx = with(LocalDensity.current) { height.toPx() }
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset { IntOffset(0, -heightPx.roundToInt()) }
            .fillMaxWidth()
            .height(height)
            .background(color),
    )
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

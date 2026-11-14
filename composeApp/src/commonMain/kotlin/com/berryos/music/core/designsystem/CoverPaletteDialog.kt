package com.berryos.music.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.RAlertDialog
import com.berryos.music.resources.Res
import com.berryos.music.resources.cover_palette
import org.jetbrains.compose.resources.stringResource

@Composable
fun CoverPaletteDialog(swatches: List<CoverSwatch>, onDismiss: () -> Unit) {
    RAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.cover_palette)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SWATCH_ROW_SPACING)) {
                swatches.forEach { swatch ->
                    SwatchRow(swatch)
                }
            }
        },
    )
}

@Composable
private fun SwatchRow(swatch: CoverSwatch) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SWATCH_ROW_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SWATCH_SIZE)
                .background(swatch.color, CircleShape),
        )
        Spacer(modifier = Modifier.width(SWATCH_GAP))
        Text(text = swatch.name, modifier = Modifier.weight(1f))
        Text(
            text = swatch.hex(),
            color = MaterialTheme.colors.onSurface.copy(alpha = MEDIUM_ALPHA),
        )
    }
}

private fun CoverSwatch.hex(): String {
    val rgb = color.toArgb() and RGB_MASK
    return "#" + rgb.toString(HEX_RADIX).padStart(HEX_DIGITS, '0').uppercase()
}

private val SWATCH_SIZE = 32.dp
private val SWATCH_GAP = 16.dp
private val SWATCH_ROW_SPACING = 4.dp
private val SWATCH_ROW_VERTICAL_PADDING = 6.dp
private const val RGB_MASK = 0xFFFFFF
private const val HEX_RADIX = 16
private const val HEX_DIGITS = 6

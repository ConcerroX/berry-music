package com.berryos.music.core.designsystem

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import coil3.Image

data class CoverColors(
    val background: Color,
    val contentColor: Color,
    val supportingColor: Color,
    val accentColor: Color,
    val accentContentColor: Color,
    val swatches: List<CoverSwatch> = emptyList(),
)

/** One named Palette swatch extracted from the cover. */
data class CoverSwatch(val name: String, val color: Color, val contentColor: Color)

expect fun Image.coverColorsOf(): CoverColors?

expect suspend fun defaultCoverColorsOf(): CoverColors?

private val coverColorsCache = mutableStateMapOf<String, CoverColors>()

fun cachedCoverColors(coverUrl: String): CoverColors? = coverColorsCache[coverUrl]

fun cacheCoverColors(coverUrl: String, colors: CoverColors) {
    coverColorsCache[coverUrl] = colors
}

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

/** Cached background color of a cover, or null while the palette is not available. */
fun cachedCoverBackground(coverUrl: String?): Color? = coverUrl?.let(::cachedCoverColors)?.background

/**
 * Page background for [coverUrl], falling back to the default cover palette while the cover
 * palette is not cached yet.
 */
fun coverPageBackground(coverUrl: String?, fallbackPalette: CoverColors?): Color? =
    cachedCoverBackground(coverUrl) ?: fallbackPalette?.background

fun cacheCoverColors(coverUrl: String, colors: CoverColors) {
    coverColorsCache[coverUrl] = colors
}

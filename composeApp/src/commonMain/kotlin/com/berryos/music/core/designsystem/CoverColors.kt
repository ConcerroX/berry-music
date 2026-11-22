package com.berryos.music.core.designsystem

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import coil3.Image
import com.kmpalette.palette.graphics.Palette

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

/** Raw ARGB pixels of a decoded cover, prepared for palette extraction. */
internal class CoverPixels(val pixels: IntArray, val width: Int, val height: Int)

internal expect fun Image.coverPixels(): CoverPixels?

internal expect suspend fun defaultCoverPixels(): CoverPixels?

fun Image.coverColorsOf(): CoverColors? = coverPixels()?.let(::paletteColors)

suspend fun defaultCoverColorsOf(): CoverColors? = defaultCoverPixels()?.let(::paletteColors)

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

private fun paletteColors(pixels: CoverPixels): CoverColors? {
    val palette = Palette.from(pixels.pixels, pixels.width, pixels.height)
        .maximumColorCount(MAX_COLORS)
        .scaling(false)
        .generate()
    val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch ?: return null
    val accent = accentSwatch(palette, swatch)
    return CoverColors(
        background = Color(swatch.rgb),
        contentColor = Color(swatch.titleTextColor).copy(alpha = 1f),
        supportingColor = Color(swatch.bodyTextColor).copy(alpha = MEDIUM_ALPHA),
        accentColor = Color(accent.rgb),
        accentContentColor = Color(accent.titleTextColor).copy(alpha = 1f),
        swatches = paletteSwatches(palette),
    )
}

private fun paletteSwatches(palette: Palette): List<CoverSwatch> = listOf(
    "Vibrant" to palette.vibrantSwatch,
    "Light vibrant" to palette.lightVibrantSwatch,
    "Dark vibrant" to palette.darkVibrantSwatch,
    "Muted" to palette.mutedSwatch,
    "Light muted" to palette.lightMutedSwatch,
    "Dark muted" to palette.darkMutedSwatch,
    "Dominant" to palette.dominantSwatch,
).mapNotNull { (name, swatch) ->
    swatch?.let {
        CoverSwatch(
            name = name,
            color = Color(it.rgb),
            contentColor = Color(it.titleTextColor).copy(alpha = 1f),
        )
    }
}

private fun accentSwatch(palette: Palette, background: Palette.Swatch): Palette.Swatch =
    palette.vibrantSwatch?.takeIf { it.rgb != background.rgb }
        ?: palette.lightVibrantSwatch?.takeIf { it.rgb != background.rgb }
        ?: palette.darkVibrantSwatch?.takeIf { it.rgb != background.rgb }
        ?: palette.mutedSwatch?.takeIf { it.rgb != background.rgb }
        ?: palette.lightMutedSwatch?.takeIf { it.rgb != background.rgb }
        ?: palette.darkMutedSwatch?.takeIf { it.rgb != background.rgb }
        ?: background

private const val MAX_COLORS = 16

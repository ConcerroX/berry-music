package com.berryos.music.core.designsystem

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.Image
import coil3.toBitmap
import com.berryos.music.resources.Res

actual fun Image.coverColorsOf(): CoverColors? {
    val bitmap = softwareBitmap()
    return if (bitmap.width > 0 && bitmap.height > 0) paletteColors(bitmap) else null
}

/** Drawing a hardware bitmap onto a software canvas throws, so copy it first. */
private fun Image.softwareBitmap(): Bitmap {
    val source = (this as? BitmapImage)?.bitmap
    return when {
        source == null -> toBitmap(width, height, Bitmap.Config.ARGB_8888)
        source.config == Bitmap.Config.HARDWARE -> source.copy(Bitmap.Config.ARGB_8888, false)
        else -> source
    }
}

actual suspend fun defaultCoverColorsOf(): CoverColors? {
    val bytes = Res.readBytes(DEFAULT_COVER_PATH)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return paletteColors(bitmap)
}

private fun paletteColors(bitmap: Bitmap): CoverColors? {
    val scaled = Bitmap.createScaledBitmap(bitmap, PALETTE_SIZE, PALETTE_SIZE, false)
    val palette = Palette.from(scaled)
        .maximumColorCount(MAX_COLORS)
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

private const val PALETTE_SIZE = 64
private const val MAX_COLORS = 16
private const val DEFAULT_COVER_PATH = "drawable/default_album_art.png"

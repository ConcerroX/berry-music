package com.berryos.music.core.designsystem

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.Image
import coil3.toBitmap
import com.berryos.music.resources.Res

actual fun Image.coverColorsOf(): CoverColors? {
    val bitmap = toBitmap(width, height, Bitmap.Config.ARGB_8888)
    return if (bitmap.width > 0 && bitmap.height > 0) paletteColors(bitmap) else null
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
    Log.d(
        "CoverColor",
        "background=#${swatch.rgb.toUInt().toString(HEX_RADIX)} " +
            "title=#${swatch.titleTextColor.toUInt().toString(HEX_RADIX)} " +
            "body=#${swatch.bodyTextColor.toUInt().toString(HEX_RADIX)}",
    )
    return CoverColors(
        background = Color(swatch.rgb),
        contentColor = Color(swatch.titleTextColor).copy(alpha = 1f),
        supportingColor = Color(swatch.bodyTextColor).copy(alpha = MEDIUM_ALPHA),
    )
}

private const val PALETTE_SIZE = 64
private const val MAX_COLORS = 16
private const val MEDIUM_ALPHA = 0.54f
private const val HEX_RADIX = 16
private const val DEFAULT_COVER_PATH = "drawable/default_album_art.png"

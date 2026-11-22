package com.berryos.music.core.designsystem

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil3.BitmapImage
import coil3.Image
import coil3.toBitmap
import com.berryos.music.resources.Res

internal actual fun Image.coverPixels(): CoverPixels? = scaledPixels(softwareBitmap())

internal actual suspend fun defaultCoverPixels(): CoverPixels? {
    val bytes = Res.readBytes(DEFAULT_COVER_PATH)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return scaledPixels(bitmap)
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

private fun scaledPixels(bitmap: Bitmap): CoverPixels? {
    if (bitmap.width <= 0 || bitmap.height <= 0) return null
    val scaled = Bitmap.createScaledBitmap(bitmap, PALETTE_SIZE, PALETTE_SIZE, false)
    val pixels = IntArray(PALETTE_SIZE * PALETTE_SIZE)
    scaled.getPixels(pixels, 0, PALETTE_SIZE, 0, 0, PALETTE_SIZE, PALETTE_SIZE)
    return CoverPixels(pixels, PALETTE_SIZE, PALETTE_SIZE)
}

private const val PALETTE_SIZE = 64
private const val DEFAULT_COVER_PATH = "drawable/default_album_art.png"

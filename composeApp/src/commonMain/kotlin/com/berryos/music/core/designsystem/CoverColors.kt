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
)

expect fun Image.coverColorsOf(): CoverColors?

expect suspend fun defaultCoverColorsOf(): CoverColors?

private val coverColorsCache = mutableStateMapOf<String, CoverColors>()

fun cachedCoverColors(coverUrl: String): CoverColors? = coverColorsCache[coverUrl]

fun cacheCoverColors(coverUrl: String, colors: CoverColors) {
    coverColorsCache[coverUrl] = colors
}

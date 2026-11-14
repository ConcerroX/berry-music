package com.berryos.music.core.designsystem

import androidx.compose.ui.graphics.Color
import coil3.Image

data class CoverColors(val background: Color, val contentColor: Color, val supportingColor: Color)

expect fun Image.coverColorsOf(): CoverColors?

expect suspend fun defaultCoverColorsOf(): CoverColors?

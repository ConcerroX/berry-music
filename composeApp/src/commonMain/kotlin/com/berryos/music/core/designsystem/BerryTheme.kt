package com.berryos.music.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import art.rippledesign.compose.rui.RTheme

/**
 * App theme. When [accent] carries the currently playing cover's colors, the primary and
 * secondary colors follow it; backgrounds, surfaces, and text colors stay on the default
 * Material palette.
 */
@Composable
fun BerryTheme(accent: CoverColors? = null, content: @Composable () -> Unit) {
    val base = if (isSystemInDarkTheme()) darkColors() else lightColors()
    RTheme(colors = animatedColors(base, accent), content = content)
}

@Composable
private fun animatedColors(base: Colors, accent: CoverColors?): Colors {
    val accentColor = accent?.accentColor
    val accentContentColor = accent?.accentContentColor
    val variant = accent?.let { lerp(it.accentColor, Color.Black, VARIANT_BLACK_RATIO) }
    val primary = animatedColor(accentColor ?: base.primary)
    val primaryVariant = animatedColor(variant ?: base.primaryVariant)
    val secondary = animatedColor(accentColor ?: base.secondary)
    val secondaryVariant = animatedColor(variant ?: base.secondaryVariant)
    val onPrimary = animatedColor(accentContentColor ?: base.onPrimary)
    val onSecondary = animatedColor(accentContentColor ?: base.onSecondary)
    return base.copy(
        primary = primary,
        primaryVariant = primaryVariant,
        secondary = secondary,
        secondaryVariant = secondaryVariant,
        onPrimary = onPrimary,
        onSecondary = onSecondary,
    )
}

@Composable
private fun animatedColor(target: Color): Color = animateColorAsState(
    targetValue = target,
    animationSpec = tween(THEME_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
).value

/** Alpha for medium-emphasis content such as secondary text and icons. */
const val MEDIUM_ALPHA = 0.54f

/** Alpha for low-emphasis content, such as queue rows that have already been played. */
const val LOW_ALPHA = 0.38f

private const val THEME_ANIMATION_MILLIS = 600
private const val VARIANT_BLACK_RATIO = 0.3f

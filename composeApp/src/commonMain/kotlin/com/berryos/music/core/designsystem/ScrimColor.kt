package com.berryos.music.core.designsystem

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The platform cross-activity scrim: black at 0.8 on dark themes and 0.2 on light ones. Shared by
 * every surface that dims content behind a transition so the app has one scrim definition.
 */
@Composable
fun rememberPlatformScrimColor(): Color = Color.Black.copy(
    alpha = if (MaterialTheme.colors.isLight) SCRIM_ALPHA_LIGHT else SCRIM_ALPHA_DARK,
)

private const val SCRIM_ALPHA_DARK = 0.8f
private const val SCRIM_ALPHA_LIGHT = 0.2f

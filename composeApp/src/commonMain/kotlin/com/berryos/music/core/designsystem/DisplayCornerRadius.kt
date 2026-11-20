package com.berryos.music.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * Radius of the display's rounded corners, used to round pages when they are presented as scaled
 * cards (predictive back) the way the platform rounds its windows.
 */
@Composable
expect fun rememberDisplayCornerRadius(): Dp

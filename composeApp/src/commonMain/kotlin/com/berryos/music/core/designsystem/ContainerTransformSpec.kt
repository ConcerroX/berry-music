package com.berryos.music.core.designsystem

import androidx.compose.ui.graphics.Color
import art.rippledesign.compose.rui.container.RContainerTransformSpec

internal val ContainerTransformSpec = RContainerTransformSpec()

/** Uses the item's own background for the transforming surface instead of the theme surface. */
internal fun RContainerTransformSpec.withContainerColor(color: Color?): RContainerTransformSpec =
    if (color == null) this else copy(containerColor = color)

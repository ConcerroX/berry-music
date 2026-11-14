package com.berryos.music.feature.detail

import androidx.compose.ui.geometry.Offset

/** Quadratic arc motion following MaterialArcMotion's control point rule. */
internal fun arcTranslation(start: Offset, end: Offset, progress: Float): Offset {
    if (progress <= 0f) {
        return Offset.Zero
    }
    val control = if (start.y > end.y) {
        Offset(end.x, start.y)
    } else {
        Offset(start.x, end.y)
    }
    val inverse = 1f - progress
    val startWeight = inverse * inverse
    val controlWeight = 2f * inverse * progress
    val endWeight = progress * progress
    return Offset(
        x = startWeight * start.x + controlWeight * control.x + endWeight * end.x - start.x,
        y = startWeight * start.y + controlWeight * control.y + endWeight * end.y - start.y,
    )
}

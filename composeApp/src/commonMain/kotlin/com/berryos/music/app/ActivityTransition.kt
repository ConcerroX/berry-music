package com.berryos.music.app

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

// AOSP's default activity transition, mirroring activity_open_enter/exit and
// activity_close_enter/exit: a 96dp slide with a short delayed window fade.
private const val SLIDE_MILLIS = 450
private const val FADE_MILLIS = 83
private const val FADE_IN_DELAY_MILLIS = 50
private const val FADE_OUT_DELAY_MILLIS = 35

internal val WindowMotionOffset = 96.dp

/**
 * The platform activity open/close transition.
 *
 * Opening slides the new scene in from +[WindowMotionOffset] with a delayed fade and moves the
 * covered scene out to -[WindowMotionOffset] without fading it. Closing is the mirror: the
 * revealed scene slides in from -[WindowMotionOffset] without fading while the closing scene
 * slides out to +[WindowMotionOffset] and fades.
 */
internal fun activityTransition(slidePx: Int, forward: Boolean): ContentTransform {
    val slideSpec = tween<IntOffset>(SLIDE_MILLIS, easing = EmphasizedEasing)
    val enter: EnterTransition
    val exit: ExitTransition
    if (forward) {
        enter = slideInHorizontally(animationSpec = slideSpec) { slidePx } +
            fadeIn(
                animationSpec = tween(
                    FADE_MILLIS,
                    delayMillis = FADE_IN_DELAY_MILLIS,
                    easing = LinearEasing,
                ),
            )
        exit = slideOutHorizontally(animationSpec = slideSpec) { -slidePx }
    } else {
        enter = slideInHorizontally(animationSpec = slideSpec) { -slidePx }
        exit = slideOutHorizontally(animationSpec = slideSpec) { slidePx } +
            fadeOut(
                animationSpec = tween(
                    FADE_MILLIS,
                    delayMillis = FADE_OUT_DELAY_MILLIS,
                    easing = LinearEasing,
                ),
            )
    }
    return ContentTransform(enter, exit)
}

// The platform's fast_out_extra_slow_in path interpolator:
// M 0,0 C 0.05, 0, 0.133333, 0.06, 0.166666, 0.4 C 0.208333, 0.82, 0.25, 1, 1, 1
@Suppress("MagicNumber")
internal val EmphasizedEasing = Easing { input ->
    val x = input.coerceIn(0f, 1f)
    if (x <= 0.166666f) {
        cubicCurveYAtX(
            x = x,
            x0 = 0f,
            x1 = 0.05f,
            x2 = 0.133333f,
            x3 = 0.166666f,
            y0 = 0f,
            y1 = 0f,
            y2 = 0.06f,
            y3 = 0.4f,
        )
    } else {
        cubicCurveYAtX(
            x = x,
            x0 = 0.166666f,
            x1 = 0.208333f,
            x2 = 0.25f,
            x3 = 1f,
            y0 = 0.4f,
            y1 = 0.82f,
            y2 = 1f,
            y3 = 1f,
        )
    }
}

/** Evaluates the y of a cubic bezier at the parameter where x matches. */
@Suppress("MagicNumber")
private fun cubicCurveYAtX(
    x: Float,
    x0: Float,
    x1: Float,
    x2: Float,
    x3: Float,
    y0: Float,
    y1: Float,
    y2: Float,
    y3: Float,
): Float {
    var low = 0f
    var high = 1f
    repeat(24) {
        val t = (low + high) / 2f
        if (cubicCurveValue(t, x0, x1, x2, x3) < x) low = t else high = t
    }
    return cubicCurveValue((low + high) / 2f, y0, y1, y2, y3)
}

@Suppress("MagicNumber")
private fun cubicCurveValue(t: Float, p0: Float, p1: Float, p2: Float, p3: Float): Float {
    val u = 1f - t
    return u * u * u * p0 + 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t * p3
}

/**
 * AOSP's activity animation edge extension: the platform samples the window's 1px edge and
 * stretches it into the vacated strip (SurfaceAnimationRunner#edgeExtendWindow).
 */
internal fun DrawScope.drawEdgeExtension(layer: GraphicsLayer, left: Boolean) {
    val strip = WindowMotionOffset.toPx()
    if (strip <= 0f) {
        return
    }
    if (left) {
        // activity_open_enter/close_exit extend the edit window to the left.
        clipRect(left = -strip, top = 0f, right = 0f, bottom = size.height) {
            withTransform({
                translate(-strip, 0f)
                scale(strip, 1f, pivot = Offset.Zero)
            }) {
                drawLayer(layer)
            }
        }
    } else {
        // activity_open_exit/close_enter extend the covered page to the right.
        val right = size.width
        clipRect(left = right, top = 0f, right = right + strip, bottom = size.height) {
            withTransform({
                translate(1f, 0f)
                scale(strip, 1f, pivot = Offset(right - 1f, 0f))
            }) {
                drawLayer(layer)
            }
        }
    }
}

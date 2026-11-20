@file:Suppress("MagicNumber")

package com.berryos.music.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.navigationevent.NavigationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// AOSP's default cross-activity back animation: WM Shell's DefaultCrossActivityBackAnimation.
// The closing window scales to 0.9 and moves to the screen edge, the revealed window starts 96dp
// off-screen and scales in sync, and the commit runs a 450ms emphasized settle.
private const val SCALED_WINDOW_SCALE = 0.9f
private const val ALPHA_FADE_FACTOR = 5f
private const val POST_COMMIT_MILLIS = 450
private const val RESET_TAIL_MILLIS = 100L
private const val VELOCITY_HORIZON_MILLIS = 100L
private const val SAMPLE_LIFETIME_MILLIS = 150L
private const val MAX_SAMPLES = 32
private const val SPRING_VALUE_SCALE = 100f
private const val FLING_MIN_PROGRESS = 0.1f
private const val FLING_MIN_VELOCITY = 120f
private const val FLING_MAX_VELOCITY = 1000f
private const val EDGE_VELOCITY_FACTOR = 2f
private const val FLING_VISIBILITY_THRESHOLD = 0.001f

/** How long the scene transition must stay alive for the post-commit settle. */
internal const val PREDICTIVE_BACK_HOLD_MILLIS = 500

private val DisplayBoundsMargin = 8.dp

// android.view.animation.BackGestureInterpolator: PathInterpolator(0.1, 0.1, 0, 1).
private val BackGestureEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

// android.view.animation.DecelerateInterpolator.
private val DecelerateEasing = Easing { input -> 1f - (1f - input) * (1f - input) }

@Composable
internal fun rememberPredictiveBackTransition(): PredictiveBackTransition {
    val scope = rememberCoroutineScope()
    return remember(scope) { PredictiveBackTransition(scope) }
}

/**
 * Follows a predictive back gesture with AOSP's cross-activity geometry and settles the committed
 * gesture with the platform's post-commit animation.
 */
internal class PredictiveBackTransition(private val scope: CoroutineScope) {
    var active by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var settling by mutableStateOf(false)
        private set
    var settleProgress by mutableFloatStateOf(0f)
        private set
    var flingScale by mutableFloatStateOf(1f)
        private set
    var touchDeltaY by mutableFloatStateOf(0f)
        private set
    var startProgress by mutableFloatStateOf(0f)
        private set
    var startTouchDeltaY by mutableFloatStateOf(0f)
        private set
    var swipeEdge by mutableIntStateOf(NavigationEvent.EDGE_NONE)
        private set

    private var sawSwipe = false
    private var initialTouchY = 0f
    private var settleJob: Job? = null
    private var flingJob: Job? = null
    private val samples = ArrayDeque<ProgressSample>()

    fun onGestureProgress(event: NavigationEvent) {
        if (event.swipeEdge == NavigationEvent.EDGE_NONE) {
            return
        }
        if (!active) {
            active = true
            sawSwipe = true
            initialTouchY = event.touchY
            swipeEdge = event.swipeEdge
            samples.clear()
        }
        if (settling) {
            settleJob?.cancel()
            flingJob?.cancel()
            settling = false
            settleProgress = 0f
            flingScale = 1f
        }
        progress = BackGestureEasing.transform(event.progress)
        touchDeltaY = event.touchY - initialTouchY
        recordSample(event.frameTimeMillis, event.progress)
    }

    fun onIdle() {
        if (settling || !active) {
            return
        }
        clearGesture()
    }

    /**
     * Hands the committed gesture over to the post-commit animation.
     *
     * The scene transition must be told before the back stack pops, so the pop can keep the
     * outgoing scene composed for the duration of the settle.
     */
    fun commit() {
        if (!active || !sawSwipe || settling) {
            return
        }
        startProgress = progress
        startTouchDeltaY = touchDeltaY
        settleProgress = 0f
        settling = true
        startFling(flingVelocity())
        settleJob = scope.launch {
            Animatable(0f).animateTo(
                targetValue = 1f,
                animationSpec = tween(POST_COMMIT_MILLIS, easing = EmphasizedEasing),
            ) {
                settleProgress = value
            }
            flingJob?.cancel()
            flingJob = null
            flingScale = 1f
            // The platform drops the closing window when its animation ends; the scene transition
            // needs a little longer before it disposes the outgoing entry.
            delay(RESET_TAIL_MILLIS)
            clearGesture()
            settling = false
        }
    }

    private fun startFling(velocity: Float) {
        flingJob = scope.launch {
            Animatable(1f).animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow,
                    visibilityThreshold = FLING_VISIBILITY_THRESHOLD,
                ),
                initialVelocity = -velocity / SPRING_VALUE_SCALE,
            ) {
                flingScale = value.coerceAtMost(1f)
            }
            flingScale = 1f
        }
    }

    private fun clearGesture() {
        active = false
        sawSwipe = false
        progress = 0f
        touchDeltaY = 0f
        samples.clear()
    }

    /** Gesture velocity in progress per second, matching ProgressVelocityTracker's horizon. */
    @Suppress("ReturnCount")
    private fun flingVelocity(): Float {
        val last = samples.lastOrNull()?.timeMillis ?: return 0f
        val window = samples.filter { last - it.timeMillis <= VELOCITY_HORIZON_MILLIS }
        if (window.size < 2) {
            return 0f
        }
        val meanTime = window.map { it.timeMillis.toDouble() }.average()
        val meanProgress = window.map { it.progress.toDouble() }.average()
        var numerator = 0.0
        var denominator = 0.0
        window.forEach { sample ->
            val time = sample.timeMillis - meanTime
            numerator += time * (sample.progress - meanProgress)
            denominator += time * time
        }
        if (denominator == 0.0) {
            return 0f
        }
        val velocity = (numerator / denominator * 1000.0).toFloat()
        var startVelocity = velocity * SPRING_VALUE_SCALE * (1f - SCALED_WINDOW_SCALE) * EDGE_VELOCITY_FACTOR
        if (progress < FLING_MIN_PROGRESS) {
            startVelocity = max(startVelocity, FLING_MIN_VELOCITY)
        }
        return startVelocity.coerceIn(0f, FLING_MAX_VELOCITY)
    }

    private fun recordSample(timeMillis: Long, progress: Float) {
        if (timeMillis <= 0L) {
            return
        }
        samples.addLast(ProgressSample(timeMillis, progress))
        while (
            samples.size > MAX_SAMPLES ||
            (samples.size > 1 && timeMillis - samples.first().timeMillis > SAMPLE_LIFETIME_MILLIS)
        ) {
            samples.removeFirst()
        }
    }

    private data class ProgressSample(val timeMillis: Long, val progress: Float)
}

internal fun GraphicsLayerScope.applyPredictiveBackTransform(transition: PredictiveBackTransition, editPage: Boolean) {
    if (!transition.active) {
        compositingStrategy = CompositingStrategy.Auto
        return
    }
    // Render into an offscreen buffer at the page's own resolution before scaling, so the card
    // edge stays hard instead of being anti-aliased against the backdrop like a vector rect.
    compositingStrategy = CompositingStrategy.Offscreen
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) {
        return
    }
    val margin = DisplayBoundsMargin.toPx()
    val enterOffset = WindowMotionOffset.toPx()
    val geometry = if (transition.settling) {
        committedGeometry(transition, editPage, width, height, margin, enterOffset)
    } else {
        gestureGeometry(transition, editPage, width, height, margin, enterOffset)
    }
    transformOrigin = TransformOrigin(0f, 0f)
    scaleX = geometry.scale
    scaleY = geometry.scale
    translationX = geometry.offsetX
    translationY = geometry.offsetY
    alpha = if (editPage && transition.settling) {
        max(1f - ALPHA_FADE_FACTOR * transition.settleProgress, 0f)
    } else {
        1f
    }
}

private fun gestureGeometry(
    transition: PredictiveBackTransition,
    editPage: Boolean,
    width: Float,
    height: Float,
    margin: Float,
    enterOffset: Float,
): LayerTransform = geometryAt(
    progress = transition.progress,
    editPage = editPage,
    swipeEdge = transition.swipeEdge,
    touchDeltaY = transition.touchDeltaY,
    width = width,
    height = height,
    margin = margin,
    enterOffset = enterOffset,
)

/** AOSP's pre-commit geometry for a page at [progress]. */
private fun geometryAt(
    progress: Float,
    editPage: Boolean,
    swipeEdge: Int,
    touchDeltaY: Float,
    width: Float,
    height: Float,
    margin: Float,
    enterOffset: Float,
): LayerTransform {
    val scale = lerp(1f, SCALED_WINDOW_SCALE, progress)
    val yOffset = yOffset(
        containerHeight = height,
        rectHeight = scale * height,
        touchDeltaY = touchDeltaY,
        margin = margin,
    )
    return if (editPage) {
        val left = if (swipeEdge != NavigationEvent.EDGE_RIGHT) {
            progress * (0.1f * width - margin)
        } else {
            progress * 0.05f * width
        }
        LayerTransform(scale, left, (1f - scale) * height / 2f + yOffset)
    } else {
        LayerTransform(
            scale = scale,
            offsetX = -enterOffset + progress * 0.05f * width,
            offsetY = progress * 0.05f * height + yOffset,
        )
    }
}

private fun committedGeometry(
    transition: PredictiveBackTransition,
    editPage: Boolean,
    width: Float,
    height: Float,
    margin: Float,
    enterOffset: Float,
): LayerTransform {
    val start = geometryAt(
        progress = transition.startProgress,
        editPage = editPage,
        swipeEdge = transition.swipeEdge,
        touchDeltaY = transition.startTouchDeltaY,
        width = width,
        height = height,
        margin = margin,
        enterOffset = enterOffset,
    )
    val eased = EmphasizedEasing.transform(transition.settleProgress)
    val left = lerp(start.offsetX, if (editPage) start.offsetX + enterOffset else 0f, eased)
    val top = lerp(start.offsetY, 0f, eased)
    val rectWidth = lerp(start.scale * width, width, eased)
    val rectHeight = lerp(start.scale * height, height, eased)
    val fling = transition.flingScale
    val scaledWidth = rectWidth * fling
    val scaledHeight = rectHeight * fling
    return LayerTransform(
        scale = scaledWidth / width,
        offsetX = left + (rectWidth - scaledWidth) / 2f,
        offsetY = top + (rectHeight - scaledHeight) / 2f,
    )
}

private fun yOffset(containerHeight: Float, rectHeight: Float, touchDeltaY: Float, margin: Float): Float {
    if (touchDeltaY == 0f) {
        return 0f
    }
    val half = containerHeight / 2f
    val ratio = min(half, abs(touchDeltaY)) / half
    val interpolated = DecelerateEasing.transform(ratio)
    val maxShift = max(0f, (containerHeight - rectHeight) / 2f - margin)
    val direction = if (touchDeltaY < 0f) -1f else 1f
    return maxShift * interpolated * direction
}

private class LayerTransform(val scale: Float, val offsetX: Float, val offsetY: Float)

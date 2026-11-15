package com.berryos.music.app

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationEventState
import art.rippledesign.compose.rui.container.RContainerTransform
import art.rippledesign.compose.rui.container.RContainerTransformSource
import art.rippledesign.compose.rui.container.RContainerTransformSourceState
import art.rippledesign.compose.rui.container.RContainerTransformSpec
import art.rippledesign.compose.rui.container.RContainerTransformState
import com.berryos.music.core.model.SourceKey
import kotlinx.coroutines.flow.filterNotNull

internal data class ContainerTransformKey(val kind: String, val source: SourceKey, val id: String)

internal fun albumTransformKey(source: SourceKey, albumId: String): ContainerTransformKey =
    ContainerTransformKey(kind = "album", source = source, id = albumId)

internal fun artistTransformKey(source: SourceKey, artistId: String): ContainerTransformKey =
    ContainerTransformKey(kind = "artist", source = source, id = artistId)

@Stable
internal class ContainerTransformController {
    private val sources = mutableMapOf<ContainerTransformKey, RContainerTransformSourceState>()

    var activeKey by mutableStateOf<ContainerTransformKey?>(null)
        private set

    var hiddenKey by mutableStateOf<ContainerTransformKey?>(null)
        private set

    var transitionRunning by mutableStateOf(false)
        private set

    var transformEntering by mutableStateOf(true)
        private set

    var gestureCancelCount by mutableIntStateOf(0)
        private set

    fun sourceState(key: ContainerTransformKey): RContainerTransformSourceState =
        sources.getOrPut(key) { RContainerTransformSourceState() }

    fun request(key: ContainerTransformKey): RContainerTransformSourceState {
        val state = sourceState(key)
        state.beginCapture()
        activeKey = key
        transitionRunning = true
        return state
    }

    fun beginTransition(key: ContainerTransformKey, entering: Boolean) {
        if (activeKey == key) {
            transitionRunning = true
            transformEntering = entering
        }
    }

    fun endTransition(key: ContainerTransformKey) {
        if (activeKey == key) {
            transitionRunning = false
            transformEntering = true
        }
    }

    fun notifyGestureCancelled() {
        gestureCancelCount++
    }

    fun markSourceHidden(key: ContainerTransformKey) {
        if (activeKey == key) {
            hiddenKey = key
        }
    }

    fun markSourceVisible(key: ContainerTransformKey) {
        if (hiddenKey == key) {
            hiddenKey = null
        }
    }

    fun finish(key: ContainerTransformKey) {
        if (activeKey == key) {
            activeKey = null
            transitionRunning = false
            transformEntering = true
        }
        if (hiddenKey == key) {
            hiddenKey = null
        }
        sourceState(key).stopCapture()
    }
}

internal val ContainerTransformSpec = RContainerTransformSpec()

@Composable
internal fun rememberContainerTransformController(): ContainerTransformController =
    remember { ContainerTransformController() }

internal val LocalContainerTransformNavigationEventState =
    staticCompositionLocalOf<NavigationEventState<SceneInfo<NavKey>>?> { null }

private val CancelEasing: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)

private const val CANCEL_DURATION_MILLIS = 150

/**
 * Keeps the current nav entry composed for the duration of a container transform.
 *
 * Nav3 disposes the outgoing scene as soon as its own enter/exit animations settle. A container
 * transform renders outside the scene transition, so without this hold the outgoing screen would
 * disappear on the first frame of the transform.
 */
@Composable
internal fun rememberContainerTransformHold(active: Boolean) {
    if (!active) {
        return
    }
    val transition = LocalNavAnimatedContentScope.current.transition
    transition.animateFloat(
        transitionSpec = { tween(ContainerTransformSpec.enterDurationMillis, easing = LinearEasing) },
        label = "containerTransformHold",
        targetValueByState = { if (it == EnterExitState.Visible) 1f else 0f },
    )
}

/**
 * Wraps a detail page in a container transform.
 *
 * The transform owns its progress animation and only reads the navigation transition to learn when
 * the page starts leaving. Predictive back progress is taken straight from the navigation event so
 * the container follows the gesture instead of the deferred scene transition.
 *
 * [content] stays in the same composition slot for the whole lifetime of the entry, so the page
 * keeps its state while it grows out of the source card and shrinks back into it.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun ContainerTransformEntry(
    key: ContainerTransformKey,
    controller: ContainerTransformController,
    content: @Composable () -> Unit,
) {
    val navigationEventState = checkNotNull(LocalContainerTransformNavigationEventState.current)
    val sourceState = controller.sourceState(key)
    val sourceLayer = sourceState.layer
    val boundsReady = sourceState.boundsInWindow != null
    val transformState = remember(key) { RContainerTransformState() }
    val transition = LocalNavAnimatedContentScope.current.transition
    val activeKey = controller.activeKey == key
    val progress = remember(key) { Animatable(0f) }
    var armed by remember(key) { mutableStateOf(false) }
    var gestureInitiated by remember(key) { mutableStateOf(false) }
    val exiting = transition.targetState == EnterExitState.PostExit
    val gestureActive by remember(navigationEventState) {
        derivedStateOf { navigationEventState.transitionState is NavigationEventTransitionState.InProgress }
    }
    val cancelCount = controller.gestureCancelCount

    rememberContainerTransformHold(active = activeKey)

    LaunchedEffect(key, activeKey, boundsReady) {
        if (!activeKey || armed) {
            return@LaunchedEffect
        }
        val bounds = sourceState.boundsInWindow ?: return@LaunchedEffect
        transformState.start(bounds, entering = true)
        armed = true
        controller.beginTransition(key, entering = true)
        controller.markSourceHidden(key)
        sourceState.beginCapture()
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(ContainerTransformSpec.enterDurationMillis, easing = ContainerTransformSpec.easing),
        )
        transformState.finish()
        sourceState.stopCapture()
        controller.endTransition(key)
    }
    LaunchedEffect(key, activeKey, exiting) {
        if (!activeKey || !exiting) {
            return@LaunchedEffect
        }
        val bounds = sourceState.boundsInWindow ?: return@LaunchedEffect
        if (transformState.active) {
            transformState.beginReturn()
        } else {
            transformState.start(bounds, entering = false)
        }
        armed = true
        gestureInitiated = false
        controller.beginTransition(key, entering = false)
        sourceState.beginCapture()
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(ContainerTransformSpec.returnDurationMillis, easing = ContainerTransformSpec.easing),
        )
        controller.markSourceVisible(key)
        transformState.requestSourceSnap()
        controller.endTransition(key)
    }
    LaunchedEffect(key, activeKey, gestureActive) {
        if (!activeKey || !gestureActive) {
            return@LaunchedEffect
        }
        val bounds = sourceState.boundsInWindow ?: return@LaunchedEffect
        if (transformState.active) {
            transformState.beginReturn()
        } else {
            transformState.start(bounds, entering = false)
        }
        armed = true
        gestureInitiated = true
        controller.beginTransition(key, entering = false)
        sourceState.beginCapture()
    }
    LaunchedEffect(key, activeKey) {
        if (!activeKey) {
            return@LaunchedEffect
        }
        snapshotFlow {
            (navigationEventState.transitionState as? NavigationEventTransitionState.InProgress)
                ?.latestEvent
                ?.progress
        }
            .filterNotNull()
            .collect { value ->
                if (transformState.active && !transformState.entering) {
                    progress.snapTo(1f - value.coerceIn(0f, 1f))
                }
            }
    }
    LaunchedEffect(key, cancelCount) {
        if (!gestureInitiated || cancelCount == 0) {
            return@LaunchedEffect
        }
        gestureInitiated = false
        if (transition.targetState == EnterExitState.PostExit) {
            return@LaunchedEffect
        }
        transformState.beginEnter()
        controller.beginTransition(key, entering = true)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(CANCEL_DURATION_MILLIS, easing = CancelEasing),
        )
        transformState.finish()
        sourceState.stopCapture()
        controller.endTransition(key)
    }
    DisposableEffect(key) {
        onDispose { controller.finish(key) }
    }
    val pending = activeKey && !armed
    RContainerTransform(
        state = transformState,
        progress = { if (transformState.snapToSource) 0f else progress.value },
        spec = ContainerTransformSpec,
        source = sourceLayer?.let { RContainerTransformSource.Layer(it) },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (pending) 0f else 1f },
            ) {
                content()
            }
        },
    )
}

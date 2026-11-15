package com.berryos.music.feature.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** [FastOutSlowInEasing] sampled in reverse time, i.e. the same curve played backwards. */
private val ReverseStandardEasing = Easing { fraction -> 1f - FastOutSlowInEasing.transform(1f - fraction) }

/** Presentation phase of the playback header. Interactivity keys off this, not raw booleans. */
internal enum class HeaderPhase { Collapsed, Expanded, Collapsing }

/**
 * Orchestrates the media detail playback header transition.
 *
 * Forward (0-400ms): the FAB arcs to the header, the reveal grows from the live FAB position,
 * the title fades out, the band grows, then the controls and bottom text fade in.
 *
 * Reverse mirrors every window to `[400 - end, 400 - start]`. The FAB return samples the forward
 * easing backwards so that, at 250ms, it is back at the position where the forward reveal started;
 * the shrinking reveal therefore lands exactly on its own starting point without recording it.
 */
internal class MediaPlaybackHeaderState(private val scope: CoroutineScope, initiallyExpanded: Boolean) {
    var bandSize by mutableStateOf(IntSize.Zero)
    var expanded by mutableStateOf(initiallyExpanded)
        private set

    val phase: HeaderPhase
        get() = when {
            collapsing -> HeaderPhase.Collapsing
            expanded -> HeaderPhase.Expanded
            else -> HeaderPhase.Collapsed
        }

    // Kept separate from fabArrived so the FAB shadow returns only after the reverse reveal finishes.
    var containerVisible by mutableStateOf(!initiallyExpanded)
        private set
    var fabArrived by mutableStateOf(initiallyExpanded)
        private set
    var revealed by mutableStateOf(initiallyExpanded)
        private set
    var collapsing by mutableStateOf(false)
        private set
    val fabMoveProgress = Animatable(if (initiallyExpanded) 1f else 0f)
    val playFabScale = Animatable(if (initiallyExpanded) 1f else 0f)
    val playFabAlpha = Animatable(if (initiallyExpanded) 1f else 0f)
    val playFabIconScale = Animatable(if (initiallyExpanded) 1f else 0f)
    val controlsProgress = Animatable(if (initiallyExpanded) 1f else 0f)
    val volumeProgress = Animatable(if (initiallyExpanded) 1f else 0f)
    val topStripProgress = Animatable(if (initiallyExpanded) 1f else 0f)
    val bandExpandProgress = Animatable(if (initiallyExpanded) 1f else 0f)
    val titleFadeProgress = Animatable(if (initiallyExpanded) 1f else 0f)
    val bottomTextProgress = Animatable(if (initiallyExpanded) 1f else 0f)
    val revealShadowProgress = Animatable(if (initiallyExpanded) 1f else 0f)

    fun start(onPlay: () -> Unit) {
        if (expanded) {
            return
        }
        expanded = true
        containerVisible = true
        onPlay()
        scope.launch {
            launch { animateFabMove() }
            launch { animatePlayFabIn() }
            launch { revealControls() }
            launch { animateVolume() }
            launch { animateTopStrip() }
            launch { animateBandExpand() }
            launch { animateTitleFade() }
            launch { animateBottomText() }
            launch { animateRevealShadow() }
        }
    }

    fun collapse() {
        if (!expanded || collapsing) {
            return
        }
        collapsing = true
        revealed = false
        containerVisible = false
        scope.launch {
            launch { playFabScale.animateTo(0f, tween(PLAY_FAB_HIDE_SCALE_MILLIS, easing = FastOutLinearInEasing)) }
            launch {
                playFabAlpha.animateTo(
                    0f,
                    tween(
                        PLAY_FAB_HIDE_ALPHA_MILLIS,
                        delayMillis = PLAY_FAB_HIDE_ALPHA_DELAY_MILLIS,
                        easing = LinearEasing,
                    ),
                )
            }
            launch {
                playFabIconScale.animateTo(0f, tween(PLAY_FAB_HIDE_ICON_MILLIS, easing = FastOutLinearInEasing))
            }
            launch { controlsProgress.animateTo(0f, tween(REVEAL_MILLIS, easing = FastOutLinearInEasing)) }
            launch { volumeProgress.animateTo(0f, tween(VOLUME_FADE_MILLIS, easing = FastOutLinearInEasing)) }
            launch { topStripProgress.animateTo(0f, tween(TOP_STRIP_MILLIS, easing = FastOutLinearInEasing)) }
            launch { bandExpandProgress.animateTo(0f, tween(BAND_EXPAND_MILLIS, easing = FastOutSlowInEasing)) }
            launch { bottomTextProgress.animateTo(0f, tween(BOTTOM_TEXT_MILLIS, easing = FastOutLinearInEasing)) }
            launch { revealShadowProgress.animateTo(0f, tween(SHADOW_FADE_MILLIS, easing = FastOutLinearInEasing)) }
            launch {
                delay(REVEAL_START_MILLIS.toLong())
                fabArrived = false
                fabMoveProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(FAB_MOVE_MILLIS, easing = ReverseStandardEasing),
                )
            }
            launch {
                // The reveal finishes shrinking at 250ms; only then hand the shadow back to the FAB.
                delay(REVEAL_MILLIS.toLong())
                containerVisible = true
            }
            launch {
                delay(TITLE_FADE_MILLIS.toLong())
                titleFadeProgress.animateTo(0f, tween(TITLE_FADE_MILLIS, easing = LinearOutSlowInEasing))
            }
            delay(BAND_EXPAND_MILLIS.toLong())
            expanded = false
            collapsing = false
        }
    }

    private suspend fun animateFabMove() {
        fabMoveProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(FAB_MOVE_MILLIS, easing = FastOutSlowInEasing),
        )
        fabArrived = true
        containerVisible = false
    }

    private suspend fun revealControls() {
        delay(REVEAL_START_MILLIS.toLong())
        revealed = true
        controlsProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(REVEAL_MILLIS, easing = LinearOutSlowInEasing),
        )
    }

    /**
     * Opens the expanded play FAB, following MDC's `mtrl_fab_show_motion_spec`: scale with
     * LinearOutSlowIn, a quick 15ms fade, and a separate icon scale that starts later.
     */
    private suspend fun animatePlayFabIn() {
        delay(REVEAL_START_MILLIS.toLong())
        scope.launch {
            playFabScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(PLAY_FAB_SHOW_SCALE_MILLIS, easing = LinearOutSlowInEasing),
            )
        }
        scope.launch {
            playFabAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    PLAY_FAB_SHOW_ALPHA_MILLIS,
                    delayMillis = PLAY_FAB_SHOW_ALPHA_DELAY_MILLIS,
                    easing = LinearEasing,
                ),
            )
        }
        scope.launch {
            playFabIconScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    PLAY_FAB_SHOW_ICON_MILLIS,
                    delayMillis = PLAY_FAB_SHOW_ICON_DELAY_MILLIS,
                    easing = LinearOutSlowInEasing,
                ),
            )
        }
    }

    private suspend fun animateVolume() {
        delay(VOLUME_START_MILLIS.toLong())
        volumeProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(VOLUME_FADE_MILLIS, easing = LinearOutSlowInEasing),
        )
    }

    private suspend fun animateTopStrip() {
        delay(TOP_STRIP_START_MILLIS.toLong())
        topStripProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(TOP_STRIP_MILLIS, easing = LinearOutSlowInEasing),
        )
    }

    private suspend fun animateBandExpand() {
        bandExpandProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(BAND_EXPAND_MILLIS, easing = FastOutSlowInEasing),
        )
    }

    private suspend fun animateTitleFade() {
        titleFadeProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(TITLE_FADE_MILLIS, easing = FastOutLinearInEasing),
        )
    }

    private suspend fun animateBottomText() {
        delay(BOTTOM_TEXT_START_MILLIS.toLong())
        bottomTextProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(BOTTOM_TEXT_MILLIS, easing = LinearOutSlowInEasing),
        )
    }

    private suspend fun animateRevealShadow() {
        delay(SHADOW_START_MILLIS.toLong())
        revealShadowProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(SHADOW_FADE_MILLIS, easing = LinearOutSlowInEasing),
        )
    }
}

// Forward windows in milliseconds. Reverse windows are their mirror in 0..400.
internal const val FAB_MOVE_MILLIS = 250
internal const val ICON_MORPH_MILLIS = 250
internal const val REVEAL_START_MILLIS = 150
internal const val REVEAL_MILLIS = 250
internal const val VOLUME_START_MILLIS = 250
internal const val VOLUME_FADE_MILLIS = 150
internal const val TOP_STRIP_START_MILLIS = 200
internal const val TOP_STRIP_MILLIS = 200
internal const val BAND_EXPAND_MILLIS = 400
internal const val TITLE_FADE_MILLIS = 175
internal const val BOTTOM_TEXT_START_MILLIS = 175
internal const val BOTTOM_TEXT_MILLIS = 225
internal const val SHADOW_START_MILLIS = 300
internal const val SHADOW_FADE_MILLIS = 100

// MDC mtrl_fab_show_motion_spec / mtrl_fab_hide_motion_spec, relative to the FAB itself.
internal const val PLAY_FAB_SHOW_SCALE_MILLIS = 330
internal const val PLAY_FAB_SHOW_ALPHA_DELAY_MILLIS = 30
internal const val PLAY_FAB_SHOW_ALPHA_MILLIS = 15
internal const val PLAY_FAB_SHOW_ICON_DELAY_MILLIS = 90
internal const val PLAY_FAB_SHOW_ICON_MILLIS = 240
internal const val PLAY_FAB_HIDE_SCALE_MILLIS = 135
internal const val PLAY_FAB_HIDE_ALPHA_DELAY_MILLIS = 150
internal const val PLAY_FAB_HIDE_ALPHA_MILLIS = 15
internal const val PLAY_FAB_HIDE_ICON_MILLIS = 180

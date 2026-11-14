package com.berryos.music.feature.album

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** [FastOutSlowInEasing] sampled in reverse time, i.e. the same curve played backwards. */
private val ReverseStandardEasing = Easing { fraction -> 1f - FastOutSlowInEasing.transform(1f - fraction) }

/**
 * Orchestrates the album playback header transition.
 *
 * Forward (0-400ms): the FAB arcs to the header, the reveal grows from the live FAB position,
 * the title fades out, the band grows, then the controls and bottom text fade in.
 *
 * Reverse mirrors every window to `[400 - end, 400 - start]`. The FAB return samples the forward
 * easing backwards so that, at 250ms, it is back at the position where the forward reveal started;
 * the shrinking reveal therefore lands exactly on its own starting point without recording it.
 */
internal class AlbumPlaybackHeaderState(private val scope: CoroutineScope) {
    var bandSize by mutableStateOf(IntSize.Zero)
    var expanding by mutableStateOf(false)
        private set

    // Kept separate from fabDone so the FAB shadow returns only after the reverse reveal finishes.
    var containerVisible by mutableStateOf(true)
        private set
    var iconPlaying by mutableStateOf(false)
        private set
    var fabDone by mutableStateOf(false)
        private set
    var revealed by mutableStateOf(false)
        private set
    var collapsing by mutableStateOf(false)
        private set
    val fabMoveProgress = Animatable(0f)
    val controlsProgress = Animatable(0f)
    val volumeProgress = Animatable(0f)
    val topStripProgress = Animatable(0f)
    val bandExpandProgress = Animatable(0f)
    val titleFadeProgress = Animatable(0f)
    val bottomTextProgress = Animatable(0f)

    fun start(onPlay: () -> Unit) {
        if (expanding) {
            return
        }
        expanding = true
        iconPlaying = true
        containerVisible = true
        onPlay()
        scope.launch {
            launch { animateFabMove() }
            launch { revealControls() }
            launch { animateVolume() }
            launch { animateTopStrip() }
            launch { animateBandExpand() }
            launch { animateTitleFade() }
            launch { animateBottomText() }
        }
    }

    fun collapse(onTogglePlayPause: () -> Unit) {
        if (!expanding || collapsing) {
            return
        }
        collapsing = true
        revealed = false
        containerVisible = false
        onTogglePlayPause()
        scope.launch {
            launch { controlsProgress.animateTo(0f, tween(REVEAL_MILLIS, easing = FastOutLinearInEasing)) }
            launch { volumeProgress.animateTo(0f, tween(VOLUME_FADE_MILLIS, easing = FastOutLinearInEasing)) }
            launch { topStripProgress.animateTo(0f, tween(TOP_STRIP_MILLIS, easing = FastOutLinearInEasing)) }
            launch { bandExpandProgress.animateTo(0f, tween(BAND_EXPAND_MILLIS, easing = FastOutSlowInEasing)) }
            launch { bottomTextProgress.animateTo(0f, tween(BOTTOM_TEXT_MILLIS, easing = FastOutLinearInEasing)) }
            launch {
                delay(REVEAL_START_MILLIS.toLong())
                iconPlaying = false
                fabDone = false
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
            expanding = false
            collapsing = false
        }
    }

    private suspend fun animateFabMove() {
        fabMoveProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(FAB_MOVE_MILLIS, easing = FastOutSlowInEasing),
        )
        fabDone = true
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
}

@Composable
internal fun rememberAlbumPlaybackHeaderState(): AlbumPlaybackHeaderState {
    val scope = rememberCoroutineScope()
    return remember(scope) { AlbumPlaybackHeaderState(scope) }
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

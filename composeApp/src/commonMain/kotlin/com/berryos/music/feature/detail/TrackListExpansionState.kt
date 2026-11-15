package com.berryos.music.feature.detail

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Expansion of the track list card over the playback header, like a bottom sheet.
 *
 * Dragging up from the top of the list grows the card until its top reaches the app bar;
 * dragging down shrinks it back before the list starts scrolling. Expansion is only possible
 * while the list content is taller than the space below the header.
 */
internal class TrackListExpansionState(
    private val scope: CoroutineScope,
    private val listState: LazyListState,
    private val appBarBottomPx: Int,
) {
    var containerHeight by mutableIntStateOf(0)
    var headerHeight by mutableIntStateOf(0)
    var cardHeight by mutableIntStateOf(0)
    var expansion by mutableFloatStateOf(0f)
        private set

    private var settleJob: Job? = null

    val expandable: Boolean
        get() = containerHeight > headerHeight &&
            headerHeight > appBarBottomPx &&
            cardHeight >= containerHeight - headerHeight

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.Drag || !expandable || listState.canScrollBackward) return Offset.Zero
            settleJob?.cancel()
            val extra = (headerHeight - appBarBottomPx).toFloat()
            val dy = available.y
            val consumed = when {
                dy < 0f && expansion < 1f -> drag(dy, extra, expand = true)
                dy > 0f && expansion > 0f -> drag(dy, extra, expand = false)
                else -> 0f
            }
            return Offset(0f, consumed)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!expandable || listState.canScrollBackward) return Velocity.Zero
            val target = when {
                available.y < 0f -> 1f
                available.y > 0f -> 0f
                expansion > 0.5f -> 1f
                else -> 0f
            }
            val settle = target != expansion
            if (settle) {
                settleJob?.cancel()
                settleJob = scope.launch {
                    animate(expansion, target, animationSpec = tween(EXPANSION_SETTLE_MILLIS)) { value, _ ->
                        expansion = value
                    }
                }
            }
            return if (settle) available else Velocity.Zero
        }
    }

    fun collapseIfNotExpandable() {
        if (!expandable && expansion != 0f) {
            settleJob?.cancel()
            expansion = 0f
        }
    }

    fun collapse() {
        if (expansion == 0f) return
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(expansion, 0f, animationSpec = tween(EXPANSION_SETTLE_MILLIS)) { value, _ ->
                expansion = value
            }
        }
    }

    private fun drag(dy: Float, extra: Float, expand: Boolean): Float {
        val consumed = if (expand) maxOf(dy, -extra * (1f - expansion)) else minOf(dy, extra * expansion)
        expansion = (expansion - consumed / extra).coerceIn(0f, 1f)
        return consumed
    }
}

internal const val CARD_SIZE_CHANGE_MILLIS = 250

private const val EXPANSION_SETTLE_MILLIS = 200

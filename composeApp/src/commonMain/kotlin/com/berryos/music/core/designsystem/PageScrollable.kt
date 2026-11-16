package com.berryos.music.core.designsystem

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import kotlin.math.roundToInt

/**
 * Whether the current page's content can scroll. The app bar only moves while the content can
 * scroll, matching MDC's AppBarLayout, whose scrolling child dispatches no nested scroll when its
 * content fits.
 */
val LocalPageScrollable = compositionLocalOf<MutableState<Boolean>?> { null }

/** Current app bar offset in pixels, so pages judge overflow against the expanded bar. */
val LocalTopBarOffsetPx = compositionLocalOf<State<Float>?> { null }

/** Reports the current page's scrollability to the app chrome. */
@Composable
fun ReportPageScrollable(scrollable: Boolean) {
    val target = LocalPageScrollable.current ?: return
    LaunchedEffect(target, scrollable) { target.value = scrollable }
}

/** Reports a vertical list's scrollability to the app chrome. */
@Composable
fun LazyListState.reportPageScrollable() {
    val barOffsetPx = LocalTopBarOffsetPx.current?.value?.roundToInt() ?: 0
    ReportPageScrollable(layoutInfo.contentOverflows(barOffsetPx))
}

/** Reports a vertical grid's scrollability to the app chrome. */
@Composable
fun LazyGridState.reportPageScrollable() {
    val barOffsetPx = LocalTopBarOffsetPx.current?.value?.roundToInt() ?: 0
    ReportPageScrollable(layoutInfo.contentOverflows(barOffsetPx))
}

/**
 * Whether the list content is taller than the viewport, ignoring the trailing inset padding (the
 * mini player area) and the space the collapsing app bar currently gives back, so the answer does
 * not change while the bar moves.
 */
fun LazyListLayoutInfo.contentOverflows(barOffsetPx: Int = 0): Boolean {
    val first = visibleItemsInfo.firstOrNull() ?: return false
    val last = visibleItemsInfo.last()
    return first.index > 0 ||
        last.index < totalItemsCount - 1 ||
        last.offset + last.size > viewportEndOffset - afterContentPadding - barOffsetPx
}

/** [LazyGridLayoutInfo] variant of [contentOverflows] for vertical grids. */
fun LazyGridLayoutInfo.contentOverflows(barOffsetPx: Int = 0): Boolean {
    val first = visibleItemsInfo.firstOrNull() ?: return false
    val last = visibleItemsInfo.last()
    return first.index > 0 ||
        last.index < totalItemsCount - 1 ||
        last.offset.y + last.size.height > viewportEndOffset - afterContentPadding - barOffsetPx
}

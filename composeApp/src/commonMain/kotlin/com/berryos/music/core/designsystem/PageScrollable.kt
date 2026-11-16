package com.berryos.music.core.designsystem

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf

/**
 * Whether the current page's content can scroll. The app bar only moves while the content can
 * scroll, matching MDC's AppBarLayout, whose scrolling child dispatches no nested scroll when its
 * content fits.
 */
val LocalPageScrollable = compositionLocalOf<MutableState<Boolean>?> { null }

/** Reports the current page's scrollability to the app chrome. */
@Composable
fun ReportPageScrollable(scrollable: Boolean) {
    val target = LocalPageScrollable.current ?: return
    LaunchedEffect(target, scrollable) { target.value = scrollable }
    DisposableEffect(target) {
        onDispose { target.value = true }
    }
}

/**
 * Whether the list content is taller than the viewport, ignoring the trailing inset padding (the
 * mini player area); lists whose content fits can still scroll into that padding.
 */
fun LazyListLayoutInfo.contentOverflows(): Boolean {
    val first = visibleItemsInfo.firstOrNull() ?: return false
    val last = visibleItemsInfo.last()
    return first.index > 0 ||
        last.index < totalItemsCount - 1 ||
        last.offset + last.size > viewportEndOffset - afterContentPadding
}

/** [LazyGridLayoutInfo] variant of [contentOverflows] for vertical grids. */
fun LazyGridLayoutInfo.contentOverflows(): Boolean {
    val first = visibleItemsInfo.firstOrNull() ?: return false
    val last = visibleItemsInfo.last()
    return first.index > 0 ||
        last.index < totalItemsCount - 1 ||
        last.offset.y + last.size.height > viewportEndOffset - afterContentPadding
}

package com.berryos.music.feature.detail

import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackListExpansionStateTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun expansionNeedsMoreContentThanTheSpaceBelowTheHeader() {
        val state = state(container = 1000, header = 500)
        state.contentHeight = 499
        assertFalse(state.expandable)

        state.contentHeight = 500
        assertTrue(state.expandable)
    }

    @Test
    fun expansionNeedsRoomBetweenTheAppBarAndTheHeader() {
        assertFalse(state(container = 1000, header = 40).apply { contentHeight = 1000 }.expandable)
        assertFalse(state(container = 400, header = 500).apply { contentHeight = 1000 }.expandable)
    }

    private fun state(container: Int, header: Int) = TrackListExpansionState(
        scope = scope,
        listState = LazyListState(),
        appBarBottomPx = APP_BAR_BOTTOM_PX,
    ).apply {
        containerHeight = container
        headerHeight = header
    }

    private companion object {
        const val APP_BAR_BOTTOM_PX = 100
    }
}

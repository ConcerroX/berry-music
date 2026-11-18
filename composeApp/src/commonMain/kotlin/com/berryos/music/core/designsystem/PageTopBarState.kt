package com.berryos.music.core.designsystem

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Top bar state owned by one route: scrollability and bar offset. */
@Stable
class PageTopBarState {
    val isScrollable = mutableStateOf(true)
    var topBarOffset by mutableStateOf(0f)
        private set
    var hasTopBarOffset by mutableStateOf(false)
        private set

    fun updateTopBarOffset(offset: Float) {
        topBarOffset = offset
        hasTopBarOffset = true
    }
}

/** Per-route top bar states; an outgoing page cannot overwrite the current page's top bar. */
@Stable
class PageTopBarStates {
    private val states = mutableStateMapOf<Any, PageTopBarState>()

    fun stateFor(owner: Any): PageTopBarState = states.getOrPut(owner) { PageTopBarState() }

    fun retainAll(owners: Set<Any>) {
        states.keys.retainAll(owners)
    }
}

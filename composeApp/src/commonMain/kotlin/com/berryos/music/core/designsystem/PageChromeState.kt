package com.berryos.music.core.designsystem

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Chrome state owned by one route: scrollability and bar offset. */
@Stable
class PageChromeState {
    val scrollable = mutableStateOf(true)
    var topBarOffset by mutableStateOf(0f)
        private set
    var hasTopBarOffset by mutableStateOf(false)
        private set

    fun updateTopBarOffset(offset: Float) {
        topBarOffset = offset
        hasTopBarOffset = true
    }
}

/** Per-route chrome state; an outgoing page cannot overwrite the current page's chrome. */
@Stable
class PageChromeRegistry {
    private val states = mutableStateMapOf<Any, PageChromeState>()

    fun state(owner: Any): PageChromeState = states.getOrPut(owner) { PageChromeState() }

    fun retain(owners: Set<Any>) {
        states.keys.retainAll(owners)
    }
}

package com.berryos.music.core.designsystem

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** App bar actions published by the page that owns them. */
@Stable
class AppBarActionState {
    var onSave: (() -> Unit)? by mutableStateOf(null)
    var saveEnabled: Boolean by mutableStateOf(false)
    var tabLabels: List<String> by mutableStateOf(emptyList())
    var selectedTabIndex: Int by mutableStateOf(0)
    var selectedTabPosition: Float by mutableStateOf(0f)
    var onTabSelected: (Int) -> Unit = {}
}

/** Chrome state owned by one route: scrollability, bar offset and app bar actions. */
@Stable
class PageChromeState {
    val actions = AppBarActionState()
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

val LocalPageChromeRegistry = staticCompositionLocalOf<PageChromeRegistry?> { null }

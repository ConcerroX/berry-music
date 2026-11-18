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

/** Per-page action state; an outgoing page cannot overwrite the current page's bar. */
@Stable
class AppBarActionRegistry {
    private val states = mutableStateMapOf<Any, AppBarActionState>()

    fun state(owner: Any): AppBarActionState = states.getOrPut(owner) { AppBarActionState() }

    fun retain(owners: Set<Any>) {
        states.keys.retainAll(owners)
    }
}

val LocalAppBarActionRegistry = staticCompositionLocalOf<AppBarActionRegistry?> { null }

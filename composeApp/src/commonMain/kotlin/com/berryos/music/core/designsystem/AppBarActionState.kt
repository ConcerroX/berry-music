package com.berryos.music.core.designsystem

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** App bar actions published by the currently visible screen. */
@Stable
class AppBarActionState {
    var onSave: (() -> Unit)? by mutableStateOf(null)
    var saveEnabled: Boolean by mutableStateOf(false)
    var tabLabels: List<String> by mutableStateOf(emptyList())
    var selectedTabIndex: Int by mutableStateOf(0)
    var selectedTabPosition: Float by mutableStateOf(0f)
    var onTabSelected: (Int) -> Unit = {}
}

val LocalAppBarActionState = staticCompositionLocalOf<AppBarActionState?> { null }

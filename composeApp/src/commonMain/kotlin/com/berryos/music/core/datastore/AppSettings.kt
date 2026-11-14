package com.berryos.music.core.datastore

import kotlinx.coroutines.flow.StateFlow

data class AppSettings(val collapsePlayerOnPause: Boolean = false)

interface AppSettingsRepository {
    val settings: StateFlow<AppSettings>

    fun setCollapsePlayerOnPause(enabled: Boolean)
}

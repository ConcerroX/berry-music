package com.berryos.music.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath

class DataStoreAppSettingsRepository(context: Context) : AppSettingsRepository {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(scope = scope) {
        context.applicationContext.filesDir.resolve(SETTINGS_FILE).toOkioPath()
    }
    private val _settings = MutableStateFlow(AppSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        scope.launch {
            dataStore.data.collect { preferences ->
                _settings.value = AppSettings(
                    collapsePlayerOnPause = preferences[CollapsePlayerOnPauseKey] ?: false,
                    showTechnicalDetails = preferences[ShowTechnicalDetailsKey] ?: false,
                )
            }
        }
    }

    override fun setCollapsePlayerOnPause(enabled: Boolean) {
        scope.launch {
            dataStore.edit { preferences -> preferences[CollapsePlayerOnPauseKey] = enabled }
        }
    }

    override fun setShowTechnicalDetails(enabled: Boolean) {
        scope.launch {
            dataStore.edit { preferences -> preferences[ShowTechnicalDetailsKey] = enabled }
        }
    }

    companion object {
        private val CollapsePlayerOnPauseKey = booleanPreferencesKey("collapse_player_on_pause")
        private val ShowTechnicalDetailsKey = booleanPreferencesKey("show_technical_details")
        private const val SETTINGS_FILE = "datastore/settings.preferences_pb"
    }
}

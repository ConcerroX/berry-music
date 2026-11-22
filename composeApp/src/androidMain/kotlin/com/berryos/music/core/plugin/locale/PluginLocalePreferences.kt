package com.berryos.music.core.plugin.locale

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.icu.util.ULocale
import com.berryos.music.core.plugin.i18n.PluginStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Application configuration honors both system and per-app language preferences. */
object PluginLocalePreferences {
    private val current = MutableStateFlow<List<String>>(emptyList())
    val preferences = current.asStateFlow()
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        PluginStrings.inferScript = { ULocale.addLikelySubtags(ULocale.forLocale(it)).script }
        update(context.resources.configuration)
        context.applicationContext.registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) = update(newConfig)

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = Unit
        })
    }

    fun update(configuration: Configuration) {
        current.value = (0 until configuration.locales.size()).map { configuration.locales[it].toLanguageTag() }
    }
}

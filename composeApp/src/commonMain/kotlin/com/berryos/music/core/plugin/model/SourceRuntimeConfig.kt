package com.berryos.music.core.plugin.model

import kotlinx.serialization.Serializable

// Ported from Lyrico (Apache License 2.0): com.lonx.lyrico.data.model.lyrics.SourceRuntimeConfig
@Serializable
data class SourceRuntimeConfig(
    val values: Map<String, String> = emptyMap(),
) {
    fun getString(key: String, defaultValue: String = ""): String {
        return values[key].takeUnless { it.isNullOrBlank() } ?: defaultValue
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return values[key]?.toBooleanStrictOrNull() ?: defaultValue
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return values[key]?.toIntOrNull() ?: defaultValue
    }
}

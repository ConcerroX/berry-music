package com.berryos.music.core.plugin.model

import kotlinx.serialization.Serializable

// Ported from Lyrico (Apache License 2.0): com.lonx.lyrico.data.model.plugin.PluginManifest
@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val versionCode: Int,
    val versionName: String,
    val author: String = "",
    val description: String = "",
    val apiVersion: Int,
    val minHostApiVersion: Int = 1,
    val entry: String = "source.js",
    val includeDirs: List<String> = emptyList(),
    val icon: String? = null,
    val capabilities: Set<PluginCapability> = emptySet(),
    val configFields: List<PluginConfigField> = emptyList(),
    val i18n: PluginI18n? = null,
    val updateUrl: String? = null,
)

@Serializable
data class PluginI18n(val defaultLocale: String, val resources: Map<String, String>)

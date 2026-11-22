package com.berryos.music.core.plugin.registry

import com.berryos.music.core.database.entity.SourcePluginEntity
import com.berryos.music.core.plugin.model.PluginCapability
import com.berryos.music.core.plugin.model.PluginSourceType
import kotlinx.serialization.json.Json

private val CapabilitiesJson = Json { ignoreUnknownKeys = true }

val SourcePluginEntity.capabilities: Set<PluginCapability>
    get() = CapabilitiesJson.decodeFromString<Set<PluginCapability>>(capabilitiesJson)

val SourcePluginEntity.displayName: String
    get() = customName?.trim()?.takeIf { it.isNotEmpty() } ?: name

fun SourcePluginEntity.isEnabledFor(sourceType: PluginSourceType): Boolean = when (sourceType) {
    PluginSourceType.METADATA -> metadataEnabled
    PluginSourceType.LYRICS -> lyricsEnabled
    PluginSourceType.COVER -> coverEnabled
}

val SourcePluginEntity.isEnabledAnywhere: Boolean
    get() = metadataEnabled || lyricsEnabled || coverEnabled

fun SourcePluginEntity.sortOrderFor(sourceType: PluginSourceType): Int = when (sourceType) {
    PluginSourceType.METADATA -> metadataSortOrder
    PluginSourceType.LYRICS -> lyricsSortOrder
    PluginSourceType.COVER -> coverSortOrder
}

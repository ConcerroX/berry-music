package com.berryos.music.core.plugin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Capability declared by a plugin manifest. A plugin can declare any combination of the three.
 */
@Serializable
enum class PluginCapability {
    @SerialName("searchSongs")
    SEARCH_SONGS,

    @SerialName("getLyrics")
    GET_LYRICS,

    @SerialName("searchCovers")
    SEARCH_COVERS,
}

/**
 * Host-side source category derived from a [PluginCapability]. This is how the app groups enabled
 * plugins (metadata / lyrics / cover), not a value the plugin declares itself.
 */
enum class PluginSourceType {
    METADATA,
    LYRICS,
    COVER,
}

fun Set<PluginCapability>.normalizedPluginCapabilities(): Set<PluginCapability> =
    ifEmpty { setOf(PluginCapability.SEARCH_SONGS) }

fun Set<PluginCapability>.supportsSourceType(sourceType: PluginSourceType): Boolean {
    val normalized = normalizedPluginCapabilities()
    return when (sourceType) {
        PluginSourceType.METADATA -> PluginCapability.SEARCH_SONGS in normalized
        PluginSourceType.LYRICS -> PluginCapability.GET_LYRICS in normalized
        PluginSourceType.COVER -> PluginCapability.SEARCH_COVERS in normalized
    }
}

package com.berryos.music.core.plugin.model

import kotlinx.serialization.Serializable

// Ported from Lyrico (Apache License 2.0): com.lonx.lyrico.data.model.lyrics.SongSearchResult
@Serializable
data class SongSearchResult(
    val id: String,
    val pluginId: String,
    val pluginName: String,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0L,
    val date: String = "",
    val trackNumber: String = "",
    val picUrl: String = "",
    val fields: Map<String, String> = emptyMap(),
    val internal: Map<String, String> = emptyMap(),
) {
    fun normalizedFields(): Map<String, String> {
        return buildMap {
            putAll(fields.sanitizeStandardFields())

            if (title.isNotBlank()) putIfAbsent("title", title)
            if (artist.isNotBlank()) putIfAbsent("artist", artist)
            if (album.isNotBlank()) putIfAbsent("album", album)
            if (date.isNotBlank()) putIfAbsent("date", date)
            if (trackNumber.isNotBlank()) putIfAbsent("track_number", trackNumber)
            if (picUrl.isNotBlank()) putIfAbsent("cover_url", picUrl)
        }
    }
}

fun Map<String, String>.sanitizeStandardFields(): Map<String, String> {
    return asSequence()
        .filter { (key, value) ->
            StandardPluginField.fromKey(key) != null && value.isNotBlank()
        }
        .associate { (key, value) -> key to value }
}

fun Map<String, String>.sanitizePluginInternal(): Map<String, String> {
    return asSequence()
        .filter { (key, value) ->
            key.isNotBlank() && key.length <= 64 && value.length <= 4096
        }
        .take(64)
        .associate { (key, value) -> key to value }
}

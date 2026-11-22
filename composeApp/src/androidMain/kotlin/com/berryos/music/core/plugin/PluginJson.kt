package com.berryos.music.core.plugin

import kotlinx.serialization.json.Json

internal val PluginJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

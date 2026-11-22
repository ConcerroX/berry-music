package com.berryos.music.core.plugin

import kotlinx.serialization.json.Json

internal val pluginJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

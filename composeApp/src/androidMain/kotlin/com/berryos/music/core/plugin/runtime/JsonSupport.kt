package com.berryos.music.core.plugin.runtime

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonObject.string(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

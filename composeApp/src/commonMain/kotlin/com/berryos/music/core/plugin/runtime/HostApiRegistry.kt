package com.berryos.music.core.plugin.runtime

object HostApiRegistry {
    const val MIN_PLUGIN_PROTOCOL_VERSION = 1
    const val PLUGIN_PROTOCOL_VERSION = 5
    const val MIN_PLATFORM_API_VERSION = 1
    const val PLATFORM_API_VERSION = 4

    fun supportsPluginApiVersion(apiVersion: Int): Boolean =
        apiVersion in MIN_PLUGIN_PROTOCOL_VERSION..PLUGIN_PROTOCOL_VERSION

    fun supportsHostApiVersion(minHostApiVersion: Int): Boolean =
        minHostApiVersion in MIN_PLATFORM_API_VERSION..PLATFORM_API_VERSION
}

package com.berryos.music.core.plugin.source

import android.util.Log
import com.berryos.music.core.database.entity.SourcePluginEntity
import com.berryos.music.core.plugin.i18n.PluginNameResolver
import com.berryos.music.core.plugin.locale.PluginLocalePreferences
import com.berryos.music.core.plugin.model.PluginSourceType
import com.berryos.music.core.plugin.model.SourceRuntimeConfig
import com.berryos.music.core.plugin.model.supportsSourceType
import com.berryos.music.core.plugin.registry.PluginRegistry
import com.berryos.music.core.plugin.registry.capabilities
import com.berryos.music.core.plugin.registry.displayName
import com.berryos.music.core.plugin.registry.isEnabledAnywhere
import com.berryos.music.core.plugin.registry.isEnabledFor
import com.berryos.music.core.plugin.registry.sortOrderFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SearchSourceWithState(val source: SearchSource, val enabled: Boolean)

class PluginSourceProvider(private val registry: PluginRegistry, private val factory: ScriptSearchSourceFactory) {
    private val cache = mutableMapOf<String, ScriptSearchSource>()
    private val cachedPlugins = mutableMapOf<String, SourcePluginEntity>()
    private val mutex = Mutex()

    fun observeSources(sourceType: PluginSourceType? = null): Flow<List<SearchSource>> = registry.observePlugins()
        .combine(PluginLocalePreferences.preferences) { plugins, _ ->
            buildSources(
                plugins = plugins.sourcesFor(sourceType),
                retainedIds = plugins.enabledPluginIds(),
            )
        }

    suspend fun getSources(sourceType: PluginSourceType? = null): List<SearchSource> {
        val plugins = registry.getPlugins()
        return buildSources(
            plugins = plugins.sourcesFor(sourceType),
            retainedIds = plugins.enabledPluginIds(),
        )
    }

    suspend fun getSourceWithState(pluginId: String): SearchSourceWithState? =
        registry.getPlugin(pluginId)?.let { plugin ->
            buildSources(listOf(plugin), retainedIds = null).firstOrNull()?.let { source ->
                SearchSourceWithState(source = source, enabled = plugin.isEnabledAnywhere)
            }
        }

    suspend fun invalidate(pluginId: String) {
        mutex.withLock {
            cache.remove(pluginId)?.close()
            cachedPlugins.remove(pluginId)
        }
    }

    private suspend fun buildSources(
        plugins: List<SourcePluginEntity>,
        retainedIds: Set<String>?,
    ): List<SearchSource> = mutex.withLock {
        buildSourcesLocked(plugins, retainedIds)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun buildSourcesLocked(
        plugins: List<SourcePluginEntity>,
        retainedIds: Set<String>?,
    ): List<SearchSource> {
        if (retainedIds != null) {
            (cache.keys - retainedIds).forEach { id ->
                cache.remove(id)?.close()
                cachedPlugins.remove(id)
            }
        }

        return plugins.mapNotNull { plugin ->
            try {
                val config = SourceRuntimeConfig(registry.getConfig(plugin.id))
                val existing = cache[plugin.id]
                if (existing != null && cachedPlugins[plugin.id]?.pluginDir == plugin.pluginDir) {
                    existing.applyConfig(config)
                    cachedPlugins[plugin.id] = plugin
                    existing.snapshot(plugin)
                } else {
                    existing?.close()
                    factory.create(plugin).also {
                        it.applyConfig(config)
                        cache[plugin.id] = it
                        cachedPlugins[plugin.id] = plugin
                    }.snapshot(plugin)
                }
            } catch (throwable: Exception) {
                Log.w(TAG, "Failed to build plugin source: ${PluginNameResolver.resolve(plugin)}", throwable)
                null
            }
        }
    }

    suspend fun close() {
        mutex.withLock {
            cache.values.forEach { it.close() }
            cache.clear()
            cachedPlugins.clear()
        }
    }

    private companion object {
        const val TAG = "PluginSourceProvider"
    }
}

private fun List<SourcePluginEntity>.sourcesFor(sourceType: PluginSourceType?): List<SourcePluginEntity> =
    if (sourceType == null) enabledPlugins() else forSourceType(sourceType)

private fun List<SourcePluginEntity>.enabledPlugins(): List<SourcePluginEntity> =
    filter(SourcePluginEntity::isEnabledAnywhere)

private fun List<SourcePluginEntity>.enabledPluginIds(): Set<String> = asSequence()
    .filter(SourcePluginEntity::isEnabledAnywhere)
    .mapTo(mutableSetOf()) { it.id }

private fun List<SourcePluginEntity>.forSourceType(sourceType: PluginSourceType): List<SourcePluginEntity> =
    asSequence()
        .filter { plugin ->
            plugin.isEnabledFor(sourceType) && plugin.capabilities.supportsSourceType(sourceType)
        }
        .sortedWith(
            compareBy<SourcePluginEntity> { it.sortOrderFor(sourceType) }
                .thenBy { it.displayName },
        )
        .toList()

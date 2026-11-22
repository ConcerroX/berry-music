package com.berryos.music.core.plugin.registry

import com.berryos.music.core.database.entity.SourcePluginEntity
import kotlinx.coroutines.flow.Flow

interface PluginRegistry {
    fun observePlugins(): Flow<List<SourcePluginEntity>>
    suspend fun getPlugins(): List<SourcePluginEntity>
    suspend fun getPlugin(id: String): SourcePluginEntity?
    suspend fun upsert(plugin: SourcePluginEntity)
    suspend fun delete(id: String)
    suspend fun getConfig(pluginId: String): Map<String, String>
    suspend fun saveConfig(pluginId: String, values: Map<String, String>)
}

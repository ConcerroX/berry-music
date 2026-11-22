package com.berryos.music.core.plugin.registry

import com.berryos.music.core.database.BerryDatabase
import com.berryos.music.core.database.entity.PluginConfigEntity
import com.berryos.music.core.database.entity.SourcePluginEntity

class RoomPluginRegistry(database: BerryDatabase) : PluginRegistry {
    private val pluginDao = database.sourcePluginDao()
    private val configDao = database.pluginConfigDao()

    override fun observePlugins() = pluginDao.observeAll()

    override suspend fun getPlugins(): List<SourcePluginEntity> = pluginDao.getAll()

    override suspend fun getPlugin(id: String): SourcePluginEntity? = pluginDao.getById(id)

    override suspend fun upsert(plugin: SourcePluginEntity) = pluginDao.upsert(plugin)

    override suspend fun delete(id: String) = pluginDao.deleteById(id)

    override suspend fun getConfig(pluginId: String): Map<String, String> =
        configDao.getForPlugin(pluginId).associate { it.key to it.value }

    override suspend fun saveConfig(pluginId: String, values: Map<String, String>) {
        configDao.replaceForPlugin(
            pluginId = pluginId,
            values = values.map { (key, value) -> PluginConfigEntity(pluginId, key, value) },
        )
    }
}

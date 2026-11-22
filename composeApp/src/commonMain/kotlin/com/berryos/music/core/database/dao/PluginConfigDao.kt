package com.berryos.music.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.berryos.music.core.database.entity.PluginConfigEntity

@Dao
interface PluginConfigDao {

    @Query("SELECT * FROM plugin_config WHERE pluginId = :pluginId")
    suspend fun getForPlugin(pluginId: String): List<PluginConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(values: List<PluginConfigEntity>)

    @Query("DELETE FROM plugin_config WHERE pluginId = :pluginId")
    suspend fun deleteForPlugin(pluginId: String)

    @Transaction
    suspend fun replaceForPlugin(pluginId: String, values: List<PluginConfigEntity>) {
        deleteForPlugin(pluginId)
        upsertAll(values)
    }
}

package com.berryos.music.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.berryos.music.core.database.entity.SourcePluginEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourcePluginDao {

    @Query("SELECT * FROM source_plugins ORDER BY name")
    fun observeAll(): Flow<List<SourcePluginEntity>>

    @Query("SELECT * FROM source_plugins")
    suspend fun getAll(): List<SourcePluginEntity>

    @Query("SELECT * FROM source_plugins WHERE id = :id")
    suspend fun getById(id: String): SourcePluginEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plugin: SourcePluginEntity)

    @Query("DELETE FROM source_plugins WHERE id = :id")
    suspend fun deleteById(id: String)
}

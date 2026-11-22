package com.berryos.music.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.berryos.music.core.database.dao.LocalLibraryDao
import com.berryos.music.core.database.dao.PluginConfigDao
import com.berryos.music.core.database.dao.SourcePluginDao
import com.berryos.music.core.database.entity.LocalPersonEntity
import com.berryos.music.core.database.entity.LocalTrackCreditEntity
import com.berryos.music.core.database.entity.LocalTrackEntity
import com.berryos.music.core.database.entity.PluginConfigEntity
import com.berryos.music.core.database.entity.SourcePluginEntity

@Database(
    entities = [
        SourcePluginEntity::class,
        PluginConfigEntity::class,
        LocalTrackEntity::class,
        LocalPersonEntity::class,
        LocalTrackCreditEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@ConstructedBy(BerryDatabaseConstructor::class)
abstract class BerryDatabase : RoomDatabase() {
    abstract fun sourcePluginDao(): SourcePluginDao
    abstract fun pluginConfigDao(): PluginConfigDao
    abstract fun localLibraryDao(): LocalLibraryDao
}

@Suppress("KotlinNoActualForExpect")
expect object BerryDatabaseConstructor : RoomDatabaseConstructor<BerryDatabase> {
    override fun initialize(): BerryDatabase
}

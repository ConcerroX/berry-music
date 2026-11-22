package com.berryos.music.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "plugin_config",
    primaryKeys = ["pluginId", "key"],
    foreignKeys = [
        ForeignKey(
            entity = SourcePluginEntity::class,
            parentColumns = ["id"],
            childColumns = ["pluginId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["pluginId"])],
)
data class PluginConfigEntity(val pluginId: String, val key: String, val value: String)

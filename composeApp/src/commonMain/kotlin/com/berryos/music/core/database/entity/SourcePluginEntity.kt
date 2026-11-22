package com.berryos.music.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "source_plugins",
    indices = [Index(value = ["pluginDir"])],
)
data class SourcePluginEntity(
    @PrimaryKey val id: String,
    val name: String,
    val versionCode: Int,
    val versionName: String,
    val apiVersion: Int,
    val minHostApiVersion: Int,
    val author: String,
    val description: String,
    val pluginDir: String,
    val entryFile: String,
    val capabilitiesJson: String,
    val iconPath: String?,
    val customName: String?,
    val metadataEnabled: Boolean,
    val lyricsEnabled: Boolean,
    val coverEnabled: Boolean,
    val metadataSortOrder: Int,
    val lyricsSortOrder: Int,
    val coverSortOrder: Int,
    val installedAt: Long,
    val updatedAt: Long,
)

package com.berryos.music.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Tag-derived metadata for one local track, keyed by its MediaStore file id. */
@Entity(
    tableName = "local_tracks",
    indices = [Index("albumKey")],
)
data class LocalTrackEntity(
    @PrimaryKey val mediaId: Long,
    val filename: String,
    val title: String,
    val artistName: String,
    val album: String,
    val albumKey: String,
    val trackNumber: Int,
    val year: Int,
    val durationMs: Long,
    val dateModified: Long,
    /** 0 means the tags have not been read; a value older than [dateModified] is stale. */
    val tagScannedAt: Long,
)

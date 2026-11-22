package com.berryos.music.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.berryos.music.core.model.CreditRole

/** One role a person has on a track; [position] preserves the tag order within the role. */
@Entity(
    tableName = "local_track_credits",
    primaryKeys = ["mediaId", "personKey", "role"],
    foreignKeys = [
        ForeignKey(
            entity = LocalTrackEntity::class,
            parentColumns = ["mediaId"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LocalPersonEntity::class,
            parentColumns = ["personKey"],
            childColumns = ["personKey"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mediaId"), Index("personKey"), Index("role")],
)
data class LocalTrackCreditEntity(val mediaId: Long, val personKey: String, val role: CreditRole, val position: Int)

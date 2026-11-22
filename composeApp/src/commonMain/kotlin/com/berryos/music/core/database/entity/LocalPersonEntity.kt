package com.berryos.music.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One person referenced by track credits; the key is the trimmed display name. */
@Entity(tableName = "local_people")
data class LocalPersonEntity(@PrimaryKey val personKey: String, val name: String)

package com.berryos.music.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.berryos.music.core.database.entity.LocalPersonEntity
import com.berryos.music.core.database.entity.LocalTrackCreditEntity
import com.berryos.music.core.database.entity.LocalTrackEntity

@Dao
interface LocalLibraryDao {

    @Query("SELECT * FROM local_tracks ORDER BY mediaId")
    suspend fun tracks(): List<LocalTrackEntity>

    @Query("SELECT * FROM local_track_credits ORDER BY mediaId, role, position")
    suspend fun credits(): List<LocalTrackCreditEntity>

    @Query("SELECT * FROM local_people")
    suspend fun people(): List<LocalPersonEntity>

    @Upsert
    suspend fun upsertTracks(tracks: List<LocalTrackEntity>)

    @Upsert
    suspend fun upsertPeople(people: List<LocalPersonEntity>)

    @Query("DELETE FROM local_tracks WHERE mediaId IN (:mediaIds)")
    suspend fun deleteTracks(mediaIds: List<Long>)

    @Query("DELETE FROM local_tracks")
    suspend fun deleteAllTracks()

    @Query("DELETE FROM local_track_credits WHERE mediaId = :mediaId")
    suspend fun deleteCredits(mediaId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredits(credits: List<LocalTrackCreditEntity>)

    @Transaction
    suspend fun replaceCredits(mediaId: Long, credits: List<LocalTrackCreditEntity>) {
        deleteCredits(mediaId)
        if (credits.isNotEmpty()) insertCredits(credits)
    }

    @Query("UPDATE local_tracks SET tagScannedAt = :scannedAt WHERE mediaId = :mediaId")
    suspend fun updateScanState(mediaId: Long, scannedAt: Long)

    @Query(
        "SELECT * FROM local_tracks " +
            "WHERE tagScannedAt = 0 OR dateModified > tagScannedAt " +
            "ORDER BY mediaId LIMIT :limit",
    )
    suspend fun staleTracks(limit: Int): List<LocalTrackEntity>
}

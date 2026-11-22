package com.berryos.music.core.library

import com.berryos.music.core.database.entity.LocalTrackEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalLibraryPlanTest {
    @Test
    fun createsPlaceholderFromFileFactsOnly() {
        val plan = planLocalSync(
            existing = emptyList(),
            media = listOf(MediaTrackRow(mediaId = 1, filename = "Song.mp3", dateModified = 10)),
            unknownTitle = "Unknown title",
            unknownArtist = "Unknown artist",
            unknownAlbum = "Unknown album",
        )

        val track = plan.changedTracks.single()
        assertEquals("Song", track.title)
        assertEquals("Unknown artist", track.artistName)
        assertEquals("Unknown album", track.album)
        assertEquals(UNKNOWN_ALBUM_KEY, track.albumKey)
        assertEquals(0L, track.tagScannedAt)
        assertTrue(plan.hasChanges)
    }

    @Test
    fun leavesUnchangedTracksUntouched() {
        val existing = entity(mediaId = 1, dateModified = 10, scannedAt = 20)
        val plan = planLocalSync(
            existing = listOf(existing),
            media = listOf(MediaTrackRow(mediaId = 1, filename = "Song.mp3", dateModified = 10)),
            unknownTitle = "Unknown title",
            unknownArtist = "Unknown artist",
            unknownAlbum = "Unknown album",
        )

        assertTrue(plan.changedTracks.isEmpty())
        assertFalse(plan.hasChanges)
    }

    @Test
    fun marksModifiedTrackForRescanAndKeepsMetadata() {
        val existing = entity(mediaId = 1, dateModified = 10, scannedAt = 20)
        val plan = planLocalSync(
            existing = listOf(existing),
            media = listOf(MediaTrackRow(mediaId = 1, filename = "Renamed.mp3", dateModified = 30)),
            unknownTitle = "Unknown title",
            unknownArtist = "Unknown artist",
            unknownAlbum = "Unknown album",
        )

        val track = plan.changedTracks.single()
        assertEquals(0L, track.tagScannedAt)
        assertEquals("Renamed.mp3", track.filename)
        assertEquals(existing.title, track.title)
        assertEquals(existing.albumKey, track.albumKey)
    }

    @Test
    fun reportsDeletedTracks() {
        val plan = planLocalSync(
            existing = listOf(entity(mediaId = 1, dateModified = 10, scannedAt = 20)),
            media = emptyList(),
            unknownTitle = "Unknown title",
            unknownArtist = "Unknown artist",
            unknownAlbum = "Unknown album",
        )

        assertTrue(plan.clearAll)
        assertEquals(listOf(1L), plan.deletedIds)
    }

    private fun entity(mediaId: Long, dateModified: Long, scannedAt: Long): LocalTrackEntity = LocalTrackEntity(
        mediaId = mediaId,
        filename = "Song.mp3",
        title = "Song",
        artistName = "Artist",
        album = "Album",
        albumKey = "album-key",
        trackNumber = 1,
        year = 2020,
        durationMs = 60_000,
        dateModified = dateModified,
        tagScannedAt = scannedAt,
    )
}

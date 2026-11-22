package com.berryos.music.core.library

import com.berryos.music.core.database.entity.LocalPersonEntity
import com.berryos.music.core.database.entity.LocalTrackCreditEntity
import com.berryos.music.core.database.entity.LocalTrackEntity
import com.berryos.music.core.model.CreditRole
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalLibrarySnapshotTest {
    @Test
    fun groupsByEveryArtistCredit() {
        val snapshot = buildLocalSnapshot(
            tracks = listOf(
                track(mediaId = 1, albumKey = "album-1"),
                track(mediaId = 2, albumKey = "album-1"),
            ),
            credits = listOf(
                credit(1, "Alice", CreditRole.ARTIST, 0),
                credit(1, "Bob", CreditRole.ARTIST, 1),
                credit(1, "Alice", CreditRole.ALBUM_ARTIST, 0),
                credit(1, "Carol", CreditRole.COMPOSER, 0),
                credit(2, "Alice", CreditRole.ARTIST, 0),
            ),
            people = listOf(
                LocalPersonEntity("Alice", "Alice"),
                LocalPersonEntity("Bob", "Bob"),
                LocalPersonEntity("Carol", "Carol"),
            ),
            unknownTitle = "Unknown title",
            unknownArtist = "Unknown artist",
            unknownAlbum = "Unknown album",
        )

        val first = snapshot.tracks.first()
        assertEquals(listOf("Alice", "Bob"), first.artists.map { it.name })
        assertEquals("Alice", first.artist)
        assertEquals("Alice", first.artistId)
        assertEquals(2, snapshot.artists.first { it.id == "Alice" }.trackCount)
        assertEquals(1, snapshot.artists.first { it.id == "Alice" }.albumCount)
        assertEquals(1, snapshot.artists.first { it.id == "Bob" }.trackCount)
        assertEquals(2, snapshot.artists.size)
        assertEquals("Alice", snapshot.albums.single().artist)
        assertEquals("album-1", snapshot.albums.single().id)
        assertEquals("local-cover://1?v=10", snapshot.albums.single().coverUrl)
        assertEquals("local-cover://1?v=10", first.coverUrl)
        assertEquals("local-cover://2?v=10", snapshot.tracks.last().coverUrl)
        assertEquals("local-cover://1?v=10", snapshot.artists.first { it.id == "Alice" }.coverUrl)
    }

    @Test
    fun fallsBackToTrackArtistForAlbum() {
        val snapshot = buildLocalSnapshot(
            tracks = listOf(track(mediaId = 1, albumKey = "album-1")),
            credits = listOf(credit(1, "Alice", CreditRole.ARTIST, 0)),
            people = emptyList(),
            unknownTitle = "Unknown title",
            unknownArtist = "Unknown artist",
            unknownAlbum = "Unknown album",
        )

        assertEquals("Alice", snapshot.albums.single().artist)
        assertEquals("Alice", snapshot.artists.single().name)
    }

    private fun track(mediaId: Long, albumKey: String): LocalTrackEntity = LocalTrackEntity(
        mediaId = mediaId,
        filename = "Song$mediaId.flac",
        title = "Song $mediaId",
        artistName = "Alice",
        album = "Album",
        albumKey = albumKey,
        trackNumber = mediaId.toInt(),
        year = 2020,
        durationMs = 60_000,
        dateModified = 10,
        tagScannedAt = 20,
    )

    private fun credit(mediaId: Long, personKey: String, role: CreditRole, position: Int): LocalTrackCreditEntity =
        LocalTrackCreditEntity(mediaId, personKey, role, position)
}

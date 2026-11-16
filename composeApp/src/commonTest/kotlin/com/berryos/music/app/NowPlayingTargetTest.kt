package com.berryos.music.app

import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.model.Album
import com.berryos.music.core.model.Artist
import com.berryos.music.core.model.LibrarySnapshot
import com.berryos.music.core.model.SourceKey
import com.berryos.music.core.model.Track
import com.berryos.music.core.playback.PlaybackSource
import com.berryos.music.core.playback.PlaybackState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NowPlayingTargetTest {
    @Test
    fun queueSourceResolvesWithoutALibrarySnapshot() {
        assertQueueTarget(LibraryUiState.Loading)
        assertQueueTarget(ready)
    }

    @Test
    fun noCurrentTrackResolvesToNull() {
        assertNull(resolveNowPlayingTarget(ready, PlaybackState()))
    }

    @Test
    fun albumSourceUsesTheAlbumFromTheSnapshot() {
        assertEquals(NowPlayingTarget.Album(SourceKey.Local, ALBUM_ID), resolve(PlaybackSource.Album(ALBUM_ID)))
    }

    @Test
    fun albumSourceFallsBackToTheTrackAlbum() {
        assertEquals(NowPlayingTarget.Album(SourceKey.Local, ALBUM_ID), resolve(PlaybackSource.Album("missing")))
    }

    @Test
    fun artistSourceResolvesToTheArtistOrTheTrackAlbum() {
        assertEquals(
            NowPlayingTarget.Artist(SourceKey.Local, ARTIST_ID),
            resolve(PlaybackSource.Artist(ARTIST_ID)),
        )
        assertEquals(
            NowPlayingTarget.Album(SourceKey.Local, ALBUM_ID),
            resolve(PlaybackSource.Artist("missing")),
        )
    }

    @Test
    fun playlistSourceResolvesToThePlaylist() {
        assertEquals(NowPlayingTarget.Playlist("playlist"), resolve(PlaybackSource.Playlist("playlist")))
    }

    @Test
    fun songsSourceResolvesToTheTrackAlbum() {
        assertEquals(NowPlayingTarget.Album(SourceKey.Local, ALBUM_ID), resolve(PlaybackSource.Songs))
    }

    @Test
    fun collectionSourcesNeedASnapshot() {
        assertNull(resolveNowPlayingTarget(LibraryUiState.Loading, state(PlaybackSource.Album(ALBUM_ID))))
    }

    private fun assertQueueTarget(library: LibraryUiState) {
        assertEquals(NowPlayingTarget.Queue, resolveNowPlayingTarget(library, state(PlaybackSource.Queue)))
    }

    private fun resolve(source: PlaybackSource) = resolveNowPlayingTarget(ready, state(source))

    private fun state(source: PlaybackSource) = PlaybackState(queue = listOf(track), currentIndex = 0, source = source)

    private val track = Track(
        source = SourceKey.Local,
        id = "track",
        title = "Track",
        artist = "Artist",
        artistId = ARTIST_ID,
        album = "Album",
        albumId = ALBUM_ID,
        durationMs = 1_000L,
        trackNumber = 1,
        year = 2020,
    )

    private val ready = LibraryUiState.Ready(
        LibrarySnapshot(
            tracks = listOf(track),
            albums = listOf(Album(SourceKey.Local, ALBUM_ID, "Album", "Artist", 1, 2020)),
            artists = listOf(Artist(SourceKey.Local, ARTIST_ID, "Artist", 1, 1)),
        ),
    )

    private companion object {
        const val ALBUM_ID = "album"
        const val ARTIST_ID = "artist"
    }
}

package com.berryos.music.core.playback

import com.berryos.music.core.model.SourceKey
import com.berryos.music.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackStateTest {
    @Test
    fun playbackStatusIsNotPlayingWhenTheSourceDiffers() {
        val state = PlaybackState(source = PlaybackSource.Queue, isPlayRequested = true)
        assertEquals(PlaybackStatus.NotPlaying, state.playbackStatus(PlaybackSource.Album("album")))
    }

    @Test
    fun playbackStatusFollowsThePlayRequest() {
        val playing = PlaybackState(source = PlaybackSource.Album("album"), isPlayRequested = true)
        assertEquals(PlaybackStatus.Playing, playing.playbackStatus(PlaybackSource.Album("album")))
        assertEquals(
            PlaybackStatus.Paused,
            playing.copy(isPlayRequested = false).playbackStatus(PlaybackSource.Album("album")),
        )
    }

    @Test
    fun currentTrackFollowsTheIndex() {
        val tracks = listOf(track("1"), track("2"))
        assertEquals(tracks[1], PlaybackState(queue = tracks, currentIndex = 1).currentTrack)
        assertNull(PlaybackState(queue = tracks, currentIndex = tracks.size).currentTrack)
        assertNull(PlaybackState().currentTrack)
    }

    private fun track(id: String) = Track(
        source = SourceKey.Local,
        id = id,
        title = "Track $id",
        artist = "Artist",
        artistId = "artist",
        album = "Album",
        albumId = "album",
        durationMs = 1_000L,
        trackNumber = id.toInt(),
        year = 2020,
    )
}

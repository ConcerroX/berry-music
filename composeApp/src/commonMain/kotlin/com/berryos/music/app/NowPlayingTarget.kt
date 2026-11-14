package com.berryos.music.app

import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.model.LibrarySnapshot
import com.berryos.music.core.model.Track
import com.berryos.music.core.playback.PlaybackSource
import com.berryos.music.core.playback.PlaybackState

/** Page shown by the now playing sheet: the detail page of whatever collection playback was started from. */
internal sealed interface NowPlayingTarget {
    data class Album(val albumId: String) : NowPlayingTarget
    data class Artist(val artistId: String) : NowPlayingTarget
    data class Playlist(val playlistId: String) : NowPlayingTarget
}

internal fun resolveNowPlayingTarget(state: LibraryUiState, playbackState: PlaybackState): NowPlayingTarget? {
    val snapshot = (state as? LibraryUiState.Ready)?.snapshot
    val track = playbackState.currentTrack
    return if (snapshot == null || track == null) {
        null
    } else {
        resolveTarget(snapshot, track, playbackState.source)
    }
}

private fun resolveTarget(snapshot: LibrarySnapshot, track: Track, source: PlaybackSource): NowPlayingTarget? =
    when (source) {
        is PlaybackSource.Album ->
            albumTarget(snapshot, track, source.albumId) ?: albumTarget(snapshot, track, track.albumId)

        is PlaybackSource.Artist ->
            snapshot.artists
                .firstOrNull { it.source == track.source && it.id == source.artistId }
                ?.let { NowPlayingTarget.Artist(it.id) }
                ?: albumTarget(snapshot, track, track.albumId)

        is PlaybackSource.Playlist ->
            NowPlayingTarget.Playlist(source.playlistId)

        PlaybackSource.Queue, PlaybackSource.Songs, PlaybackSource.Search ->
            albumTarget(snapshot, track, track.albumId)
    }

private fun albumTarget(snapshot: LibrarySnapshot, track: Track, albumId: String): NowPlayingTarget? = snapshot.albums
    .firstOrNull { it.source == track.source && it.id == albumId }
    ?.let { NowPlayingTarget.Album(it.id) }

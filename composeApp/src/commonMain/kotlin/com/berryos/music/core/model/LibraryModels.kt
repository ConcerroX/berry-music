package com.berryos.music.core.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface SourceKey {
    @Serializable
    data object Local : SourceKey

    @Serializable
    data class Subsonic(val serverId: String) : SourceKey
}

data class Track(
    val source: SourceKey,
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val album: String,
    val albumId: String,
    val durationMs: Long,
    val trackNumber: Int,
    val year: Int,
    val streamUrl: String? = null,
)

data class Album(
    val source: SourceKey,
    val id: String,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val year: Int,
    val coverUrl: String? = null,
)

data class Artist(
    val source: SourceKey,
    val id: String,
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val coverUrl: String? = null,
)

data class LibrarySnapshot(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
)

fun LibrarySnapshot.album(source: SourceKey, albumId: String): Album? =
    albums.firstOrNull { it.source == source && it.id == albumId }

fun LibrarySnapshot.artist(source: SourceKey, artistId: String): Artist? =
    artists.firstOrNull { it.source == source && it.id == artistId }

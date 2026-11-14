package com.berryos.music.core.library

import com.berryos.music.core.auth.AuthConfig
import com.berryos.music.core.auth.ServerAccount
import com.berryos.music.core.model.Album
import com.berryos.music.core.model.Artist
import com.berryos.music.core.model.LibrarySnapshot
import com.berryos.music.core.model.SourceKey
import com.berryos.music.core.model.Track
import com.berryos.music.core.network.SubsonicAlbum
import com.berryos.music.core.network.SubsonicClient
import com.berryos.music.core.network.SubsonicSong
import com.berryos.music.core.network.createJsonHttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class SubsonicLibrarySource(
    private val account: () -> ServerAccount?,
    private val client: SubsonicClient = SubsonicClient(createJsonHttpClient(), AuthConfig.SERVER_BASE_URL),
) : LibrarySource {
    private val serverKey = SourceKey.Subsonic(AuthConfig.SERVER_BASE_URL)

    override suspend fun load(): LibrarySnapshot = coroutineScope {
        val account = account() ?: return@coroutineScope LibrarySnapshot()
        val jwt = account.subsonicJwt ?: return@coroutineScope LibrarySnapshot()
        val albums = loadAlbums(account.username, jwt)
        if (albums.isEmpty()) return@coroutineScope LibrarySnapshot()
        val songs = loadSongs(account.username, jwt, albums)
        buildSnapshot(account.username, jwt, albums, songs)
    }

    private suspend fun loadAlbums(username: String, jwt: String): List<SubsonicAlbum> {
        val albums = mutableListOf<SubsonicAlbum>()
        var offset = 0
        while (true) {
            val page = runCatching { client.albumPage(username, jwt, offset, PAGE_SIZE) }
                .getOrDefault(emptyList())
            albums += page
            if (page.size < PAGE_SIZE) break
            offset += page.size
        }
        return albums
    }

    private suspend fun loadSongs(
        username: String,
        jwt: String,
        albums: List<SubsonicAlbum>,
    ): List<List<SubsonicSong>> = coroutineScope {
        val semaphore = Semaphore(MAX_PARALLEL_REQUESTS)
        albums.map { album ->
            async {
                semaphore.withPermit {
                    runCatching { client.albumSongs(username, jwt, album.id) }.getOrDefault(emptyList())
                }
            }
        }.awaitAll()
    }

    private fun buildSnapshot(
        username: String,
        jwt: String,
        albums: List<SubsonicAlbum>,
        songs: List<List<SubsonicSong>>,
    ): LibrarySnapshot {
        val tracks = albums.flatMapIndexed { index, _ -> songs[index].map { it.toTrack(username, jwt) } }
        val albumModels = albums.map { album ->
            Album(
                source = serverKey,
                id = album.id,
                title = album.name,
                artist = album.artist,
                trackCount = album.songCount,
                year = album.year,
                coverUrl = client.coverUrl(username, jwt, album.id),
            )
        }
        val artists = tracks.groupBy { it.artistId }.map { (artistId, artistTracks) ->
            Artist(
                source = serverKey,
                id = artistId,
                name = artistTracks.first().artist,
                albumCount = artistTracks.map { it.albumId }.distinct().size,
                trackCount = artistTracks.size,
            )
        }
        return LibrarySnapshot(tracks = tracks, albums = albumModels, artists = artists)
    }

    private fun SubsonicSong.toTrack(username: String, jwt: String): Track = Track(
        source = serverKey,
        id = id,
        title = title,
        artist = artist,
        artistId = artistId.ifBlank { artist },
        album = album,
        albumId = albumId,
        durationMs = duration.toLong() * MILLIS_PER_SECOND,
        trackNumber = track,
        year = year,
        streamUrl = client.streamUrl(username, jwt, id),
    )

    private companion object {
        const val PAGE_SIZE = 500
        const val MAX_PARALLEL_REQUESTS = 4
        const val MILLIS_PER_SECOND = 1000L
    }
}

package com.berryos.music.core.library

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.berryos.music.core.model.Album
import com.berryos.music.core.model.Artist
import com.berryos.music.core.model.LibrarySnapshot
import com.berryos.music.core.model.SourceKey
import com.berryos.music.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalLibrarySource(context: Context) : LibrarySource {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    override suspend fun load(): LibrarySnapshot = withContext(Dispatchers.IO) {
        val tracks = queryTracks()
        LibrarySnapshot(tracks = tracks, albums = albumsFrom(tracks), artists = artistsFrom(tracks))
    }

    private fun queryTracks(): List<Track> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
        )
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(MIN_DURATION_MS.toString())
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        return runCatching {
            resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

                buildList {
                    while (cursor.moveToNext()) {
                        val artist = cursor.getString(artistColumn) ?: UNKNOWN_ARTIST
                        val artistId = cursor.getLong(artistIdColumn)
                        add(
                            Track(
                                source = SourceKey.Local,
                                id = cursor.getLong(idColumn).toString(),
                                title = cursor.getString(titleColumn) ?: UNKNOWN_TITLE,
                                artist = artist,
                                artistId = if (artistId == 0L) artist else artistId.toString(),
                                album = cursor.getString(albumColumn) ?: UNKNOWN_ALBUM,
                                albumId = cursor.getLong(albumIdColumn).toString(),
                                durationMs = cursor.getLong(durationColumn),
                                trackNumber = cursor.getInt(trackColumn) % TRACK_MODULO,
                                year = cursor.getInt(yearColumn),
                            ),
                        )
                    }
                }
            } ?: emptyList()
        }.getOrElse { emptyList() }
    }

    private fun albumsFrom(tracks: List<Track>): List<Album> = tracks
        .groupBy { it.albumId }
        .map { (albumId, albumTracks) ->
            val first = albumTracks.first()
            Album(
                source = SourceKey.Local,
                id = albumId,
                title = first.album,
                artist = first.artist,
                trackCount = albumTracks.size,
                year = first.year,
            )
        }

    private fun artistsFrom(tracks: List<Track>): List<Artist> = tracks
        .groupBy { it.artistId }
        .map { (artistId, artistTracks) ->
            Artist(
                source = SourceKey.Local,
                id = artistId,
                name = artistTracks.first().artist,
                albumCount = artistTracks.map { it.albumId }.distinct().size,
                trackCount = artistTracks.size,
            )
        }

    companion object {
        private const val MIN_DURATION_MS = 10_000L
        private const val TRACK_MODULO = 1000
        private const val UNKNOWN_TITLE = "Unknown title"
        private const val UNKNOWN_ARTIST = "Unknown artist"
        private const val UNKNOWN_ALBUM = "Unknown album"
    }
}

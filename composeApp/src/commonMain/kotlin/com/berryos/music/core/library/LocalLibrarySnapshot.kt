package com.berryos.music.core.library

import com.berryos.music.core.database.entity.LocalPersonEntity
import com.berryos.music.core.database.entity.LocalTrackCreditEntity
import com.berryos.music.core.database.entity.LocalTrackEntity
import com.berryos.music.core.model.Album
import com.berryos.music.core.model.Artist
import com.berryos.music.core.model.ArtistRef
import com.berryos.music.core.model.CreditRole
import com.berryos.music.core.model.LibrarySnapshot
import com.berryos.music.core.model.SourceKey
import com.berryos.music.core.model.Track

internal const val LOCAL_COVER_SCHEME = "local-cover"

internal fun localCoverUrl(mediaId: Long, dateModified: Long): String = "$LOCAL_COVER_SCHEME://$mediaId?v=$dateModified"

internal fun buildLocalSnapshot(
    tracks: List<LocalTrackEntity>,
    credits: List<LocalTrackCreditEntity>,
    people: List<LocalPersonEntity>,
    unknownTitle: String,
    unknownArtist: String,
    unknownAlbum: String,
): LibrarySnapshot {
    val names = people.associate { it.personKey to it.name }
    val creditsByTrack = credits.groupBy { it.mediaId }
    val albumKeysByTrack = tracks.associate { it.mediaId to it.albumKey }
    val entitiesById = tracks.associateBy { it.mediaId }

    val models = tracks.map { track ->
        val artists = creditsByTrack[track.mediaId].orEmpty()
            .filter { it.role == CreditRole.ARTIST }
            .sortedBy { it.position }
            .map { ArtistRef(it.personKey, names[it.personKey] ?: it.personKey) }
        val primary = artists.firstOrNull()
        Track(
            source = SourceKey.Local,
            id = track.mediaId.toString(),
            title = track.title.ifBlank { unknownTitle },
            artist = primary?.name ?: track.artistName.ifBlank { unknownArtist },
            artistId = primary?.id ?: personKey(track.artistName),
            album = track.album.ifBlank { unknownAlbum },
            albumId = track.albumKey,
            durationMs = track.durationMs,
            trackNumber = track.trackNumber,
            year = track.year,
            coverUrl = localCoverUrl(track.mediaId, track.dateModified),
            artists = artists,
        )
    }

    val tracksByAlbum = models.groupBy { it.albumId }
    val albumArtistNames = credits
        .filter { it.role == CreditRole.ALBUM_ARTIST }
        .sortedBy { it.position }
        .mapNotNull { credit -> albumKeysByTrack[credit.mediaId]?.let { it to credit.personKey } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, keys) -> names[keys.first()] ?: keys.first() }

    val albums = tracksByAlbum.map { (albumKey, albumTracks) ->
        val first = albumTracks.first()
        Album(
            source = SourceKey.Local,
            id = albumKey,
            title = first.album,
            artist = albumArtistNames[albumKey] ?: first.artist,
            trackCount = albumTracks.size,
            year = first.year,
            coverUrl = first.coverUrl,
        )
    }

    val artists = credits
        .filter { it.role == CreditRole.ARTIST }
        .groupBy { it.personKey }
        .map { (key, personCredits) ->
            val albumKeys = personCredits.mapNotNull { albumKeysByTrack[it.mediaId] }.distinct()
            val representative = personCredits
                .map { it.mediaId }
                .filter { entitiesById.containsKey(it) }
                .minOrNull()
            Artist(
                source = SourceKey.Local,
                id = key,
                name = names[key] ?: key,
                albumCount = albumKeys.size,
                trackCount = personCredits.map { it.mediaId }.distinct().size,
                coverUrl = representative?.let { id ->
                    entitiesById[id]?.let { localCoverUrl(it.mediaId, it.dateModified) }
                },
            )
        }

    return LibrarySnapshot(tracks = models, albums = albums, artists = artists)
}

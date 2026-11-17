package com.berryos.music.core.metadata

import com.berryos.audiometadata.AudioFile
import com.berryos.audiometadata.Id3Version
import com.berryos.audiometadata.TagContainer
import com.berryos.audiometadata.id3.Id3v2FrameIds
import com.berryos.audiometadata.mp4.Mp4ItemIds
import com.berryos.audiometadata.vorbis.VorbisCommentKeys

private val ENTRY_PRIORITY = listOf(
    TrackTagField.TITLE,
    TrackTagField.ARTIST,
    TrackTagField.ALBUM,
    TrackTagField.ALBUM_ARTIST,
    TrackTagField.COMPOSER,
    TrackTagField.LYRICIST,
    TrackTagField.GENRE,
    TrackTagField.LANGUAGE,
    TrackTagField.DATE,
    TrackTagField.RATING,
    TrackTagField.TRACK_NUMBER,
    TrackTagField.TRACK_TOTAL,
    TrackTagField.DISC_NUMBER,
    TrackTagField.DISC_TOTAL,
    TrackTagField.LYRICS,
    TrackTagField.ARTWORK,
)

internal fun List<TagEntry>.prioritizedByField(): List<TagEntry> {
    val used = BooleanArray(size)
    val ordered = ArrayList<TagEntry>(size)
    ENTRY_PRIORITY.forEach { field ->
        forEachIndexed { index, entry ->
            if (!used[index] && entry.field == field) {
                used[index] = true
                ordered += entry
            }
        }
    }
    forEachIndexed { index, entry ->
        if (!used[index]) ordered += entry
    }
    return ordered
}

internal fun List<TagEntry>.withMissingFields(keys: Map<TrackTagField, String>): List<TagEntry> {
    val present = mapNotNull { it.field }.toSet()
    return this + ENTRY_PRIORITY.filter { it !in present && it in keys }.map { field ->
        TagEntry(key = keys[field], value = "", field = field)
    }
}

internal fun AudioFile.expectedFieldKeys(): Map<TrackTagField, String> = when (tagContainer) {
    TagContainer.ID3V2 -> id3FieldKeys()

    TagContainer.VORBIS_COMMENT -> mapOf(
        TrackTagField.TITLE to VorbisCommentKeys.TITLE,
        TrackTagField.ARTIST to VorbisCommentKeys.ARTIST,
        TrackTagField.ALBUM to VorbisCommentKeys.ALBUM,
        TrackTagField.ALBUM_ARTIST to VorbisCommentKeys.ALBUM_ARTIST,
        TrackTagField.COMPOSER to VorbisCommentKeys.COMPOSER,
        TrackTagField.LYRICIST to VorbisCommentKeys.LYRICIST,
        TrackTagField.GENRE to VorbisCommentKeys.GENRE,
        TrackTagField.LANGUAGE to VorbisCommentKeys.LANGUAGE,
        TrackTagField.DATE to VorbisCommentKeys.DATE,
        TrackTagField.RATING to VorbisCommentKeys.RATING,
        TrackTagField.TRACK_NUMBER to VorbisCommentKeys.TRACK_NUMBER,
        TrackTagField.TRACK_TOTAL to VorbisCommentKeys.TRACK_TOTAL,
        TrackTagField.DISC_NUMBER to VorbisCommentKeys.DISC_NUMBER,
        TrackTagField.DISC_TOTAL to VorbisCommentKeys.DISC_TOTAL,
        TrackTagField.LYRICS to VorbisCommentKeys.LYRICS,
    )

    TagContainer.MP4_ILST -> mapOf(
        TrackTagField.TITLE to Mp4ItemIds.TITLE,
        TrackTagField.ARTIST to Mp4ItemIds.ARTIST,
        TrackTagField.ALBUM to Mp4ItemIds.ALBUM,
        TrackTagField.ALBUM_ARTIST to Mp4ItemIds.ALBUM_ARTIST,
        TrackTagField.COMPOSER to Mp4ItemIds.COMPOSER,
        TrackTagField.LYRICIST to Mp4ItemIds.freeformIdentifier(Mp4ItemIds.LYRICIST),
        TrackTagField.GENRE to Mp4ItemIds.GENRE,
        TrackTagField.LANGUAGE to Mp4ItemIds.freeformIdentifier(Mp4ItemIds.LANGUAGE),
        TrackTagField.DATE to Mp4ItemIds.YEAR,
        TrackTagField.RATING to Mp4ItemIds.RATING,
        TrackTagField.TRACK_NUMBER to Mp4ItemIds.TRACK,
        TrackTagField.TRACK_TOTAL to Mp4ItemIds.TRACK,
        TrackTagField.DISC_NUMBER to Mp4ItemIds.DISC,
        TrackTagField.DISC_TOTAL to Mp4ItemIds.DISC,
        TrackTagField.LYRICS to Mp4ItemIds.LYRICS,
    )

    null -> emptyMap()
}

private fun AudioFile.id3FieldKeys(): Map<TrackTagField, String> {
    val v22 = id3Version == Id3Version.V22
    fun id(v22Id: String, id: String): String = if (v22) v22Id else id
    return mapOf(
        TrackTagField.TITLE to id(Id3v2FrameIds.V22.TITLE, Id3v2FrameIds.TITLE),
        TrackTagField.ARTIST to id(Id3v2FrameIds.V22.ARTIST, Id3v2FrameIds.ARTIST),
        TrackTagField.ALBUM to id(Id3v2FrameIds.V22.ALBUM, Id3v2FrameIds.ALBUM),
        TrackTagField.ALBUM_ARTIST to id(Id3v2FrameIds.V22.ALBUM_ARTIST, Id3v2FrameIds.ALBUM_ARTIST),
        TrackTagField.COMPOSER to id(Id3v2FrameIds.V22.COMPOSER, Id3v2FrameIds.COMPOSER),
        TrackTagField.LYRICIST to id(Id3v2FrameIds.V22.LYRICIST, Id3v2FrameIds.LYRICIST),
        TrackTagField.GENRE to id(Id3v2FrameIds.V22.GENRE, Id3v2FrameIds.GENRE),
        TrackTagField.LANGUAGE to id(Id3v2FrameIds.V22.LANGUAGE, Id3v2FrameIds.LANGUAGE),
        TrackTagField.DATE to when (id3Version) {
            Id3Version.V22 -> Id3v2FrameIds.V22.YEAR
            Id3Version.V23 -> Id3v2FrameIds.YEAR
            Id3Version.V24 -> Id3v2FrameIds.RECORDING_TIME
            null -> Id3v2FrameIds.RECORDING_TIME
        },
        TrackTagField.RATING to Id3v2FrameIds.POPULARITY,
        TrackTagField.TRACK_NUMBER to id(Id3v2FrameIds.V22.TRACK, Id3v2FrameIds.TRACK),
        TrackTagField.TRACK_TOTAL to id(Id3v2FrameIds.V22.TRACK, Id3v2FrameIds.TRACK),
        TrackTagField.DISC_NUMBER to id(Id3v2FrameIds.V22.DISC, Id3v2FrameIds.DISC),
        TrackTagField.DISC_TOTAL to id(Id3v2FrameIds.V22.DISC, Id3v2FrameIds.DISC),
        TrackTagField.LYRICS to id(Id3v2FrameIds.V22.LYRICS, Id3v2FrameIds.LYRICS),
        TrackTagField.ARTWORK to id(Id3v2FrameIds.V22.ARTWORK, Id3v2FrameIds.ARTWORK),
    )
}

/** Replaces the separate ID3 date frames with one normalized entry from the library. */
internal fun List<TagEntry>.mergedByDate(date: String?): List<TagEntry> {
    val dateEntries = if (date == null) emptyList() else filter { it.field == TrackTagField.DATE }
    return if (dateEntries.isEmpty()) {
        this
    } else {
        val merged = TagEntry(
            key = dateEntries.mapNotNull { it.key }.distinct().joinToString(", ").takeIf(String::isNotEmpty),
            value = date,
            field = TrackTagField.DATE,
        )
        val result = ArrayList<TagEntry>(size - dateEntries.size + 1)
        var inserted = false
        for (entry in this) {
            if (entry.field == TrackTagField.DATE) {
                if (!inserted) {
                    result += merged
                    inserted = true
                }
            } else {
                result += entry
            }
        }
        result
    }
}

/** Uses the library's typed rating for binary rating entries such as POPM and `rate`. */
internal fun List<TagEntry>.filledRating(rating: String?): List<TagEntry> {
    if (rating == null) return this
    return map { entry ->
        if (entry.field == TrackTagField.RATING && entry.value.isNullOrEmpty()) {
            entry.copy(value = rating)
        } else {
            entry
        }
    }
}

private data class NumberKeys(val track: String, val trackTotal: String, val disc: String, val discTotal: String)

private fun AudioFile.numberKeys(): NumberKeys? = when (tagContainer) {
    TagContainer.ID3V2 -> if (id3Version == Id3Version.V22) {
        NumberKeys(
            Id3v2FrameIds.V22.TRACK,
            Id3v2FrameIds.V22.TRACK,
            Id3v2FrameIds.V22.DISC,
            Id3v2FrameIds.V22.DISC,
        )
    } else {
        NumberKeys(
            Id3v2FrameIds.TRACK,
            Id3v2FrameIds.TRACK,
            Id3v2FrameIds.DISC,
            Id3v2FrameIds.DISC,
        )
    }

    TagContainer.VORBIS_COMMENT -> NumberKeys(
        VorbisCommentKeys.TRACK_NUMBER,
        VorbisCommentKeys.TRACK_TOTAL,
        VorbisCommentKeys.DISC_NUMBER,
        VorbisCommentKeys.DISC_TOTAL,
    )

    TagContainer.MP4_ILST -> NumberKeys(
        Mp4ItemIds.TRACK,
        Mp4ItemIds.TRACK,
        Mp4ItemIds.DISC,
        Mp4ItemIds.DISC,
    )

    null -> null
}

internal fun AudioFile.numberEntries(): List<TagEntry> {
    val keys = numberKeys() ?: return emptyList()
    return buildList {
        trackNumber?.let { add(TagEntry(keys.track, it.toString(), TrackTagField.TRACK_NUMBER)) }
        trackTotal?.let { add(TagEntry(keys.trackTotal, it.toString(), TrackTagField.TRACK_TOTAL)) }
        discNumber?.let { add(TagEntry(keys.disc, it.toString(), TrackTagField.DISC_NUMBER)) }
        discTotal?.let { add(TagEntry(keys.discTotal, it.toString(), TrackTagField.DISC_TOTAL)) }
    }
}

internal fun AudioFile.numberKeySet(): Set<String> {
    val keys = numberKeys() ?: return emptySet()
    return setOf(keys.track, keys.trackTotal, keys.disc, keys.discTotal)
        .map { it.lowercase() }
        .toSet()
}

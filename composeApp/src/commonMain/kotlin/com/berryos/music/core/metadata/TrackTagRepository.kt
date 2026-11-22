package com.berryos.music.core.metadata

import com.berryos.music.core.model.Track

/** Reads and writes the tag metadata stored in a track's file. */
interface TrackTagRepository {
    suspend fun inspect(track: Track): TrackTagInspection

    /**
     * Writes [edits] back to the file; empty values remove the corresponding field.
     * [artists] replaces the multi-valued artist list when present.
     */
    suspend fun save(track: Track, edits: Map<TrackTagField, String>, artists: List<String>? = null)

    /** Renames the file through MediaStore. */
    suspend fun rename(track: Track, filename: String)
}

/** One tag container found in the file, shown as technical information. */
data class TrackTagInspection(
    val tagType: String?,
    val entries: List<TagEntry>,
    val filename: String? = null,
    val artists: List<String> = emptyList(),
)

/** Fields that have a readable label; other entries keep their raw key. */
enum class TrackTagField {
    TITLE,
    ARTIST,
    ALBUM,
    ALBUM_ARTIST,
    COMPOSER,
    LYRICIST,
    GENRE,
    LANGUAGE,
    DATE,
    RATING,
    TRACK_NUMBER,
    TRACK_TOTAL,
    DISC_NUMBER,
    DISC_TOTAL,
    LYRICS,
    ARTWORK,
}

/**
 * One raw tag entry. [key] is null for a missing prioritized field; [value] is null for binary data.
 */
data class TagEntry(
    val key: String?,
    val value: String?,
    val field: TrackTagField? = null,
    val picture: TagPicture? = null,
)

/** Attached picture metadata; image bytes are never shown. */
data class TagPicture(val type: Int, val mimeType: String?, val description: String?)

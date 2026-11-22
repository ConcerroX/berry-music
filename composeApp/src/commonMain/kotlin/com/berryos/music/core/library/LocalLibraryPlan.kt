package com.berryos.music.core.library

import com.berryos.music.core.database.entity.LocalTrackEntity

/** One row produced by a MediaStore query; only file facts are used. */
internal data class MediaTrackRow(val mediaId: Long, val filename: String, val dateModified: Long)

/** Difference between the MediaStore listing and the cached local library. */
internal class LocalSyncPlan(
    val changedTracks: List<LocalTrackEntity>,
    val deletedIds: List<Long>,
    val clearAll: Boolean,
) {
    val hasChanges: Boolean
        get() = clearAll || changedTracks.isNotEmpty() || deletedIds.isNotEmpty()
}

internal fun personKey(name: String): String = name.trim()

internal const val UNKNOWN_ALBUM_KEY = "unknown"

internal fun filenameTitle(filename: String): String = filename.substringBeforeLast('.', filename).ifBlank { filename }

internal fun planLocalSync(
    existing: List<LocalTrackEntity>,
    media: List<MediaTrackRow>,
    unknownTitle: String,
    unknownArtist: String,
    unknownAlbum: String,
): LocalSyncPlan {
    val existingById = existing.associateBy { it.mediaId }
    val mediaIds = media.mapTo(mutableSetOf()) { it.mediaId }
    val changed = ArrayList<LocalTrackEntity>()

    for (row in media) {
        val old = existingById[row.mediaId]
        val entity = when {
            old == null -> placeholderTrack(row, unknownTitle, unknownArtist, unknownAlbum)

            old.filename == row.filename && old.dateModified == row.dateModified -> old

            else -> old.copy(
                filename = row.filename,
                dateModified = row.dateModified,
                tagScannedAt = 0,
            )
        }
        if (entity != old) changed += entity
    }

    return LocalSyncPlan(
        changedTracks = changed,
        deletedIds = existingById.keys.filterNot(mediaIds::contains),
        clearAll = media.isEmpty() && existing.isNotEmpty(),
    )
}

private fun placeholderTrack(
    row: MediaTrackRow,
    unknownTitle: String,
    unknownArtist: String,
    unknownAlbum: String,
): LocalTrackEntity = LocalTrackEntity(
    mediaId = row.mediaId,
    filename = row.filename,
    title = filenameTitle(row.filename).ifBlank { unknownTitle },
    artistName = unknownArtist,
    album = unknownAlbum,
    albumKey = UNKNOWN_ALBUM_KEY,
    trackNumber = 0,
    year = 0,
    durationMs = 0,
    dateModified = row.dateModified,
    tagScannedAt = 0,
)

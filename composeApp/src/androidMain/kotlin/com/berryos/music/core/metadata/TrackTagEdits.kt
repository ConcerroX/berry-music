package com.berryos.music.core.metadata

import com.berryos.audiometadata.AudioFile
import com.berryos.audiometadata.ExperimentalAudioMetadataApi

@OptIn(ExperimentalAudioMetadataApi::class)
internal fun AudioFile.applyEdits(edits: Map<TrackTagField, String>) {
    applyTextEdits(edits)
    applyNumberEdits(edits)
}

private fun AudioFile.applyTextEdits(edits: Map<TrackTagField, String>) {
    edits[TrackTagField.TITLE]?.let { title = it.ifBlank { null } }
    edits[TrackTagField.ARTIST]?.let { artists = it.splitValues() }
    edits[TrackTagField.ALBUM]?.let { album = it.ifBlank { null } }
    edits[TrackTagField.ALBUM_ARTIST]?.let { albumArtist = it.ifBlank { null } }
    edits[TrackTagField.COMPOSER]?.let { composers = it.splitValues() }
    edits[TrackTagField.LYRICIST]?.let { lyricist = it.ifBlank { null } }
    edits[TrackTagField.GENRE]?.let { genres = it.splitValues() }
    edits[TrackTagField.LANGUAGE]?.let { language = it.ifBlank { null } }
    edits[TrackTagField.DATE]?.let { recordingDate = it.ifBlank { null } }
    edits[TrackTagField.LYRICS]?.let { lyrics = it.ifBlank { null } }
}

@OptIn(ExperimentalAudioMetadataApi::class)
private fun AudioFile.applyNumberEdits(edits: Map<TrackTagField, String>) {
    edits[TrackTagField.RATING]?.let {
        extensions.rating = it.trim().toIntOrNull()?.coerceIn(0, MAX_RATING)
    }
    edits[TrackTagField.TRACK_NUMBER]?.let { trackNumber = it.trim().toIntOrNull() }
    edits[TrackTagField.TRACK_TOTAL]?.let { trackTotal = it.trim().toIntOrNull() }
    edits[TrackTagField.DISC_NUMBER]?.let { discNumber = it.trim().toIntOrNull() }
    edits[TrackTagField.DISC_TOTAL]?.let { discTotal = it.trim().toIntOrNull() }
}

private fun String.splitValues(): List<String> = split(" | ")
    .map { it.trim() }
    .filter { it.isNotEmpty() }

private const val MAX_RATING = 100

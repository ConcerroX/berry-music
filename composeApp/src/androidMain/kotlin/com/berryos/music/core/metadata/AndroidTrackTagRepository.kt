package com.berryos.music.core.metadata

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.berryos.audiometadata.AudioFile
import com.berryos.audiometadata.ExperimentalAudioMetadataApi
import com.berryos.audiometadata.Id3Version
import com.berryos.audiometadata.TagContainer
import com.berryos.audiometadata.id3.Id3v2FrameIds
import com.berryos.audiometadata.id3.Id3v2Tag
import com.berryos.audiometadata.mp4.Mp4Item
import com.berryos.audiometadata.mp4.Mp4ItemIds
import com.berryos.audiometadata.mp4.Mp4Tag
import com.berryos.audiometadata.vorbis.VorbisCommentKeys
import com.berryos.audiometadata.vorbis.VorbisCommentTag
import com.berryos.music.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalAudioMetadataApi::class)
class AndroidTrackTagRepository(context: Context) : TrackTagRepository {
    private val resolver = context.applicationContext.contentResolver

    override suspend fun inspect(track: Track): TrackTagInspection = withContext(Dispatchers.IO) {
        val mediaId = track.id.toLongOrNull()
            ?: throw IllegalArgumentException("Track id is not a MediaStore id")
        val filename = queryColumn(mediaId, MediaStore.Audio.Media.DISPLAY_NAME)
        resolver.openFileDescriptor(audioUri(mediaId), "r")?.use { descriptor ->
            val audio = AudioFile.read(FileDescriptorSource(descriptor), includeArtworks = false)
            val inspection = audio?.toInspection() ?: TrackTagInspection(tagType = null, entries = emptyList())
            inspection.copy(filename = filename)
        } ?: throw IllegalStateException("Unable to open the audio file")
    }

    override suspend fun save(
        track: Track,
        edits: Map<TrackTagField, String>,
        personValues: Map<TrackTagField, List<String>>,
    ) {
        withContext(Dispatchers.IO) {
            val mediaId = track.id.toLongOrNull()
                ?: throw IllegalArgumentException("Track id is not a MediaStore id")
            resolver.openFileDescriptor(audioUri(mediaId), "rw")?.use { descriptor ->
                val source = FileDescriptorSource(descriptor)
                val audio = AudioFile.read(source, includeArtworks = false)
                    ?: error("Unable to read the audio file")
                audio.applyEdits(edits, personValues)
                audio.save(source)
            } ?: error("Unable to open the audio file")
        }
    }

    @Suppress("DEPRECATION")
    override suspend fun rename(track: Track, filename: String): Unit = withContext(Dispatchers.IO) {
        val mediaId = track.id.toLongOrNull()
            ?: throw IllegalArgumentException("Track id is not a MediaStore id")
        val uri = audioUri(mediaId)
        val current = queryColumn(mediaId, MediaStore.Audio.Media.DISPLAY_NAME)
            ?: throw IllegalStateException("Unable to read the current filename")
        val target = renameTarget(current, filename)
        if (target == current) return@withContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, target)
            }
            if (resolver.update(uri, values, null, null) <= 0) {
                throw IllegalStateException("Unable to rename the audio file")
            }
        } else {
            val oldPath = queryColumn(mediaId, MediaStore.MediaColumns.DATA)
                ?: throw IllegalStateException("Unable to read the current file path")
            val oldFile = File(oldPath)
            val newFile = File(oldFile.parentFile, target)
            if (!oldFile.renameTo(newFile)) {
                throw IllegalStateException("Unable to rename the audio file")
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, newFile.absolutePath)
                put(MediaStore.MediaColumns.DISPLAY_NAME, target)
            }
            resolver.update(uri, values, null, null)
        }
    }

    private fun queryColumn(mediaId: Long, column: String): String? =
        resolver.query(audioUri(mediaId), arrayOf(column), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}

@OptIn(ExperimentalAudioMetadataApi::class)
private fun AudioFile.toInspection(): TrackTagInspection {
    val id3Tag = id3
    val vorbisTag = vorbis
    val mp4Tag = mp4
    val inspection = when (tagContainer) {
        TagContainer.ID3V2 -> id3Tag?.toInspection() ?: emptyInspection()
        TagContainer.VORBIS_COMMENT -> vorbisTag?.toInspection() ?: emptyInspection()
        TagContainer.MP4_ILST -> mp4Tag?.toInspection() ?: emptyInspection()
        null -> emptyInspection()
    }
    val numberKeys = numberKeySet()
    val numbers = numberEntries()
    val fieldKeys = expectedFieldKeys()
    return inspection.copy(
        entries = inspection.entries
            .filterNot { entry -> entry.key?.lowercase()?.let(numberKeys::contains) == true }
            .plus(numbers)
            .withMissingFields(fieldKeys)
            .mergedByDate(recordingDate)
            .filledRating(extensions.rating?.toString())
            .prioritizedByField(),
        artists = artists.filter(String::isNotBlank),
        composers = composers.filter(String::isNotBlank),
        albumArtists = albumArtists.filter(String::isNotBlank),
        lyricists = lyricists.filter(String::isNotBlank),
        arrangers = arrangers.filter(String::isNotBlank),
    )
}

private fun emptyInspection(): TrackTagInspection = TrackTagInspection(tagType = null, entries = emptyList())

private fun Id3v2Tag.toInspection(): TrackTagInspection {
    val pictureFrameId = if (version == Id3Version.V22) {
        Id3v2FrameIds.V22.ARTWORK
    } else {
        Id3v2FrameIds.ARTWORK
    }
    val entries = frames
        .filter { it.id != pictureFrameId }
        .map { it.id }
        .distinct()
        .map { id ->
            val values = textValues(id)
            TagEntry(
                key = id,
                value = values.joinToString(" | ").takeIf(String::isNotEmpty),
                field = trackTagField(id),
            )
        } + pictureInfos().map { picture ->
        TagEntry(
            key = pictureFrameId,
            value = null,
            field = TrackTagField.ARTWORK,
            picture = TagPicture(picture.pictureType, picture.mimeType, picture.description),
        )
    }
    return TrackTagInspection(tagType = "ID3v2.${version.majorVersion}", entries = entries)
}

private fun VorbisCommentTag.toInspection(): TrackTagInspection = TrackTagInspection(
    tagType = "Vorbis Comment",
    entries = comments
        .filterNot { comment -> comment.key.lowercase() in VORBIS_PICTURE_KEYS }
        .map { comment ->
            TagEntry(
                key = comment.key,
                value = comment.value,
                field = trackTagField(comment.key),
            )
        },
)

private fun Mp4Tag.toInspection(): TrackTagInspection = TrackTagInspection(
    tagType = "iTunes ilst",
    entries = allItems.map { it.toTagEntry() },
)

private fun Mp4Item.toTagEntry(): TagEntry {
    val value = text?.takeIf(String::isNotEmpty)
    return TagEntry(identifier, value, trackTagField(identifier))
}

private val VORBIS_PICTURE_KEYS = setOf(
    VorbisCommentKeys.METADATA_BLOCK_PICTURE,
    VorbisCommentKeys.COVERART,
    VorbisCommentKeys.COVERART_MIME,
    VorbisCommentKeys.COVERART_TYPE,
).map { it.lowercase() }.toSet()

private fun trackTagField(key: String): TrackTagField? = when (key.lowercase()) {
    Id3v2FrameIds.TITLE.lowercase(), Id3v2FrameIds.V22.TITLE.lowercase(),
    VorbisCommentKeys.TITLE.lowercase(), Mp4ItemIds.TITLE.lowercase(),
    -> TrackTagField.TITLE

    Id3v2FrameIds.ARTIST.lowercase(), Id3v2FrameIds.V22.ARTIST.lowercase(),
    VorbisCommentKeys.ARTIST.lowercase(), Mp4ItemIds.ARTIST.lowercase(),
    -> TrackTagField.ARTIST

    Id3v2FrameIds.ALBUM.lowercase(), Id3v2FrameIds.V22.ALBUM.lowercase(),
    VorbisCommentKeys.ALBUM.lowercase(), Mp4ItemIds.ALBUM.lowercase(),
    -> TrackTagField.ALBUM

    Id3v2FrameIds.ALBUM_ARTIST.lowercase(), Id3v2FrameIds.V22.ALBUM_ARTIST.lowercase(),
    VorbisCommentKeys.ALBUM_ARTIST.lowercase(), Mp4ItemIds.ALBUM_ARTIST.lowercase(),
    -> TrackTagField.ALBUM_ARTIST

    Id3v2FrameIds.COMPOSER.lowercase(), Id3v2FrameIds.V22.COMPOSER.lowercase(),
    VorbisCommentKeys.COMPOSER.lowercase(), Mp4ItemIds.COMPOSER.lowercase(),
    -> TrackTagField.COMPOSER

    Id3v2FrameIds.LYRICIST.lowercase(), Id3v2FrameIds.V22.LYRICIST.lowercase(),
    VorbisCommentKeys.LYRICIST.lowercase(), VorbisCommentKeys.TEXT.lowercase(),
    Mp4ItemIds.freeformIdentifier(Mp4ItemIds.LYRICIST).lowercase(),
    -> TrackTagField.LYRICIST

    Id3v2FrameIds.INVOLVED_PEOPLE.lowercase(), Id3v2FrameIds.INVOLVED_PEOPLE_V23.lowercase(),
    Id3v2FrameIds.V22.INVOLVED_PEOPLE.lowercase(),
    VorbisCommentKeys.ARRANGER.lowercase(),
    Mp4ItemIds.freeformIdentifier(Mp4ItemIds.ARRANGER).lowercase(),
    -> TrackTagField.ARRANGER

    Id3v2FrameIds.GENRE.lowercase(), Id3v2FrameIds.V22.GENRE.lowercase(),
    VorbisCommentKeys.GENRE.lowercase(), Mp4ItemIds.GENRE.lowercase(),
    -> TrackTagField.GENRE

    Id3v2FrameIds.LANGUAGE.lowercase(), Id3v2FrameIds.V22.LANGUAGE.lowercase(),
    VorbisCommentKeys.LANGUAGE.lowercase(),
    Mp4ItemIds.freeformIdentifier(Mp4ItemIds.LANGUAGE).lowercase(),
    -> TrackTagField.LANGUAGE

    Id3v2FrameIds.YEAR.lowercase(), Id3v2FrameIds.RECORDING_TIME.lowercase(),
    Id3v2FrameIds.DATE.lowercase(), Id3v2FrameIds.TIME.lowercase(),
    Id3v2FrameIds.V22.YEAR.lowercase(), Id3v2FrameIds.V22.DATE.lowercase(),
    Id3v2FrameIds.V22.TIME.lowercase(),
    VorbisCommentKeys.DATE.lowercase(), Mp4ItemIds.YEAR.lowercase(),
    -> TrackTagField.DATE

    Id3v2FrameIds.POPULARITY.lowercase(),
    VorbisCommentKeys.RATING.lowercase(), Mp4ItemIds.RATING.lowercase(),
    -> TrackTagField.RATING

    Id3v2FrameIds.LYRICS.lowercase(), Id3v2FrameIds.V22.LYRICS.lowercase(),
    VorbisCommentKeys.LYRICS.lowercase(), VorbisCommentKeys.UNSYNCED_LYRICS.lowercase(),
    Mp4ItemIds.LYRICS.lowercase(),
    -> TrackTagField.LYRICS

    else -> null
}

private fun renameTarget(current: String, requested: String): String {
    val target = requested.trim().trimEnd('.')
    require(target.isNotEmpty()) { "Filename must not be empty" }
    require('/' !in target && '\\' !in target) { "Filename must not contain path separators" }
    val extension = current.substringAfterLast('.', "")
    val hasExtension = target.substringAfterLast('.', "").isNotEmpty()
    return if (extension.isNotEmpty() && !hasExtension) "$target.$extension" else target
}

internal fun audioUri(mediaId: Long): Uri {
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
    return ContentUris.withAppendedId(collection, mediaId)
}

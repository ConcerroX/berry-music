package com.berryos.music.core.metadata

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.system.Os
import com.berryos.audiometadata.AudioFile
import com.berryos.audiometadata.ExperimentalAudioMetadataApi
import com.berryos.audiometadata.Id3Version
import com.berryos.audiometadata.TagContainer
import com.berryos.audiometadata.id3.Id3v2FrameIds
import com.berryos.audiometadata.id3.Id3v2Tag
import com.berryos.audiometadata.io.WritableRandomAccessSource
import com.berryos.audiometadata.mp4.Mp4Item
import com.berryos.audiometadata.mp4.Mp4ItemIds
import com.berryos.audiometadata.mp4.Mp4Tag
import com.berryos.audiometadata.vorbis.VorbisCommentKeys
import com.berryos.audiometadata.vorbis.VorbisCommentTag
import com.berryos.music.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileDescriptor

@OptIn(ExperimentalAudioMetadataApi::class)
class AndroidTrackTagRepository(context: Context) : TrackTagRepository {
    private val resolver = context.applicationContext.contentResolver

    override suspend fun inspect(track: Track): TrackTagInspection = withContext(Dispatchers.IO) {
        val mediaId = track.id.toLongOrNull()
            ?: throw IllegalArgumentException("Track id is not a MediaStore id")
        resolver.openFileDescriptor(audioUri(mediaId), "r")?.use { descriptor ->
            val audio = AudioFile.read(FileDescriptorSource(descriptor), includeArtworks = false)
            audio?.toInspection() ?: TrackTagInspection(tagType = null, entries = emptyList())
        } ?: throw IllegalStateException("Unable to open the audio file")
    }

    override suspend fun save(track: Track, edits: Map<TrackTagField, String>): Unit = withContext(Dispatchers.IO) {
        val mediaId = track.id.toLongOrNull()
            ?: throw IllegalArgumentException("Track id is not a MediaStore id")
        resolver.openFileDescriptor(audioUri(mediaId), "rw")?.use { descriptor ->
            val source = FileDescriptorSource(descriptor)
            val audio = AudioFile.read(source, includeArtworks = false)
                ?: throw IllegalStateException("Unable to read the audio file")
            audio.applyEdits(edits)
            audio.save(source)
        } ?: throw IllegalStateException("Unable to open the audio file")
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

internal fun audioUri(mediaId: Long): Uri {
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
    return ContentUris.withAppendedId(collection, mediaId)
}

private class FileDescriptorSource(descriptor: ParcelFileDescriptor) : WritableRandomAccessSource {
    private val fd: FileDescriptor = descriptor.fileDescriptor
    override val size: Long
        get() = Os.fstat(fd).st_size

    override fun readAt(offset: Long, destination: ByteArray, destinationOffset: Int, length: Int): Int {
        require(offset >= 0) { "offset must not be negative" }
        require(destinationOffset >= 0 && length >= 0 && destinationOffset + length <= destination.size) {
            "invalid destination range"
        }
        if (length == 0) return 0
        var total = 0
        while (total < length) {
            val read = Os.pread(fd, destination, destinationOffset + total, length - total, offset + total)
            if (read <= 0) break
            total += read
        }
        return total
    }

    override fun writeAt(offset: Long, source: ByteArray, sourceOffset: Int, length: Int) {
        require(offset >= 0) { "offset must not be negative" }
        require(sourceOffset >= 0 && length >= 0 && sourceOffset + length <= source.size) {
            "invalid source range"
        }
        var total = 0
        while (total < length) {
            val written = Os.pwrite(fd, source, sourceOffset + total, length - total, offset + total)
            if (written <= 0) error("Unable to write at ${offset + total}")
            total += written
        }
    }

    override fun truncate(size: Long) {
        Os.ftruncate(fd, size)
    }

    override fun flush() {
        Os.fsync(fd)
    }

    override fun close() = Unit
}

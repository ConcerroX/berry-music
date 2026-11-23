package com.berryos.music.core.library

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.berryos.audiometadata.AudioMetadata
import com.berryos.audiometadata.AudioMetadataReader
import com.berryos.music.core.database.dao.LocalLibraryDao
import com.berryos.music.core.database.entity.LocalPersonEntity
import com.berryos.music.core.database.entity.LocalTrackCreditEntity
import com.berryos.music.core.database.entity.LocalTrackEntity
import com.berryos.music.core.metadata.FileDescriptorSource
import com.berryos.music.core.metadata.audioUri
import com.berryos.music.core.model.CreditRole
import com.berryos.music.core.model.LibrarySnapshot
import com.berryos.music.resources.Res
import com.berryos.music.resources.unknown_album
import com.berryos.music.resources.unknown_artist
import com.berryos.music.resources.unknown_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import java.security.MessageDigest

/**
 * Reconciles MediaStore file facts with the tag-derived library cache in Room.
 * MediaStore supplies file ids, names and modification times only; every metadata
 * field stored for a track comes from its tags.
 */
class LocalLibrarySync(context: Context, private val dao: LocalLibraryDao, private val scope: CoroutineScope) {
    private val resolver: ContentResolver = context.applicationContext.contentResolver
    private val syncMutex = Mutex()
    private val enrichmentMutex = Mutex()
    private var enrichmentJob: Job? = null
    private val updates = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @Volatile private var labels = Labels(title = "", artist = "", album = "")

    /** Emits whenever the cached library rows change. */
    val changes: SharedFlow<Unit> = updates.asSharedFlow()

    suspend fun refresh() {
        val plan = syncMutex.withLock { prepareAndApply() }
        if (plan?.hasChanges == true) updates.tryEmit(Unit)
        startEnrichment()
    }

    suspend fun snapshot(): LibrarySnapshot = withContext(Dispatchers.IO) {
        val current = labels
        buildLocalSnapshot(
            tracks = dao.tracks(),
            credits = dao.credits(),
            people = dao.people(),
            unknownTitle = current.title,
            unknownArtist = current.artist,
            unknownAlbum = current.album,
        )
    }

    fun close() {
        enrichmentJob?.cancel()
        enrichmentJob = null
    }

    private suspend fun prepareAndApply(): LocalSyncPlan? = withContext(Dispatchers.IO) {
        val current = Labels(
            title = getString(Res.string.unknown_title),
            artist = getString(Res.string.unknown_artist),
            album = getString(Res.string.unknown_album),
        )
        labels = current
        val media = queryMedia() ?: return@withContext null
        val plan = planLocalSync(
            existing = dao.tracks(),
            media = media,
            unknownTitle = current.title,
            unknownArtist = current.artist,
            unknownAlbum = current.album,
        )
        when {
            plan.clearAll -> dao.deleteAllTracks()
            plan.deletedIds.isNotEmpty() -> dao.deleteTracks(plan.deletedIds)
        }
        if (plan.changedTracks.isNotEmpty()) dao.upsertTracks(plan.changedTracks)
        plan
    }

    private fun queryMedia(): List<MediaTrackRow>? {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_MODIFIED,
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
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                buildList {
                    while (cursor.moveToNext()) {
                        val mediaId = cursor.getLong(idColumn)
                        add(
                            MediaTrackRow(
                                mediaId = mediaId,
                                filename = cursor.getString(nameColumn) ?: mediaId.toString(),
                                dateModified = cursor.getLong(modifiedColumn),
                            ),
                        )
                    }
                }
            }
        }.getOrNull()
    }

    private suspend fun startEnrichment() {
        enrichmentMutex.withLock {
            if (enrichmentJob?.isActive == true) return
            enrichmentJob = scope.launch { enrichLoop() }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun enrichLoop() {
        try {
            while (true) {
                val batch = withContext(Dispatchers.IO) { dao.staleTracks(BATCH_SIZE) }
                if (batch.isEmpty()) return
                batch.forEach { track -> enrich(track) }
                updates.tryEmit(Unit)
            }
        } catch (throwable: Throwable) {
            Log.w(TAG, "Local tag enrichment failed", throwable)
        }
    }

    private suspend fun enrich(track: LocalTrackEntity) = withContext(Dispatchers.IO) {
        val read = runCatching { readMetadata(track) }
        val error = read.exceptionOrNull()
        if (error != null) {
            Log.w(TAG, "Unable to read tags for ${track.mediaId}; keeping cached metadata", error)
            dao.updateScanState(track.mediaId, System.currentTimeMillis())
            return@withContext
        }

        val metadata = read.getOrNull()
        val artists = metadata?.artists.orEmpty().filter { it.isNotBlank() }
            .ifEmpty { listOf(labels.artist) }
        val album = metadata?.album?.takeIf { it.isNotBlank() } ?: labels.album
        val albumArtists = metadata?.albumArtists.orEmpty().filter { it.isNotBlank() }
        val albumArtist = albumArtists.firstOrNull()
        val displayArtist = artists.first()
        val albumKey = if (album == labels.album) {
            UNKNOWN_ALBUM_KEY
        } else {
            hashAlbumKey(album, albumArtist ?: displayArtist)
        }

        val candidates = buildList {
            artists.forEach { add(CreditCandidate(CreditRole.ARTIST, it)) }
            albumArtists.forEach { add(CreditCandidate(CreditRole.ALBUM_ARTIST, it)) }
            metadata?.composers.orEmpty().filter { it.isNotBlank() }.forEach {
                add(CreditCandidate(CreditRole.COMPOSER, it))
            }
            metadata?.lyricists.orEmpty().filter { it.isNotBlank() }.forEach {
                add(CreditCandidate(CreditRole.LYRICIST, it))
            }
            metadata?.arrangers.orEmpty().filter { it.isNotBlank() }.forEach {
                add(CreditCandidate(CreditRole.ARRANGER, it))
            }
        }

        val entity = track.copy(
            title = metadata?.title?.takeIf { it.isNotBlank() }
                ?: filenameTitle(track.filename).ifBlank { labels.title },
            artistName = displayArtist,
            album = album,
            albumKey = albumKey,
            trackNumber = metadata?.trackNumber ?: 0,
            year = parseYear(metadata?.year),
            durationMs = metadata?.audioInfo?.durationMs ?: 0L,
            tagScannedAt = System.currentTimeMillis(),
        )
        val (people, credits) = creditRows(track.mediaId, candidates)
        dao.upsertTracks(listOf(entity))
        if (people.isNotEmpty()) dao.upsertPeople(people)
        dao.replaceCredits(track.mediaId, credits)
    }

    private fun readMetadata(track: LocalTrackEntity): AudioMetadata? {
        val descriptor = resolver.openFileDescriptor(audioUri(track.mediaId), "r")
            ?: error("openFileDescriptor returned null")
        return descriptor.use { raw -> AudioMetadataReader.read(FileDescriptorSource(raw)) }
    }

    private fun creditRows(
        mediaId: Long,
        candidates: List<CreditCandidate>,
    ): Pair<List<LocalPersonEntity>, List<LocalTrackCreditEntity>> {
        val names = LinkedHashMap<String, String>()
        val rows = mutableListOf<LocalTrackCreditEntity>()
        val positions = mutableMapOf<CreditRole, Int>()
        candidates.forEach { candidate ->
            val key = personKey(candidate.name)
            if (key.isEmpty()) return@forEach
            val position = positions.getOrDefault(candidate.role, 0)
            if (rows.any { it.personKey == key && it.role == candidate.role }) return@forEach
            names.putIfAbsent(key, candidate.name.trim())
            rows += LocalTrackCreditEntity(mediaId, key, candidate.role, position)
            positions[candidate.role] = position + 1
        }
        return names.map { (key, name) -> LocalPersonEntity(key, name) } to rows
    }

    private fun hashAlbumKey(album: String, artist: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$album\u0000$artist".encodeToByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun parseYear(year: String?): Int = year?.substringBefore('-')?.toIntOrNull() ?: 0

    private data class Labels(val title: String, val artist: String, val album: String)

    private data class CreditCandidate(val role: CreditRole, val name: String)

    private companion object {
        const val TAG = "LocalLibrarySync"
        const val BATCH_SIZE = 50
        const val MIN_DURATION_MS = 10_000L
    }
}

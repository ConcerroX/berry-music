package com.berryos.music.core.designsystem

import android.content.Context
import android.util.Log
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.berryos.audiometadata.AudioArtworkReader
import com.berryos.music.core.library.LOCAL_COVER_SCHEME
import com.berryos.music.core.metadata.FileDescriptorSource
import com.berryos.music.core.metadata.audioUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer

/**
 * Loads album art embedded in a local track. Extracted bytes are stored in Coil's disk
 * cache keyed by the cover URL, so each track's artwork is read from the audio file once.
 */
class LocalCoverFetcher private constructor(
    private val cacheKey: String,
    private val mediaId: Long,
    private val options: Options,
    private val diskCache: DiskCache?,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        runCatching {
            val cache = diskCache
            if (cache != null) {
                cache.openSnapshot(cacheKey)?.let { snapshot ->
                    return@runCatching cachedResult(cache, snapshot)
                }
            }

            val resolver = (options.context as Context).contentResolver
            val descriptor = resolver.openFileDescriptor(audioUri(mediaId), "r") ?: return@runCatching null
            val artworks = descriptor.use { raw ->
                AudioArtworkReader.read(FileDescriptorSource(raw))
            }
            val artwork = artworks.firstOrNull { it.pictureType == FRONT_COVER } ?: artworks.firstOrNull()
                ?: return@runCatching null

            if (cache != null) {
                val editor = cache.openEditor(cacheKey)
                if (editor != null) {
                    val snapshot = runCatching {
                        cache.fileSystem.write(editor.data) { write(artwork.data) }
                        editor.commitAndOpenSnapshot()
                    }.onFailure { editor.abort() }.getOrNull()
                    if (snapshot != null) {
                        return@runCatching SourceFetchResult(
                            source = ImageSource(snapshot.data, cache.fileSystem, cacheKey, snapshot),
                            mimeType = artwork.mimeType,
                            dataSource = DataSource.DISK,
                        )
                    }
                }
            }

            SourceFetchResult(
                source = ImageSource(Buffer().write(artwork.data), options.fileSystem),
                mimeType = artwork.mimeType,
                dataSource = DataSource.DISK,
            )
        }.onFailure { Log.w(TAG, "Unable to read embedded cover for $mediaId", it) }.getOrNull()
    }

    private fun cachedResult(cache: DiskCache, snapshot: DiskCache.Snapshot): SourceFetchResult = SourceFetchResult(
        source = ImageSource(snapshot.data, cache.fileSystem, cacheKey, snapshot),
        mimeType = null,
        dataSource = DataSource.DISK,
    )

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != LOCAL_COVER_SCHEME) return null
            val mediaId = data.authority?.toLongOrNull() ?: return null
            return LocalCoverFetcher(data.toString(), mediaId, options, imageLoader.diskCache)
        }
    }

    private companion object {
        const val TAG = "LocalCoverFetcher"
        const val FRONT_COVER = 3
    }
}

/** Installs the shared image loader with local embedded-cover support. */
object LocalCoverImageLoader {
    fun install() {
        SingletonImageLoader.setSafe { platformContext ->
            ImageLoader.Builder(platformContext)
                .components { add(LocalCoverFetcher.Factory()) }
                .build()
        }
    }
}

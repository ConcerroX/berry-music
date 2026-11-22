package com.berryos.music.core.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Resolves media session artwork through Coil, which also serves the embedded covers of local
 * tracks that the platform's data source cannot read.
 */
@OptIn(UnstableApi::class)
internal class CoilBitmapLoader(private val context: Context) :
    BitmapLoader,
    AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        return if (bitmap != null) {
            Futures.immediateFuture(bitmap)
        } else {
            Futures.immediateFailedFuture(IllegalArgumentException("Unsupported image data"))
        }
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            val result = runCatching {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .allowHardware(false)
                    .build()
                val success = SingletonImageLoader.get(context).execute(request) as? SuccessResult
                    ?: error("Unable to load artwork: $uri")
                success.image.toBitmap()
            }
            result.fold(future::set, future::setException)
        }
        return future
    }

    override fun close() {
        scope.cancel()
    }
}

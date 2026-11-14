package com.berryos.music.core.library

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
fun mediaStoreChanges(context: Context): Flow<Unit> = callbackFlow {
    val resolver = context.applicationContext.contentResolver
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            trySend(Unit)
        }
    }
    resolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
    resolver.registerContentObserver(MEDIA_ROOT, true, observer)
    awaitClose { resolver.unregisterContentObserver(observer) }
}.debounce(DEBOUNCE_MS)

private val MEDIA_ROOT = Uri.parse("content://media")
private const val DEBOUNCE_MS = 500L

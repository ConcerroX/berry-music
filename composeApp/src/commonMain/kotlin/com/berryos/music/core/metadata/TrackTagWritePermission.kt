package com.berryos.music.core.metadata

import androidx.compose.runtime.Composable
import com.berryos.music.core.model.Track

/** Requests write access to a local track's media file before saving tags. */
class TrackTagWritePermission internal constructor(private val requestWrite: (Track, () -> Unit) -> Unit) {
    fun request(track: Track, onGranted: () -> Unit) = requestWrite(track, onGranted)
}

@Composable
expect fun rememberTrackTagWritePermission(): TrackTagWritePermission

package com.berryos.music.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import coil3.Image

/**
 * Cover colors for one cover URL: starts from the process-wide cache, falls back to the default
 * cover colors, and computes or caches the result when the cover image loads.
 */
@Stable
class AlbumCoverColorsState internal constructor(
    private val coverUrl: String?,
    private val fallback: State<CoverColors?>,
) {
    private var coverColors by mutableStateOf(coverUrl?.let(::cachedCoverColors))

    val colors: CoverColors?
        get() = coverColors ?: fallback.value

    fun onImage(image: Image?) {
        val url = coverUrl ?: return
        if (coverColors != null || image == null) {
            return
        }
        val computed = image.coverColorsOf()
        coverColors = computed
        if (computed != null) {
            cacheCoverColors(url, computed)
        }
    }
}

@Composable
fun rememberAlbumCoverColors(coverUrl: String?): AlbumCoverColorsState {
    val fallbackColors = rememberDefaultCoverColors()
    val fallback = rememberUpdatedState(fallbackColors)
    return remember(coverUrl) { AlbumCoverColorsState(coverUrl, fallback) }
}

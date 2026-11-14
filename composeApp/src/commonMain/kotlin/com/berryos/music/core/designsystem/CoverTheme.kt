package com.berryos.music.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult

/**
 * Cover colors for [coverUrl], loading and caching them when no other screen has shown that
 * cover yet.
 */
@Composable
fun rememberLoadedCoverColors(coverUrl: String?): CoverColors? {
    val cached = coverUrl?.let(::cachedCoverColors)
    var loaded by remember(coverUrl) { mutableStateOf<CoverColors?>(null) }
    val context = LocalPlatformContext.current
    LaunchedEffect(coverUrl) {
        if (coverUrl != null) {
            loaded = loadCoverColors(context, coverUrl)
        }
    }
    return cached ?: loaded
}

private suspend fun loadCoverColors(context: PlatformContext, coverUrl: String): CoverColors? {
    val request = ImageRequest.Builder(context)
        .data(coverUrl)
        .size(THEME_COVER_SIZE)
        .build()
    val result = runCatching { SingletonImageLoader.get(context).execute(request) }
        .getOrNull() as? SuccessResult ?: return null
    return runCatching { result.image.coverColorsOf() }
        .getOrNull()
        ?.also { cacheCoverColors(coverUrl, it) }
}

private const val THEME_COVER_SIZE = 64

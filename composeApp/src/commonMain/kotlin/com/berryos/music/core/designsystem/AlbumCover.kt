package com.berryos.music.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.Image
import coil3.compose.AsyncImage
import com.berryos.music.resources.Res
import com.berryos.music.resources.default_album_art
import org.jetbrains.compose.resources.painterResource

@Composable
fun AlbumCover(coverUrl: String?, onImage: (Image?) -> Unit = {}) {
    val fallback = painterResource(Res.drawable.default_album_art)
    var failed by remember(coverUrl) { mutableStateOf(false) }
    if (coverUrl == null || failed) {
        Image(
            painter = fallback,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
    } else {
        AsyncImage(
            model = coverImageModel(coverUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = fallback,
            error = fallback,
            onSuccess = { onImage(it.result.image) },
            onError = { if (!failed) failed = true },
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
    }
}

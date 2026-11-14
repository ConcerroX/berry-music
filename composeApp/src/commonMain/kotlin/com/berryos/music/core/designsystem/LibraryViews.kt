package com.berryos.music.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.Image
import coil3.compose.AsyncImage
import com.berryos.music.resources.Res
import com.berryos.music.resources.default_album_art
import org.jetbrains.compose.resources.painterResource

@Composable
fun LibraryLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun LibraryMessage(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, style = MaterialTheme.typography.body2)
    }
}

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

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

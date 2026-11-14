package com.berryos.music.feature.playlist

import androidx.compose.runtime.Composable
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.playlist
import com.berryos.music.resources.playlist_id
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlaylistScreen(playlistId: String) {
    PlaceholderScreen(
        title = stringResource(Res.string.playlist),
        subtitle = stringResource(Res.string.playlist_id, playlistId),
    )
}

package com.berryos.music.feature.playlists

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.PlaylistRoute
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.playlist_detail
import com.berryos.music.resources.playlists_subtitle
import com.berryos.music.resources.tab_playlists
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlaylistsScreen(actions: AppActions) {
    PlaceholderScreen(
        title = stringResource(Res.string.tab_playlists),
        subtitle = stringResource(Res.string.playlists_subtitle),
    ) {
        TextButton(onClick = { actions.open(PlaylistRoute("demo-playlist")) }) {
            Text(stringResource(Res.string.playlist_detail))
        }
    }
}

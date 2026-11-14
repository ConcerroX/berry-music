package com.berryos.music.feature.artist

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.album_detail
import com.berryos.music.resources.artist
import com.berryos.music.resources.artist_id
import org.jetbrains.compose.resources.stringResource

@Composable
fun ArtistScreen(artistId: String, actions: AppActions) {
    PlaceholderScreen(
        title = stringResource(Res.string.artist),
        subtitle = stringResource(Res.string.artist_id, artistId),
    ) {
        TextButton(onClick = { actions.open(AlbumRoute("demo-album")) }) {
            Text(stringResource(Res.string.album_detail))
        }
    }
}

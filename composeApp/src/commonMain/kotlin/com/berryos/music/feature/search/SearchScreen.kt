package com.berryos.music.feature.search

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.album_detail
import com.berryos.music.resources.search
import com.berryos.music.resources.search_subtitle
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchScreen(actions: AppActions) {
    PlaceholderScreen(
        title = stringResource(Res.string.search),
        subtitle = stringResource(Res.string.search_subtitle),
    ) {
        TextButton(onClick = { actions.open(AlbumRoute("demo-album")) }) {
            Text(stringResource(Res.string.album_detail))
        }
    }
}

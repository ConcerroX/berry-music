package com.berryos.music.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.berryos.music.app.navigation.AboutRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.AppearanceRoute
import com.berryos.music.app.navigation.LibrarySettingsRoute
import com.berryos.music.app.navigation.PlaybackSettingsRoute
import com.berryos.music.app.navigation.ServersRoute
import com.berryos.music.core.designsystem.reportPageScrollable
import com.berryos.music.resources.Res
import com.berryos.music.resources.about
import com.berryos.music.resources.appearance
import com.berryos.music.resources.ic_dns
import com.berryos.music.resources.ic_info_i
import com.berryos.music.resources.ic_library_music
import com.berryos.music.resources.ic_palette
import com.berryos.music.resources.ic_play_circle_outline
import com.berryos.music.resources.library
import com.berryos.music.resources.playback
import com.berryos.music.resources.servers
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(actions: AppActions) {
    val listState = rememberLazyListState()
    listState.reportPageScrollable()
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            SettingsItem(Res.string.appearance, Res.drawable.ic_palette) {
                actions.open(AppearanceRoute)
            }
        }
        item {
            SettingsItem(Res.string.playback, Res.drawable.ic_play_circle_outline) {
                actions.open(PlaybackSettingsRoute)
            }
        }
        item {
            SettingsItem(Res.string.library, Res.drawable.ic_library_music) {
                actions.open(LibrarySettingsRoute)
            }
        }
        item {
            SettingsItem(Res.string.servers, Res.drawable.ic_dns) {
                actions.open(ServersRoute)
            }
        }
        item {
            SettingsItem(Res.string.about, Res.drawable.ic_info_i) {
                actions.open(AboutRoute)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SettingsItem(label: StringResource, icon: DrawableResource, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        icon = { Icon(painter = painterResource(icon), contentDescription = null) },
        text = { Text(stringResource(label)) },
    )
}

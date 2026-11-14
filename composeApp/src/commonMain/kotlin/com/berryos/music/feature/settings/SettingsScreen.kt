package com.berryos.music.feature.settings

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AboutRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.AppearanceRoute
import com.berryos.music.app.navigation.LibrarySettingsRoute
import com.berryos.music.app.navigation.PlaybackSettingsRoute
import com.berryos.music.app.navigation.ServersRoute
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.about
import com.berryos.music.resources.appearance
import com.berryos.music.resources.library
import com.berryos.music.resources.playback
import com.berryos.music.resources.servers
import com.berryos.music.resources.settings
import com.berryos.music.resources.settings_subtitle
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(actions: AppActions) {
    PlaceholderScreen(
        title = stringResource(Res.string.settings),
        subtitle = stringResource(Res.string.settings_subtitle),
    ) {
        TextButton(onClick = { actions.open(AppearanceRoute) }) { Text(stringResource(Res.string.appearance)) }
        TextButton(onClick = { actions.open(PlaybackSettingsRoute) }) { Text(stringResource(Res.string.playback)) }
        TextButton(onClick = { actions.open(LibrarySettingsRoute) }) { Text(stringResource(Res.string.library)) }
        TextButton(onClick = { actions.open(ServersRoute) }) { Text(stringResource(Res.string.servers)) }
        TextButton(onClick = { actions.open(AboutRoute) }) { Text(stringResource(Res.string.about)) }
    }
}

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

@Composable
fun SettingsScreen(actions: AppActions) {
    PlaceholderScreen(title = "Settings", subtitle = "Appearance, playback, library, and servers.") {
        TextButton(onClick = { actions.open(AppearanceRoute) }) { Text("Appearance") }
        TextButton(onClick = { actions.open(PlaybackSettingsRoute) }) { Text("Playback") }
        TextButton(onClick = { actions.open(LibrarySettingsRoute) }) { Text("Library") }
        TextButton(onClick = { actions.open(ServersRoute) }) { Text("Servers") }
        TextButton(onClick = { actions.open(AboutRoute) }) { Text("About") }
    }
}

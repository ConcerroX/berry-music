package com.berryos.music.feature.settings

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.ServerEditRoute
import com.berryos.music.core.designsystem.PlaceholderScreen

@Composable
fun AppearanceScreen() {
    PlaceholderScreen(title = "Appearance", subtitle = "Theme, accent color, and layout.")
}

@Composable
fun PlaybackSettingsScreen() {
    PlaceholderScreen(title = "Playback", subtitle = "Audio focus, resume, and crossfade.")
}

@Composable
fun LibrarySettingsScreen() {
    PlaceholderScreen(title = "Library", subtitle = "Scanning, filters, and excluded folders.")
}

@Composable
fun ServersScreen(actions: AppActions) {
    PlaceholderScreen(title = "Servers", subtitle = "Subsonic server list and connection status.") {
        TextButton(onClick = { actions.open(ServerEditRoute("demo-server")) }) { Text("Server detail") }
    }
}

@Composable
fun ServerEditScreen(serverId: String?) {
    PlaceholderScreen(title = "Server", subtitle = "Server id: ${serverId ?: "new"}")
}

@Composable
fun AboutScreen() {
    PlaceholderScreen(title = "About", subtitle = "Berry Music.")
}

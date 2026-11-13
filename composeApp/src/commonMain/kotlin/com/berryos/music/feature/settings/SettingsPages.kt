package com.berryos.music.feature.settings

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.ServerEditRoute
import com.berryos.music.core.designsystem.PlaceholderScreen

@Composable
fun AppearanceScreen(actions: AppActions) {
    PlaceholderScreen(title = "Appearance", subtitle = "Theme, accent color, and layout.") {
        TextButton(onClick = actions.back) { Text("Back") }
    }
}

@Composable
fun PlaybackSettingsScreen(actions: AppActions) {
    PlaceholderScreen(title = "Playback", subtitle = "Audio focus, resume, and crossfade.") {
        TextButton(onClick = actions.back) { Text("Back") }
    }
}

@Composable
fun LibrarySettingsScreen(actions: AppActions) {
    PlaceholderScreen(title = "Library", subtitle = "Scanning, filters, and excluded folders.") {
        TextButton(onClick = actions.back) { Text("Back") }
    }
}

@Composable
fun ServersScreen(actions: AppActions) {
    PlaceholderScreen(title = "Servers", subtitle = "Subsonic server list and connection status.") {
        TextButton(onClick = actions.back) { Text("Back") }
        TextButton(onClick = { actions.open(ServerEditRoute("demo-server")) }) { Text("Server detail") }
    }
}

@Composable
fun ServerEditScreen(serverId: String?, actions: AppActions) {
    PlaceholderScreen(title = "Server", subtitle = "Server id: ${serverId ?: "new"}") {
        TextButton(onClick = actions.back) { Text("Back") }
    }
}

@Composable
fun AboutScreen(actions: AppActions) {
    PlaceholderScreen(title = "About", subtitle = "Berry Music.") {
        TextButton(onClick = actions.back) { Text("Back") }
    }
}

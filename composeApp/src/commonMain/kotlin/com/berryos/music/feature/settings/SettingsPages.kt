package com.berryos.music.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.ServerEditRoute
import com.berryos.music.core.datastore.AppSettingsRepository
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.about
import com.berryos.music.resources.about_subtitle
import com.berryos.music.resources.collapse_player_on_pause
import com.berryos.music.resources.library
import com.berryos.music.resources.library_subtitle
import com.berryos.music.resources.playback
import com.berryos.music.resources.playback_subtitle
import com.berryos.music.resources.server
import com.berryos.music.resources.server_detail
import com.berryos.music.resources.server_id
import com.berryos.music.resources.server_id_new
import com.berryos.music.resources.servers
import com.berryos.music.resources.servers_subtitle
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppearanceScreen(settings: AppSettingsRepository) {
    val appSettings by settings.settings.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SwitchItem(
                label = stringResource(Res.string.collapse_player_on_pause),
                checked = appSettings.collapsePlayerOnPause,
                onCheckedChange = settings::setCollapsePlayerOnPause,
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwitchItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        text = { Text(label) },
        trailing = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}

@Composable
fun PlaybackSettingsScreen() {
    PlaceholderScreen(
        title = stringResource(Res.string.playback),
        subtitle = stringResource(Res.string.playback_subtitle),
    )
}

@Composable
fun LibrarySettingsScreen() {
    PlaceholderScreen(
        title = stringResource(Res.string.library),
        subtitle = stringResource(Res.string.library_subtitle),
    )
}

@Composable
fun ServersScreen(actions: AppActions) {
    PlaceholderScreen(
        title = stringResource(Res.string.servers),
        subtitle = stringResource(Res.string.servers_subtitle),
    ) {
        TextButton(onClick = { actions.open(ServerEditRoute("demo-server")) }) {
            Text(stringResource(Res.string.server_detail))
        }
    }
}

@Composable
fun ServerEditScreen(serverId: String?) {
    PlaceholderScreen(
        title = stringResource(Res.string.server),
        subtitle = if (serverId == null) {
            stringResource(Res.string.server_id_new)
        } else {
            stringResource(Res.string.server_id, serverId)
        },
    )
}

@Composable
fun AboutScreen() {
    PlaceholderScreen(
        title = stringResource(Res.string.about),
        subtitle = stringResource(Res.string.about_subtitle),
    )
}

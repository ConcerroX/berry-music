package com.berryos.music.feature.settings

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.ServerEditRoute
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.server_detail
import com.berryos.music.resources.servers
import com.berryos.music.resources.servers_subtitle
import org.jetbrains.compose.resources.stringResource

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

package com.berryos.music.feature.settings

import androidx.compose.runtime.Composable
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.server
import com.berryos.music.resources.server_id
import com.berryos.music.resources.server_id_new
import org.jetbrains.compose.resources.stringResource

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

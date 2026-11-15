package com.berryos.music.feature.settings

import androidx.compose.runtime.Composable
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.playback
import com.berryos.music.resources.playback_subtitle
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlaybackSettingsScreen() {
    PlaceholderScreen(
        title = stringResource(Res.string.playback),
        subtitle = stringResource(Res.string.playback_subtitle),
    )
}

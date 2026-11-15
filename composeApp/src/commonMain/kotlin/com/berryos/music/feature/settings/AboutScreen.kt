package com.berryos.music.feature.settings

import androidx.compose.runtime.Composable
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.about
import com.berryos.music.resources.about_subtitle
import org.jetbrains.compose.resources.stringResource

@Composable
fun AboutScreen() {
    PlaceholderScreen(
        title = stringResource(Res.string.about),
        subtitle = stringResource(Res.string.about_subtitle),
    )
}

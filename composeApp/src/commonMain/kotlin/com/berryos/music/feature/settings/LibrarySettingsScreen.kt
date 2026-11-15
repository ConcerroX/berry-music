package com.berryos.music.feature.settings

import androidx.compose.runtime.Composable
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.library
import com.berryos.music.resources.library_subtitle
import org.jetbrains.compose.resources.stringResource

@Composable
fun LibrarySettingsScreen() {
    PlaceholderScreen(
        title = stringResource(Res.string.library),
        subtitle = stringResource(Res.string.library_subtitle),
    )
}

package com.berryos.music.feature.folder

import androidx.compose.runtime.Composable
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.folder
import com.berryos.music.resources.folder_id
import org.jetbrains.compose.resources.stringResource

@Composable
fun FolderScreen(folderId: String) {
    PlaceholderScreen(
        title = stringResource(Res.string.folder),
        subtitle = stringResource(Res.string.folder_id, folderId),
    )
}

package com.berryos.music.feature.folder

import androidx.compose.runtime.Composable
import com.berryos.music.core.designsystem.PlaceholderScreen

@Composable
fun FolderScreen(folderId: String) {
    PlaceholderScreen(title = "Folder", subtitle = "Folder id: $folderId")
}

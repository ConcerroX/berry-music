package com.berryos.music.feature.folder

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.PlaceholderScreen

@Composable
fun FolderScreen(folderId: String, actions: AppActions) {
    PlaceholderScreen(title = "Folder", subtitle = "Folder id: $folderId") {
        TextButton(onClick = actions.back) { Text("Back") }
    }
}

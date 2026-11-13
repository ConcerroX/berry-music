package com.berryos.music.feature.library

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.ArtistRoute
import com.berryos.music.app.navigation.FolderRoute
import com.berryos.music.app.navigation.PlaylistRoute
import com.berryos.music.core.designsystem.PlaceholderScreen

@Composable
fun LibraryScreen(actions: AppActions) {
    PlaceholderScreen(title = "Library", subtitle = "Songs, albums, artists, playlists, and folders.") {
        TextButton(onClick = { actions.open(AlbumRoute("demo-album")) }) { Text("Album detail") }
        TextButton(onClick = { actions.open(ArtistRoute("demo-artist")) }) { Text("Artist detail") }
        TextButton(onClick = { actions.open(PlaylistRoute("demo-playlist")) }) { Text("Playlist detail") }
        TextButton(onClick = { actions.open(FolderRoute("demo-folder")) }) { Text("Folder detail") }
    }
}

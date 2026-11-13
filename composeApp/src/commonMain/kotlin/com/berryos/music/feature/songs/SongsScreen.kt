package com.berryos.music.feature.songs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.core.designsystem.LibraryLoading
import com.berryos.music.core.designsystem.LibraryMessage
import com.berryos.music.core.designsystem.formatDuration
import com.berryos.music.core.library.LibraryUiState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SongsScreen(state: LibraryUiState, actions: AppActions, onRefresh: () -> Unit) {
    when (state) {
        LibraryUiState.Loading -> LibraryLoading()

        is LibraryUiState.Error -> LibraryMessage("Couldn't load music", state.message, onRefresh)

        is LibraryUiState.Ready -> {
            val tracks = state.snapshot.tracks
            if (tracks.isEmpty()) {
                LibraryMessage(
                    title = "No music found",
                    message = "Grant audio permission or add music files to the device.",
                    onRefresh = onRefresh,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(tracks) { index, track ->
                        ListItem(
                            modifier = Modifier.clickable { actions.play(tracks, index) },
                            text = { Text(track.title) },
                            secondaryText = { Text("${track.artist} · ${track.album}") },
                            trailing = { Text(formatDuration(track.durationMs)) },
                        )
                    }
                }
            }
        }
    }
}

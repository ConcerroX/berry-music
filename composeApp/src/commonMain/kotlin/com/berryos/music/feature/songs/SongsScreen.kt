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
import com.berryos.music.resources.Res
import com.berryos.music.resources.could_not_load_music
import com.berryos.music.resources.no_music_found
import com.berryos.music.resources.no_music_message
import com.berryos.music.resources.track_summary
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SongsScreen(state: LibraryUiState, actions: AppActions) {
    when (state) {
        LibraryUiState.Loading -> LibraryLoading()

        is LibraryUiState.Error -> LibraryMessage(
            stringResource(Res.string.could_not_load_music),
            state.message,
        )

        is LibraryUiState.Ready -> {
            val tracks = state.snapshot.tracks
            if (tracks.isEmpty()) {
                LibraryMessage(
                    title = stringResource(Res.string.no_music_found),
                    message = stringResource(Res.string.no_music_message),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(tracks) { index, track ->
                        ListItem(
                            modifier = Modifier.clickable { actions.play(tracks, index) },
                            text = { Text(track.title) },
                            secondaryText = {
                                Text(stringResource(Res.string.track_summary, track.artist, track.album))
                            },
                            trailing = { Text(formatDuration(track.durationMs)) },
                        )
                    }
                }
            }
        }
    }
}

package com.berryos.music.feature.home

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.SearchRoute
import com.berryos.music.app.navigation.SettingsRoute
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.resources.Res
import com.berryos.music.resources.library_summary
import com.berryos.music.resources.loading_local_library
import com.berryos.music.resources.search
import com.berryos.music.resources.settings
import com.berryos.music.resources.tab_home
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(actions: AppActions, state: LibraryUiState) {
    PlaceholderScreen(
        title = stringResource(Res.string.tab_home),
        subtitle = when (state) {
            LibraryUiState.Loading -> stringResource(Res.string.loading_local_library)

            is LibraryUiState.Error -> state.message

            is LibraryUiState.Ready -> stringResource(
                Res.string.library_summary,
                state.snapshot.tracks.size,
                state.snapshot.albums.size,
                state.snapshot.artists.size,
            )
        },
    ) {
        TextButton(onClick = { actions.open(SearchRoute) }) { Text(stringResource(Res.string.search)) }
        TextButton(onClick = { actions.open(SettingsRoute) }) { Text(stringResource(Res.string.settings)) }
    }
}

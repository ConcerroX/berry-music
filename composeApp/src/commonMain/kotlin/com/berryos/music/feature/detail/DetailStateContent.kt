package com.berryos.music.feature.detail

import androidx.compose.runtime.Composable
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.model.LibrarySnapshot
import com.berryos.music.resources.Res
import com.berryos.music.resources.loading_library
import org.jetbrains.compose.resources.stringResource

/**
 * Loading/error/ready shell shared by the album and artist detail pages.
 *
 * The caller owns what a missing object means, so the ready branch receives the snapshot.
 */
@Composable
internal fun DetailStateContent(title: String, state: LibraryUiState, content: @Composable (LibrarySnapshot) -> Unit) {
    when (state) {
        LibraryUiState.Loading -> PlaceholderScreen(title, stringResource(Res.string.loading_library))
        is LibraryUiState.Error -> PlaceholderScreen(title, state.message)
        is LibraryUiState.Ready -> content(state.snapshot)
    }
}

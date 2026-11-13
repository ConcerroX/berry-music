package com.berryos.music.core.library

import com.berryos.music.core.model.LibrarySnapshot

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Ready(val snapshot: LibrarySnapshot) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

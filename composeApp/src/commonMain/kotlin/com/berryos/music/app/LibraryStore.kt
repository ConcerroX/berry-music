package com.berryos.music.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.berryos.music.core.library.LibraryRepository
import com.berryos.music.core.library.LibraryUiState

class LibraryStore(private val repository: LibraryRepository) {
    var state by mutableStateOf<LibraryUiState>(LibraryUiState.Loading)
        private set

    suspend fun refresh() {
        state = LibraryUiState.Loading
        state = runCatching { repository.load() }.fold(
            onSuccess = { LibraryUiState.Ready(it) },
            onFailure = { LibraryUiState.Error(it.message ?: "Failed to load library") },
        )
    }
}

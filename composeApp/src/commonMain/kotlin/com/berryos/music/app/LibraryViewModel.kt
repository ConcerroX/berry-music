package com.berryos.music.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berryos.music.core.library.LibraryRepository
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.resources.Res
import com.berryos.music.resources.failed_to_load_library
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class LibraryViewModel(private val repository: LibraryRepository, libraryChanges: Flow<Unit>) : ViewModel() {
    private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            refresh()
            libraryChanges.collect { refresh() }
        }
    }

    private suspend fun refresh() {
        if (_state.value !is LibraryUiState.Ready) {
            _state.value = LibraryUiState.Loading
        }
        _state.value = runCatching { repository.load() }.fold(
            onSuccess = { LibraryUiState.Ready(it) },
            onFailure = { LibraryUiState.Error(it.message ?: getString(Res.string.failed_to_load_library)) },
        )
    }
}

package com.berryos.music.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berryos.music.core.metadata.TrackTagInspection
import com.berryos.music.core.metadata.TrackTagRepository
import com.berryos.music.core.model.SourceKey
import com.berryos.music.core.model.Track
import com.berryos.music.resources.Res
import com.berryos.music.resources.metadata_failed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

sealed interface TrackTagUiState {
    data object Loading : TrackTagUiState
    data object Missing : TrackTagUiState
    data object NotLocal : TrackTagUiState
    data class Ready(val inspection: TrackTagInspection) : TrackTagUiState
    data class Error(val message: String) : TrackTagUiState
}

class TrackTagViewModel(private val repository: TrackTagRepository) : ViewModel() {
    private val _state = MutableStateFlow<TrackTagUiState>(TrackTagUiState.Loading)
    val state: StateFlow<TrackTagUiState> = _state.asStateFlow()

    private var openedTrack: Track? = null

    fun open(track: Track?) {
        when {
            track == null -> {
                openedTrack = null
                _state.value = TrackTagUiState.Missing
            }

            track == openedTrack && _state.value is TrackTagUiState.Ready -> Unit

            track.source != SourceKey.Local -> {
                openedTrack = track
                _state.value = TrackTagUiState.NotLocal
            }

            else -> {
                openedTrack = track
                viewModelScope.launch {
                    _state.value = TrackTagUiState.Loading
                    _state.value = runCatching { repository.inspect(track) }.fold(
                        onSuccess = { TrackTagUiState.Ready(it) },
                        onFailure = { TrackTagUiState.Error(it.message ?: getString(Res.string.metadata_failed)) },
                    )
                }
            }
        }
    }
}

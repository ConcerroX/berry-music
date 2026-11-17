package com.berryos.music.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berryos.music.core.metadata.TrackTagField
import com.berryos.music.core.metadata.TrackTagInspection
import com.berryos.music.core.metadata.TrackTagRepository
import com.berryos.music.core.model.SourceKey
import com.berryos.music.core.model.Track
import com.berryos.music.resources.Res
import com.berryos.music.resources.metadata_failed
import com.berryos.music.resources.metadata_save_failed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

sealed interface TrackTagUiState {
    data object Loading : TrackTagUiState
    data object Missing : TrackTagUiState
    data object NotLocal : TrackTagUiState
    data class Ready(
        val inspection: TrackTagInspection,
        val originalEdits: Map<TrackTagField, String> = inspection.initialEdits(),
        val edits: Map<TrackTagField, String> = originalEdits,
        val dirty: Boolean = false,
        val saving: Boolean = false,
        val error: String? = null,
    ) : TrackTagUiState

    data class Error(val message: String) : TrackTagUiState
}

private fun TrackTagInspection.initialEdits(): Map<TrackTagField, String> = buildMap {
    val grouped = mutableMapOf<TrackTagField, MutableList<String>>()
    this@initialEdits.entries.forEach { entry ->
        val field = entry.field
        if (field != null && entry.picture == null) {
            grouped.getOrPut(field) { mutableListOf() } += entry.value.orEmpty()
        }
    }
    grouped.forEach { (field, values) -> put(field, values.joinToString(" | ")) }
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

    fun edit(field: TrackTagField, value: String) {
        val current = _state.value as? TrackTagUiState.Ready ?: return
        if (current.edits[field] == value) return
        val edits = current.edits + (field to value)
        _state.value = current.copy(
            edits = edits,
            dirty = edits != current.originalEdits,
            error = null,
        )
    }

    fun save() {
        val current = _state.value as? TrackTagUiState.Ready ?: return
        val track = openedTrack
        if (track == null || !current.dirty || current.saving) return
        _state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            _state.value = runCatching {
                repository.save(track, current.edits)
                repository.inspect(track)
            }.fold(
                onSuccess = { TrackTagUiState.Ready(it, dirty = false) },
                onFailure = {
                    current.copy(saving = false, error = it.message ?: getString(Res.string.metadata_save_failed))
                },
            )
        }
    }
}

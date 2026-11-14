package com.berryos.music.core.library

import com.berryos.music.core.model.LibrarySnapshot
import com.berryos.music.core.model.SourceKey

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Ready(val snapshot: LibrarySnapshot) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

fun LibraryUiState.filtered(localOnly: Boolean): LibraryUiState = when (this) {
    is LibraryUiState.Ready -> LibraryUiState.Ready(snapshot.filtered(localOnly))
    else -> this
}

private fun LibrarySnapshot.filtered(localOnly: Boolean): LibrarySnapshot {
    fun matches(source: SourceKey) = (source == SourceKey.Local) == localOnly
    return LibrarySnapshot(
        tracks = tracks.filter { matches(it.source) },
        albums = albums.filter { matches(it.source) },
        artists = artists.filter { matches(it.source) },
    )
}

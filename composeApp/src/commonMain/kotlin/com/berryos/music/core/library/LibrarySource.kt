package com.berryos.music.core.library

import com.berryos.music.core.model.LibrarySnapshot

interface LibrarySource {
    suspend fun load(): LibrarySnapshot
}

class LibraryRepository(private val sources: List<LibrarySource>) {
    suspend fun load(): LibrarySnapshot {
        val snapshots = sources.map { it.load() }
        return LibrarySnapshot(
            tracks = snapshots.flatMap { it.tracks }.sortedBy { it.title.lowercase() },
            albums = snapshots.flatMap { it.albums }.sortedBy { it.title.lowercase() },
            artists = snapshots.flatMap { it.artists }.sortedBy { it.name.lowercase() },
            people = snapshots.flatMap { it.people }.distinct().sortedBy { it.lowercase() },
        )
    }
}

package com.berryos.music.core.library

import com.berryos.music.core.model.LibrarySnapshot

class LocalLibrarySource(private val sync: LocalLibrarySync) : LibrarySource {
    override suspend fun load(): LibrarySnapshot {
        sync.refresh()
        return sync.snapshot()
    }
}

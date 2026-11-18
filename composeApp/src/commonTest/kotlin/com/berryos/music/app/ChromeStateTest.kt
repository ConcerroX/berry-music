package com.berryos.music.app

import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AlbumsRoute
import com.berryos.music.core.designsystem.PageTopBarStates
import com.berryos.music.core.model.SourceKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChromeStateTest {
    @Test
    fun pageScrollabilityIsScopedByOwner() {
        val registry = PageTopBarStates()
        val albums = registry.stateFor("albums")
        val album = registry.stateFor("album")

        albums.isScrollable.value = false

        assertFalse(albums.isScrollable.value)
        assertTrue(album.isScrollable.value)
    }

    @Test
    fun retainingRoutesDropsInactivePageState() {
        val registry = PageTopBarStates()
        registry.stateFor("album").isScrollable.value = false

        registry.retainAll(setOf("albums"))

        assertTrue(registry.stateFor("album").isScrollable.value)
    }

    @Test
    fun offsetsForPoppedRoutesAreRemoved() {
        val album = AlbumRoute(SourceKey.Local, "album")
        val registry = PageTopBarStates()
        registry.stateFor(AlbumsRoute).updateTopBarOffset(0f)
        registry.stateFor(album).updateTopBarOffset(356f)

        registry.retainAll(setOf(AlbumsRoute))

        assertEquals(0f, registry.stateFor(album).topBarOffset)
        assertFalse(registry.stateFor(album).hasTopBarOffset)
    }

    @Test
    fun offsetsForActiveRoutesAreKept() {
        val album = AlbumRoute(SourceKey.Local, "album")
        val registry = PageTopBarStates()
        registry.stateFor(AlbumsRoute).updateTopBarOffset(0f)
        registry.stateFor(album).updateTopBarOffset(356f)

        registry.retainAll(setOf(AlbumsRoute, album))

        assertEquals(356f, registry.stateFor(album).topBarOffset)
        assertTrue(registry.stateFor(album).hasTopBarOffset)
    }
}

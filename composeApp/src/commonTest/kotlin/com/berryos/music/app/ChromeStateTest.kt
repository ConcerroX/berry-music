package com.berryos.music.app

import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AlbumsRoute
import com.berryos.music.core.designsystem.PageChromeRegistry
import com.berryos.music.core.model.SourceKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChromeStateTest {
    @Test
    fun pageScrollabilityIsScopedByOwner() {
        val registry = PageChromeRegistry()
        val albums = registry.state("albums")
        val album = registry.state("album")

        albums.scrollable.value = false

        assertFalse(albums.scrollable.value)
        assertTrue(album.scrollable.value)
    }

    @Test
    fun appBarActionsAreScopedByOwner() {
        val registry = PageChromeRegistry()
        val albums = registry.state("albums")
        val album = registry.state("album")

        album.actions.tabLabels = listOf("Metadata", "Lyrics")
        album.actions.saveEnabled = true

        assertTrue(albums.actions.tabLabels.isEmpty())
        assertFalse(albums.actions.saveEnabled)
    }

    @Test
    fun retainingRoutesDropsInactivePageState() {
        val registry = PageChromeRegistry()
        registry.state("album").scrollable.value = false
        registry.state("album").actions.tabLabels = listOf("Metadata", "Lyrics")

        registry.retain(setOf("albums"))

        assertTrue(registry.state("album").scrollable.value)
        assertTrue(registry.state("album").actions.tabLabels.isEmpty())
    }

    @Test
    fun offsetsForPoppedRoutesAreRemoved() {
        val album = AlbumRoute(SourceKey.Local, "album")
        val registry = PageChromeRegistry()
        registry.state(AlbumsRoute).updateTopBarOffset(0f)
        registry.state(album).updateTopBarOffset(356f)

        registry.retain(setOf(AlbumsRoute))

        assertEquals(0f, registry.state(album).topBarOffset)
        assertFalse(registry.state(album).hasTopBarOffset)
    }

    @Test
    fun offsetsForActiveRoutesAreKept() {
        val album = AlbumRoute(SourceKey.Local, "album")
        val registry = PageChromeRegistry()
        registry.state(AlbumsRoute).updateTopBarOffset(0f)
        registry.state(album).updateTopBarOffset(356f)

        registry.retain(setOf(AlbumsRoute, album))

        assertEquals(356f, registry.state(album).topBarOffset)
        assertTrue(registry.state(album).hasTopBarOffset)
    }
}

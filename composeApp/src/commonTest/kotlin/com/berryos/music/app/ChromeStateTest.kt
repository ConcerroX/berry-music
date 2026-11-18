package com.berryos.music.app

import art.rippledesign.compose.rui.PageScrollStateRegistry
import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AlbumsRoute
import com.berryos.music.core.designsystem.AppBarActionRegistry
import com.berryos.music.core.model.SourceKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChromeStateTest {
    @Test
    fun pageScrollabilityIsScopedByOwner() {
        val registry = PageScrollStateRegistry()
        val albums = registry.state("albums")
        val album = registry.state("album")

        albums.value = false

        assertFalse(albums.value)
        assertTrue(album.value)
    }

    @Test
    fun appBarActionsAreScopedByOwner() {
        val registry = AppBarActionRegistry()
        val albums = registry.state("albums")
        val album = registry.state("album")

        album.tabLabels = listOf("Metadata", "Lyrics")
        album.saveEnabled = true

        assertTrue(albums.tabLabels.isEmpty())
        assertFalse(albums.saveEnabled)
    }

    @Test
    fun retainingRoutesDropsInactivePageActions() {
        val registry = AppBarActionRegistry()
        registry.state("album").tabLabels = listOf("Metadata", "Lyrics")

        registry.retain(setOf("albums"))

        assertTrue(registry.state("album").tabLabels.isEmpty())
    }

    @Test
    fun retainingRoutesDropsInactivePages() {
        val registry = PageScrollStateRegistry()
        registry.state("album").value = false

        registry.retain(setOf("albums"))

        assertTrue(registry.state("album").value)
    }

    @Test
    fun offsetsForPoppedRoutesAreRemoved() {
        val album = AlbumRoute(SourceKey.Local, "album")
        val offsets = mutableMapOf<Any, Float>(
            AlbumsRoute to 0f,
            album to 356f,
        )

        offsets.retainActiveRoutes(setOf(AlbumsRoute))

        assertEquals(setOf<Any>(AlbumsRoute), offsets.keys)
    }

    @Test
    fun offsetsForActiveRoutesAreKept() {
        val album = AlbumRoute(SourceKey.Local, "album")
        val offsets = mutableMapOf<Any, Float>(
            AlbumsRoute to 0f,
            album to 356f,
        )

        offsets.retainActiveRoutes(setOf(AlbumsRoute, album))

        assertEquals(setOf<Any>(AlbumsRoute, album), offsets.keys)
    }
}

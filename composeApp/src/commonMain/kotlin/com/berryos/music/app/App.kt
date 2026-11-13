package com.berryos.music.app

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.berryos.music.app.navigation.AboutRoute
import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.AppearanceRoute
import com.berryos.music.app.navigation.ArtistRoute
import com.berryos.music.app.navigation.FolderRoute
import com.berryos.music.app.navigation.HomeRoute
import com.berryos.music.app.navigation.LibraryRoute
import com.berryos.music.app.navigation.LibrarySettingsRoute
import com.berryos.music.app.navigation.PlaybackSettingsRoute
import com.berryos.music.app.navigation.PlaylistRoute
import com.berryos.music.app.navigation.SearchRoute
import com.berryos.music.app.navigation.ServerEditRoute
import com.berryos.music.app.navigation.ServersRoute
import com.berryos.music.app.navigation.SettingsRoute
import com.berryos.music.app.navigation.appRouteSerialization
import com.berryos.music.core.designsystem.BerryTheme
import com.berryos.music.feature.album.AlbumScreen
import com.berryos.music.feature.artist.ArtistScreen
import com.berryos.music.feature.folder.FolderScreen
import com.berryos.music.feature.home.HomeScreen
import com.berryos.music.feature.library.LibraryScreen
import com.berryos.music.feature.playlist.PlaylistScreen
import com.berryos.music.feature.search.SearchScreen
import com.berryos.music.feature.settings.AboutScreen
import com.berryos.music.feature.settings.AppearanceScreen
import com.berryos.music.feature.settings.LibrarySettingsScreen
import com.berryos.music.feature.settings.PlaybackSettingsScreen
import com.berryos.music.feature.settings.ServerEditScreen
import com.berryos.music.feature.settings.ServersScreen
import com.berryos.music.feature.settings.SettingsScreen

@Composable
fun App() {
    BerryTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppShell()
        }
    }
}

private data class TabItem(val label: String, val backStack: NavBackStack<NavKey>)

@Composable
private fun AppShell() {
    val homeBackStack = rememberNavBackStack(appRouteSerialization, HomeRoute)
    val libraryBackStack = rememberNavBackStack(appRouteSerialization, LibraryRoute)
    val searchBackStack = rememberNavBackStack(appRouteSerialization, SearchRoute)
    val settingsBackStack = rememberNavBackStack(appRouteSerialization, SettingsRoute)

    val tabs = listOf(
        TabItem("Home", homeBackStack),
        TabItem("Library", libraryBackStack),
        TabItem("Search", searchBackStack),
        TabItem("Settings", settingsBackStack),
    )
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = tabs[selectedTabIndex]

    fun navigateBack() {
        if (selectedTab.backStack.size > 1) {
            selectedTab.backStack.removeLastOrNull()
        } else if (selectedTabIndex != 0) {
            selectedTabIndex = 0
        }
    }

    val actions = AppActions(
        open = { route -> selectedTab.backStack.add(route) },
        back = ::navigateBack,
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        if (compact) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppNavDisplay(selectedTab.backStack, actions, Modifier.weight(1f))
                if (selectedTab.backStack.size == 1) {
                    BottomNavigation {
                        tabs.forEachIndexed { index, tab ->
                            BottomNavigationItem(
                                selected = index == selectedTabIndex,
                                onClick = { selectedTabIndex = index },
                                icon = { Text(tab.label.take(1)) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    tabs.forEachIndexed { index, tab ->
                        NavigationRailItem(
                            selected = index == selectedTabIndex,
                            onClick = { selectedTabIndex = index },
                            icon = { Text(tab.label.take(1)) },
                            label = { Text(tab.label) },
                        )
                    }
                }
                AppNavDisplay(selectedTab.backStack, actions, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AppNavDisplay(backStack: NavBackStack<NavKey>, actions: AppActions, modifier: Modifier = Modifier) {
    NavDisplay(
        backStack = backStack,
        onBack = actions.back,
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeRoute> { HomeScreen(actions) }
            entry<LibraryRoute> { LibraryScreen(actions) }
            entry<SearchRoute> { SearchScreen(actions) }
            entry<SettingsRoute> { SettingsScreen(actions) }
            entry<AlbumRoute> { route -> AlbumScreen(route.albumId, actions) }
            entry<ArtistRoute> { route -> ArtistScreen(route.artistId, actions) }
            entry<PlaylistRoute> { route -> PlaylistScreen(route.playlistId, actions) }
            entry<FolderRoute> { route -> FolderScreen(route.folderId, actions) }
            entry<AppearanceRoute> { AppearanceScreen(actions) }
            entry<PlaybackSettingsRoute> { PlaybackSettingsScreen(actions) }
            entry<LibrarySettingsRoute> { LibrarySettingsScreen(actions) }
            entry<ServersRoute> { ServersScreen(actions) }
            entry<ServerEditRoute> { route -> ServerEditScreen(route.serverId, actions) }
            entry<AboutRoute> { AboutScreen(actions) }
        },
    )
}

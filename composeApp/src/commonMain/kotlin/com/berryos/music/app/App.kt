package com.berryos.music.app

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailDefaults
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import art.rippledesign.compose.rui.RBottomNavigation
import art.rippledesign.compose.rui.RBottomNavigationItem
import art.rippledesign.compose.rui.RTopAppBar
import com.berryos.music.app.navigation.AboutRoute
import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AlbumsRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.AppearanceRoute
import com.berryos.music.app.navigation.ArtistRoute
import com.berryos.music.app.navigation.ArtistsRoute
import com.berryos.music.app.navigation.FolderRoute
import com.berryos.music.app.navigation.HomeRoute
import com.berryos.music.app.navigation.LibrarySettingsRoute
import com.berryos.music.app.navigation.PlaybackSettingsRoute
import com.berryos.music.app.navigation.PlaylistRoute
import com.berryos.music.app.navigation.PlaylistsRoute
import com.berryos.music.app.navigation.SearchRoute
import com.berryos.music.app.navigation.ServerEditRoute
import com.berryos.music.app.navigation.ServersRoute
import com.berryos.music.app.navigation.SettingsRoute
import com.berryos.music.app.navigation.SongsRoute
import com.berryos.music.app.navigation.appRouteSerialization
import com.berryos.music.core.designsystem.BerryTheme
import com.berryos.music.core.library.LibraryRepository
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.playback.PlaybackController
import com.berryos.music.core.playback.PlaybackState
import com.berryos.music.feature.album.AlbumScreen
import com.berryos.music.feature.albums.AlbumsScreen
import com.berryos.music.feature.artist.ArtistScreen
import com.berryos.music.feature.artists.ArtistsScreen
import com.berryos.music.feature.folder.FolderScreen
import com.berryos.music.feature.home.HomeScreen
import com.berryos.music.feature.nowplaying.MiniPlayer
import com.berryos.music.feature.nowplaying.PlayingSheet
import com.berryos.music.feature.playlist.PlaylistScreen
import com.berryos.music.feature.playlists.PlaylistsScreen
import com.berryos.music.feature.search.SearchScreen
import com.berryos.music.feature.settings.AboutScreen
import com.berryos.music.feature.settings.AppearanceScreen
import com.berryos.music.feature.settings.LibrarySettingsScreen
import com.berryos.music.feature.settings.PlaybackSettingsScreen
import com.berryos.music.feature.settings.ServerEditScreen
import com.berryos.music.feature.settings.ServersScreen
import com.berryos.music.feature.settings.SettingsScreen
import com.berryos.music.feature.songs.SongsScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.ic_arrow_back
import com.berryos.music.resources.ic_filled_album
import com.berryos.music.resources.ic_filled_home
import com.berryos.music.resources.ic_filled_music_note
import com.berryos.music.resources.ic_filled_people
import com.berryos.music.resources.ic_filled_queue_music
import com.berryos.music.resources.ic_outlined_album
import com.berryos.music.resources.ic_outlined_home
import com.berryos.music.resources.ic_outlined_music_note
import com.berryos.music.resources.ic_outlined_people
import com.berryos.music.resources.ic_outlined_queue_music
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun App(libraryRepository: LibraryRepository, playback: PlaybackController) {
    BerryTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val libraryStore = remember(libraryRepository) { LibraryStore(libraryRepository) }
            val scope = rememberCoroutineScope()
            val refresh: () -> Unit = { scope.launch { libraryStore.refresh() } }
            val playbackState by playback.state.collectAsState()
            val sheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden,
                skipHalfExpanded = true,
            )
            val openPlayer: () -> Unit = { scope.launch { sheetState.show() } }
            LaunchedEffect(libraryStore) { libraryStore.refresh() }
            ModalBottomSheetLayout(
                sheetState = sheetState,
                sheetContent = {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        PlayingSheet(
                            state = playbackState,
                            onTogglePlayPause = playback::togglePlayPause,
                            onNext = playback::next,
                            onPrevious = playback::previous,
                            onPlayQueueItem = { index -> playback.play(playbackState.queue, index) },
                            onDismiss = { scope.launch { sheetState.hide() } },
                        )
                    }
                },
            ) {
                AppShell(libraryStore.state, refresh, playback, playbackState, openPlayer)
            }
        }
    }
}

private data class TabItem(
    val label: String,
    val filledIcon: DrawableResource,
    val outlinedIcon: DrawableResource,
    val backStack: NavBackStack<NavKey>,
)

@Composable
private fun AppShell(
    libraryState: LibraryUiState,
    onRefresh: () -> Unit,
    playback: PlaybackController,
    playbackState: PlaybackState,
    onOpenPlayer: () -> Unit,
) {
    val homeBackStack = rememberNavBackStack(appRouteSerialization, HomeRoute)
    val albumsBackStack = rememberNavBackStack(appRouteSerialization, AlbumsRoute)
    val songsBackStack = rememberNavBackStack(appRouteSerialization, SongsRoute)
    val playlistsBackStack = rememberNavBackStack(appRouteSerialization, PlaylistsRoute)
    val artistsBackStack = rememberNavBackStack(appRouteSerialization, ArtistsRoute)
    val tabs = createTabs(homeBackStack, albumsBackStack, songsBackStack, playlistsBackStack, artistsBackStack)
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = tabs[selectedTabIndex]
    val actions = AppActions(
        open = { route -> selectedTab.backStack.add(route) },
        back = {
            if (selectedTab.backStack.size > 1) {
                selectedTab.backStack.removeLastOrNull()
            } else if (selectedTabIndex != 0) {
                selectedTabIndex = 0
            }
        },
        play = playback::play,
        togglePlayPause = playback::togglePlayPause,
        next = playback::next,
    )
    AppChrome(tabs, selectedTabIndex, actions, libraryState, onRefresh, playbackState, onOpenPlayer) { index ->
        selectedTabIndex = index
    }
}

private fun createTabs(
    home: NavBackStack<NavKey>,
    albums: NavBackStack<NavKey>,
    songs: NavBackStack<NavKey>,
    playlists: NavBackStack<NavKey>,
    artists: NavBackStack<NavKey>,
): List<TabItem> = listOf(
    TabItem("Home", Res.drawable.ic_filled_home, Res.drawable.ic_outlined_home, home),
    TabItem("Albums", Res.drawable.ic_filled_album, Res.drawable.ic_outlined_album, albums),
    TabItem("Songs", Res.drawable.ic_filled_music_note, Res.drawable.ic_outlined_music_note, songs),
    TabItem("Playlists", Res.drawable.ic_filled_queue_music, Res.drawable.ic_outlined_queue_music, playlists),
    TabItem("Artists", Res.drawable.ic_filled_people, Res.drawable.ic_outlined_people, artists),
)

@Composable
private fun AppChrome(
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    actions: AppActions,
    libraryState: LibraryUiState,
    onRefresh: () -> Unit,
    playbackState: PlaybackState,
    onOpenPlayer: () -> Unit,
    onSelectTab: (Int) -> Unit,
) {
    val selectedTab = tabs[selectedTabIndex]
    val isRoot = selectedTab.backStack.size == 1
    val title = routeTitle(selectedTab.backStack.lastOrNull())
    val onBack = if (isRoot) null else actions.back
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        if (compact) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppTopBar(title, onRefresh.takeIf { isRoot }, onBack)
                AppNavDisplay(selectedTab.backStack, actions, libraryState, onRefresh, Modifier.weight(1f))
                if (playbackState.currentTrack != null) {
                    MiniPlayer(playbackState, onOpenPlayer, actions.togglePlayPause, actions.next)
                }
                if (isRoot) {
                    RBottomNavigation(itemCount = tabs.size) {
                        tabs.forEachIndexed { index, tab ->
                            RBottomNavigationItem(
                                selected = index == selectedTabIndex,
                                onClick = { onSelectTab(index) },
                                icon = { TabIcon(tab, index == selectedTabIndex) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                if (isRoot) {
                    NavigationRail(windowInsets = NavigationRailDefaults.windowInsets) {
                        tabs.forEachIndexed { index, tab ->
                            NavigationRailItem(
                                selected = index == selectedTabIndex,
                                onClick = { onSelectTab(index) },
                                icon = { TabIcon(tab, index == selectedTabIndex) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    AppTopBar(title, onRefresh.takeIf { isRoot }, onBack)
                    AppNavDisplay(selectedTab.backStack, actions, libraryState, onRefresh, Modifier.weight(1f))
                    if (playbackState.currentTrack != null) {
                        MiniPlayer(playbackState, onOpenPlayer, actions.togglePlayPause, actions.next)
                    }
                }
            }
        }
    }
}

private val staticRouteTitles: Map<NavKey, String> = mapOf(
    HomeRoute to "Home",
    AlbumsRoute to "Albums",
    SongsRoute to "Songs",
    PlaylistsRoute to "Playlists",
    ArtistsRoute to "Artists",
    SearchRoute to "Search",
    SettingsRoute to "Settings",
    AppearanceRoute to "Appearance",
    PlaybackSettingsRoute to "Playback",
    LibrarySettingsRoute to "Library",
    ServersRoute to "Servers",
    AboutRoute to "About",
)

private fun routeTitle(route: NavKey?): String {
    if (route == null) return "Berry Music"
    return when (route) {
        is AlbumRoute -> "Album"
        is ArtistRoute -> "Artist"
        is PlaylistRoute -> "Playlist"
        is FolderRoute -> "Folder"
        is ServerEditRoute -> if (route.serverId == null) "Add server" else "Server"
        else -> staticRouteTitles[route] ?: "Berry Music"
    }
}

@Composable
private fun AppTopBar(title: String, onRefresh: (() -> Unit)?, onBack: (() -> Unit)?) {
    RTopAppBar(
        title = { Text(title) },
        navigationIcon = onBack?.let { back ->
            {
                IconButton(onClick = back) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = "Back",
                    )
                }
            }
        },
        actions = {
            onRefresh?.let { refresh ->
                TextButton(
                    onClick = refresh,
                    colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
                ) {
                    Text("Refresh")
                }
            }
        },
    )
}

@Composable
private fun TabIcon(tab: TabItem, selected: Boolean) {
    Icon(
        painter = painterResource(if (selected) tab.filledIcon else tab.outlinedIcon),
        contentDescription = tab.label,
    )
}

@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    actions: AppActions,
    libraryState: LibraryUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = backStack,
        onBack = actions.back,
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeRoute> { HomeScreen(actions, libraryState) }
            entry<AlbumsRoute> { AlbumsScreen(libraryState, actions, onRefresh) }
            entry<SongsRoute> { SongsScreen(libraryState, actions, onRefresh) }
            entry<PlaylistsRoute> { PlaylistsScreen(actions) }
            entry<ArtistsRoute> { ArtistsScreen(libraryState, onRefresh) }
            entry<SearchRoute> { SearchScreen(actions) }
            entry<SettingsRoute> { SettingsScreen(actions) }
            entry<AlbumRoute> { route -> AlbumScreen(route.albumId, libraryState, actions) }
            entry<ArtistRoute> { route -> ArtistScreen(route.artistId, actions) }
            entry<PlaylistRoute> { route -> PlaylistScreen(route.playlistId) }
            entry<FolderRoute> { route -> FolderScreen(route.folderId) }
            entry<AppearanceRoute> { AppearanceScreen() }
            entry<PlaybackSettingsRoute> { PlaybackSettingsScreen() }
            entry<LibrarySettingsRoute> { LibrarySettingsScreen() }
            entry<ServersRoute> { ServersScreen(actions) }
            entry<ServerEditRoute> { route -> ServerEditScreen(route.serverId) }
            entry<AboutRoute> { AboutScreen() }
        },
    )
}

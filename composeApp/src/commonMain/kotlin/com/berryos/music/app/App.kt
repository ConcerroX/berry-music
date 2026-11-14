package com.berryos.music.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailDefaults
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
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
import androidx.compose.ui.graphics.Color
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
import art.rippledesign.compose.rui.fadeThrough
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
import com.berryos.music.core.auth.AuthController
import com.berryos.music.core.auth.AuthState
import com.berryos.music.core.designsystem.BerryTheme
import com.berryos.music.core.designsystem.cachedCoverColors
import com.berryos.music.core.designsystem.rememberDefaultCoverColors
import com.berryos.music.core.library.LibraryRepository
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.library.filtered
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
import com.berryos.music.resources.about
import com.berryos.music.resources.add_server
import com.berryos.music.resources.album
import com.berryos.music.resources.app_name
import com.berryos.music.resources.appearance
import com.berryos.music.resources.artist
import com.berryos.music.resources.back
import com.berryos.music.resources.folder
import com.berryos.music.resources.ic_arrow_back
import com.berryos.music.resources.ic_filled_album
import com.berryos.music.resources.ic_filled_home
import com.berryos.music.resources.ic_filled_music_note
import com.berryos.music.resources.ic_filled_people
import com.berryos.music.resources.ic_filled_queue_music
import com.berryos.music.resources.ic_menu
import com.berryos.music.resources.ic_more_vert
import com.berryos.music.resources.ic_outlined_album
import com.berryos.music.resources.ic_outlined_home
import com.berryos.music.resources.ic_outlined_music_note
import com.berryos.music.resources.ic_outlined_people
import com.berryos.music.resources.ic_outlined_queue_music
import com.berryos.music.resources.library
import com.berryos.music.resources.menu
import com.berryos.music.resources.more_options
import com.berryos.music.resources.playback
import com.berryos.music.resources.playlist
import com.berryos.music.resources.search
import com.berryos.music.resources.server
import com.berryos.music.resources.servers
import com.berryos.music.resources.settings
import com.berryos.music.resources.tab_albums
import com.berryos.music.resources.tab_artists
import com.berryos.music.resources.tab_home
import com.berryos.music.resources.tab_playlists
import com.berryos.music.resources.tab_songs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun App(
    libraryRepository: LibraryRepository,
    playback: PlaybackController,
    auth: AuthController,
    libraryChanges: Flow<Unit>,
) {
    BerryTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val libraryStore = remember(libraryRepository) { LibraryStore(libraryRepository) }
            val scope = rememberCoroutineScope()
            val playbackState by playback.state.collectAsState()
            val authState by auth.state.collectAsState()
            val sheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden,
                skipHalfExpanded = true,
            )
            val openPlayer: () -> Unit = { scope.launch { sheetState.show() } }
            LaunchedEffect(libraryStore, libraryChanges) {
                libraryStore.refresh()
                launch { libraryChanges.collect { libraryStore.refresh() } }
            }
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
                AppShell(
                    libraryStore.state.filtered(authState.showingLocal),
                    playback,
                    playbackState,
                    authState,
                    auth,
                    openPlayer,
                )
            }
        }
    }
}

internal data class TabItem(
    val label: StringResource,
    val filledIcon: DrawableResource,
    val outlinedIcon: DrawableResource,
    val backStack: NavBackStack<NavKey>,
)

@Composable
private fun AppShell(
    libraryState: LibraryUiState,
    playback: PlaybackController,
    playbackState: PlaybackState,
    authState: AuthState,
    auth: AuthController,
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
        previous = playback::previous,
    )
    AppChrome(
        tabs,
        selectedTabIndex,
        actions,
        libraryState,
        playbackState,
        authState,
        auth,
        onOpenPlayer,
    ) { index ->
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
    TabItem(Res.string.tab_home, Res.drawable.ic_filled_home, Res.drawable.ic_outlined_home, home),
    TabItem(Res.string.tab_albums, Res.drawable.ic_filled_album, Res.drawable.ic_outlined_album, albums),
    TabItem(Res.string.tab_songs, Res.drawable.ic_filled_music_note, Res.drawable.ic_outlined_music_note, songs),
    TabItem(
        Res.string.tab_playlists,
        Res.drawable.ic_filled_queue_music,
        Res.drawable.ic_outlined_queue_music,
        playlists,
    ),
    TabItem(Res.string.tab_artists, Res.drawable.ic_filled_people, Res.drawable.ic_outlined_people, artists),
)

@Suppress("LongMethod")
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AppChrome(
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    actions: AppActions,
    libraryState: LibraryUiState,
    playbackState: PlaybackState,
    authState: AuthState,
    auth: AuthController,
    onOpenPlayer: () -> Unit,
    onSelectTab: (Int) -> Unit,
) {
    val selectedTab = tabs[selectedTabIndex]
    val isRoot = selectedTab.backStack.size == 1
    val transparentTopBar = selectedTab.backStack.lastOrNull() is AlbumRoute
    val title = routeTitle(selectedTab.backStack.lastOrNull())
    val album = (selectedTab.backStack.lastOrNull() as? AlbumRoute)?.let { route ->
        (libraryState as? LibraryUiState.Ready)?.snapshot?.albums?.firstOrNull { it.id == route.albumId }
    }
    val topBarContentColor = if (album == null) {
        null
    } else {
        val fallbackColors = rememberDefaultCoverColors()
        (album.coverUrl?.let(::cachedCoverColors) ?: fallbackColors)?.contentColor
    }
    val onBack = if (isRoot) null else actions.back
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val drawerScope = rememberCoroutineScope()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        val drawerEnabled = isRoot && compact
        val openDrawer: (() -> Unit)? = if (drawerEnabled) {
            { drawerScope.launch { scaffoldState.drawerState.open() } }
        } else {
            null
        }
        Scaffold(
            scaffoldState = scaffoldState,
            drawerGesturesEnabled = drawerEnabled,
            drawerContent = {
                AppDrawer(tabs, selectedTabIndex, actions, authState, auth, onSelectTab) {
                    drawerScope.launch { scaffoldState.drawerState.close() }
                }
            },
        ) { _ ->
            if (compact) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (transparentTopBar) {
                        Box(modifier = Modifier.weight(1f)) {
                            TabContent(tabs, selectedTabIndex, actions, libraryState, Modifier.fillMaxSize())
                            AppTopBar(
                                null,
                                onBack,
                                openDrawer,
                                transparent = true,
                                contentColor = topBarContentColor,
                                onMore = {},
                            )
                        }
                    } else {
                        AppTopBar(title, onBack, openDrawer)
                        TabContent(tabs, selectedTabIndex, actions, libraryState, Modifier.weight(1f))
                    }
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
                                    label = { Text(stringResource(tab.label)) },
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
                                    label = { Text(stringResource(tab.label)) },
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (transparentTopBar) {
                            Box(modifier = Modifier.weight(1f)) {
                                TabContent(tabs, selectedTabIndex, actions, libraryState, Modifier.fillMaxSize())
                                AppTopBar(
                                    null,
                                    onBack,
                                    null,
                                    transparent = true,
                                    contentColor = topBarContentColor,
                                    onMore = {},
                                )
                            }
                        } else {
                            AppTopBar(title, onBack, null)
                            TabContent(tabs, selectedTabIndex, actions, libraryState, Modifier.weight(1f))
                        }
                        if (playbackState.currentTrack != null) {
                            MiniPlayer(playbackState, onOpenPlayer, actions.togglePlayPause, actions.next)
                        }
                    }
                }
            }
        }
    }
}

private val staticRouteTitles: Map<NavKey, StringResource> = mapOf(
    HomeRoute to Res.string.tab_home,
    AlbumsRoute to Res.string.tab_albums,
    SongsRoute to Res.string.tab_songs,
    PlaylistsRoute to Res.string.tab_playlists,
    ArtistsRoute to Res.string.tab_artists,
    SearchRoute to Res.string.search,
    SettingsRoute to Res.string.settings,
    AppearanceRoute to Res.string.appearance,
    PlaybackSettingsRoute to Res.string.playback,
    LibrarySettingsRoute to Res.string.library,
    ServersRoute to Res.string.servers,
    AboutRoute to Res.string.about,
)

@Composable
private fun routeTitle(route: NavKey?): String {
    val resource = when (route) {
        null -> null
        is AlbumRoute -> Res.string.album
        is ArtistRoute -> Res.string.artist
        is PlaylistRoute -> Res.string.playlist
        is FolderRoute -> Res.string.folder
        is ServerEditRoute -> if (route.serverId == null) Res.string.add_server else Res.string.server
        else -> staticRouteTitles[route]
    }
    return stringResource(resource ?: Res.string.app_name)
}

@Composable
private fun AppTopBar(
    title: String?,
    onBack: (() -> Unit)?,
    onMenu: (() -> Unit)?,
    transparent: Boolean = false,
    contentColor: Color? = null,
    onMore: (() -> Unit)? = null,
) {
    val back = onBack
    val menu = onMenu
    val navigationIcon: (@Composable () -> Unit)? = when {
        back != null -> {
            {
                IconButton(onClick = back) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            }
        }

        menu != null -> {
            {
                IconButton(onClick = menu) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_menu),
                        contentDescription = stringResource(Res.string.menu),
                    )
                }
            }
        }

        else -> null
    }
    val more: (@Composable () -> Unit)? = onMore?.let { moreAction ->
        {
            IconButton(onClick = moreAction) {
                Icon(
                    painter = painterResource(Res.drawable.ic_more_vert),
                    contentDescription = stringResource(Res.string.more_options),
                )
            }
        }
    }
    if (transparent) {
        RTopAppBar(
            title = { title?.let { Text(it) } },
            navigationIcon = navigationIcon,
            actions = { more?.invoke() },
            backgroundColor = Color.Transparent,
            contentColor = contentColor ?: LocalContentColor.current,
            elevation = 0.dp,
        )
    } else {
        RTopAppBar(
            title = { title?.let { Text(it) } },
            navigationIcon = navigationIcon,
            actions = { more?.invoke() },
        )
    }
}

@Composable
private fun TabIcon(tab: TabItem, selected: Boolean) {
    Icon(
        painter = painterResource(if (selected) tab.filledIcon else tab.outlinedIcon),
        contentDescription = stringResource(tab.label),
    )
}

@Composable
private fun TabContent(
    tabs: List<TabItem>,
    selectedIndex: Int,
    actions: AppActions,
    libraryState: LibraryUiState,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = selectedIndex,
        transitionSpec = { fadeThrough() },
        modifier = modifier,
    ) { index ->
        AppNavDisplay(tabs[index].backStack, actions, libraryState, Modifier.fillMaxSize())
    }
}

@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    actions: AppActions,
    libraryState: LibraryUiState,
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
            entry<AlbumsRoute> { AlbumsScreen(libraryState, actions) }
            entry<SongsRoute> { SongsScreen(libraryState, actions) }
            entry<PlaylistsRoute> { PlaylistsScreen(actions) }
            entry<ArtistsRoute> { ArtistsScreen(libraryState) }
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

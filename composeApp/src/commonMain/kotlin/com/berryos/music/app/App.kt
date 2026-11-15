package com.berryos.music.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.DrawerValue
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.berryos.music.core.datastore.AppSettingsRepository
import com.berryos.music.core.designsystem.BerryTheme
import com.berryos.music.core.designsystem.CoverColors
import com.berryos.music.core.designsystem.CoverPaletteDialog
import com.berryos.music.core.designsystem.cachedCoverColors
import com.berryos.music.core.designsystem.rememberDefaultCoverColors
import com.berryos.music.core.designsystem.rememberLoadedCoverColors
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.library.filtered
import com.berryos.music.core.model.album
import com.berryos.music.core.playback.PlaybackController
import com.berryos.music.core.playback.PlaybackState
import com.berryos.music.feature.album.AlbumScreen
import com.berryos.music.feature.albums.AlbumsScreen
import com.berryos.music.feature.artist.ArtistScreen
import com.berryos.music.feature.artists.ArtistsScreen
import com.berryos.music.feature.folder.FolderScreen
import com.berryos.music.feature.home.HomeScreen
import com.berryos.music.feature.nowplaying.MiniPlayer
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
import com.berryos.music.resources.cover_palette
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(playback: PlaybackController, auth: AuthController, settings: AppSettingsRepository) {
    val libraryViewModel: LibraryViewModel = koinViewModel()
    val libraryState by libraryViewModel.state.collectAsState()
    val playbackState by playback.state.collectAsState()
    val authState by auth.state.collectAsState()
    val visibleLibrary = libraryState.filtered(authState.showingLocal)
    val nowPlayingCoverUrl = remember(visibleLibrary, playbackState.currentTrack) {
        val track = playbackState.currentTrack
        val snapshot = (visibleLibrary as? LibraryUiState.Ready)?.snapshot
        if (track == null || snapshot == null) {
            null
        } else {
            snapshot.album(track.source, track.albumId)?.coverUrl
        }
    }
    BerryTheme(accent = rememberLoadedCoverColors(nowPlayingCoverUrl)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppShell(
                visibleLibrary,
                playback,
                playbackState,
                authState,
                auth,
                settings,
            )
        }
    }
}

internal data class TabItem(
    val label: StringResource,
    val filledIcon: DrawableResource,
    val outlinedIcon: DrawableResource,
    val backStack: NavBackStack<NavKey>,
)

@Suppress("LongMethod")
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AppShell(
    libraryState: LibraryUiState,
    playback: PlaybackController,
    playbackState: PlaybackState,
    authState: AuthState,
    auth: AuthController,
    settings: AppSettingsRepository,
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
    val playbackSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    val scope = rememberCoroutineScope()
    val appSettings by settings.settings.collectAsState()
    val collapseOnPause = appSettings.collapsePlayerOnPause
    val nowPlayingTarget = resolveNowPlayingTarget(libraryState, playbackState)
    val sheetVisible = playbackSheetState.currentValue != ModalBottomSheetValue.Hidden ||
        playbackSheetState.targetValue != ModalBottomSheetValue.Hidden
    ModalBottomSheetLayout(
        sheetState = playbackSheetState,
        sheetContent = {
            Surface(modifier = Modifier.fillMaxSize()) {
                // Only the visible sheet owns a header state; a hidden sheet must not mirror the page.
                if (sheetVisible) {
                    when (val target = nowPlayingTarget) {
                        is NowPlayingTarget.Album ->
                            AlbumScreen(target.albumId, libraryState, actions, playbackState, collapseOnPause)

                        is NowPlayingTarget.Artist ->
                            ArtistScreen(target.artistId, libraryState, actions, playbackState, collapseOnPause)

                        is NowPlayingTarget.Playlist -> PlaylistScreen(target.playlistId)

                        null -> Unit
                    }
                }
            }
        },
    ) {
        AppChrome(
            tabs,
            selectedTabIndex,
            actions,
            libraryState,
            playbackState,
            authState,
            auth,
            settings,
            collapseOnPause,
            onOpenNowPlaying = {
                if (nowPlayingTarget != null) {
                    scope.launch { playbackSheetState.show() }
                }
            },
        ) { index ->
            selectedTabIndex = index
        }
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

@Suppress("LongMethod", "CyclomaticComplexMethod")
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
    settings: AppSettingsRepository,
    collapseOnPause: Boolean,
    onOpenNowPlaying: () -> Unit,
    onSelectTab: (Int) -> Unit,
) {
    val selectedTab = tabs[selectedTabIndex]
    val isRoot = selectedTab.backStack.size == 1
    val route = selectedTab.backStack.lastOrNull()
    val isDetailRoute = route is AlbumRoute || route is ArtistRoute
    val title = routeTitle(route)
    val detailCoverUrl = when (route) {
        is AlbumRoute -> (libraryState as? LibraryUiState.Ready)?.snapshot?.albums
            ?.firstOrNull { it.id == route.albumId }?.coverUrl

        is ArtistRoute -> (libraryState as? LibraryUiState.Ready)?.snapshot?.artists
            ?.firstOrNull { it.id == route.artistId }?.coverUrl

        else -> null
    }
    val detailColors = if (!isDetailRoute) {
        null
    } else {
        val fallbackColors = rememberDefaultCoverColors()
        detailCoverUrl?.let(::cachedCoverColors) ?: fallbackColors
    }
    val topBarContentColor = detailColors?.contentColor
    var showCoverPalette by rememberSaveable { mutableStateOf(false) }
    val onBack = if (isRoot) null else actions.back
    val showMiniPlayer = playbackState.currentTrack != null && !isDetailRoute
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
                    if (isDetailRoute) {
                        Box(modifier = Modifier.weight(1f)) {
                            TabContent(
                                tabs = tabs,
                                selectedIndex = selectedTabIndex,
                                actions = actions,
                                libraryState = libraryState,
                                playbackState = playbackState,
                                settings = settings,
                                collapseOnPause = collapseOnPause,
                                modifier = Modifier.fillMaxSize(),
                            )
                            AppTopBar(
                                null,
                                onBack,
                                openDrawer,
                                transparent = true,
                                contentColor = topBarContentColor,
                                onCoverPalette = { showCoverPalette = true },
                            )
                        }
                    } else {
                        AppTopBar(title, onBack, openDrawer)
                        TabContent(
                            tabs = tabs,
                            selectedIndex = selectedTabIndex,
                            actions = actions,
                            libraryState = libraryState,
                            playbackState = playbackState,
                            settings = settings,
                            collapseOnPause = collapseOnPause,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (showMiniPlayer) {
                        MiniPlayer(playbackState, onOpenNowPlaying, actions.togglePlayPause, actions.next)
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
                        if (isDetailRoute) {
                            Box(modifier = Modifier.weight(1f)) {
                                TabContent(
                                    tabs = tabs,
                                    selectedIndex = selectedTabIndex,
                                    actions = actions,
                                    libraryState = libraryState,
                                    playbackState = playbackState,
                                    settings = settings,
                                    collapseOnPause = collapseOnPause,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                AppTopBar(
                                    null,
                                    onBack,
                                    null,
                                    transparent = true,
                                    contentColor = topBarContentColor,
                                    onCoverPalette = { showCoverPalette = true },
                                )
                            }
                        } else {
                            AppTopBar(title, onBack, null)
                            TabContent(
                                tabs = tabs,
                                selectedIndex = selectedTabIndex,
                                actions = actions,
                                libraryState = libraryState,
                                playbackState = playbackState,
                                settings = settings,
                                collapseOnPause = collapseOnPause,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (showMiniPlayer) {
                            MiniPlayer(playbackState, onOpenNowPlaying, actions.togglePlayPause, actions.next)
                        }
                    }
                }
            }
        }
        CoverPaletteHost(showCoverPalette, detailColors) { showCoverPalette = false }
    }
}

@Composable
private fun CoverPaletteHost(visible: Boolean, colors: CoverColors?, onDismiss: () -> Unit) {
    if (visible) {
        colors?.let { CoverPaletteDialog(it.swatches, onDismiss = onDismiss) }
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
    onCoverPalette: (() -> Unit)? = null,
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
    val more: (@Composable () -> Unit)? = onCoverPalette?.let { openPalette ->
        { AppMoreMenu(onCoverPalette = openPalette) }
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
private fun AppMoreMenu(onCoverPalette: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(Res.drawable.ic_more_vert),
                contentDescription = stringResource(Res.string.more_options),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    onCoverPalette()
                },
            ) {
                Text(stringResource(Res.string.cover_palette))
            }
        }
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
    playbackState: PlaybackState,
    settings: AppSettingsRepository,
    collapseOnPause: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = selectedIndex,
        transitionSpec = { fadeThrough() },
        modifier = modifier,
    ) { index ->
        AppNavDisplay(
            tabs[index].backStack,
            actions,
            libraryState,
            playbackState,
            settings,
            collapseOnPause,
            Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    actions: AppActions,
    libraryState: LibraryUiState,
    playbackState: PlaybackState,
    settings: AppSettingsRepository,
    collapseOnPause: Boolean,
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
            entry<ArtistsRoute> { ArtistsScreen(libraryState, actions) }
            entry<SearchRoute> { SearchScreen(actions) }
            entry<SettingsRoute> { SettingsScreen(actions) }
            entry<AlbumRoute> { route ->
                AlbumScreen(route.albumId, libraryState, actions, playbackState, collapseOnPause)
            }
            entry<ArtistRoute> { entry ->
                ArtistScreen(entry.artistId, libraryState, actions, playbackState, collapseOnPause)
            }
            entry<PlaylistRoute> { route -> PlaylistScreen(route.playlistId) }
            entry<FolderRoute> { route -> FolderScreen(route.folderId) }
            entry<AppearanceRoute> { AppearanceScreen(settings) }
            entry<PlaybackSettingsRoute> { PlaybackSettingsScreen() }
            entry<LibrarySettingsRoute> { LibrarySettingsScreen() }
            entry<ServersRoute> { ServersScreen(actions) }
            entry<ServerEditRoute> { route -> ServerEditScreen(route.serverId) }
            entry<AboutRoute> { AboutScreen() }
        },
    )
}

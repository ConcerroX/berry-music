@file:Suppress("TooManyFunctions")

package com.berryos.music.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailDefaults
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import art.rippledesign.compose.rui.RBottomNavigation
import art.rippledesign.compose.rui.RBottomNavigationItem
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
import com.berryos.music.core.auth.AuthController
import com.berryos.music.core.auth.AuthState
import com.berryos.music.core.datastore.AppSettingsRepository
import com.berryos.music.core.designsystem.CoverPaletteDialog
import com.berryos.music.core.designsystem.cachedCoverColors
import com.berryos.music.core.designsystem.rememberDefaultCoverColors
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.model.album
import com.berryos.music.core.model.artist
import com.berryos.music.core.playback.PlaybackPosition
import com.berryos.music.core.playback.PlaybackSource
import com.berryos.music.core.playback.PlaybackState
import com.berryos.music.feature.nowplaying.MiniPlayer
import com.berryos.music.resources.Res
import com.berryos.music.resources.about
import com.berryos.music.resources.add_server
import com.berryos.music.resources.album
import com.berryos.music.resources.app_name
import com.berryos.music.resources.appearance
import com.berryos.music.resources.artist
import com.berryos.music.resources.folder
import com.berryos.music.resources.library
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun AppChrome(
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    actions: AppActions,
    libraryState: LibraryUiState,
    playbackState: PlaybackState,
    playbackPosition: StateFlow<PlaybackPosition>,
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
        is AlbumRoute -> (libraryState as? LibraryUiState.Ready)?.snapshot?.album(route.source, route.albumId)
            ?.coverUrl

        is ArtistRoute -> (libraryState as? LibraryUiState.Ready)?.snapshot?.artist(route.source, route.artistId)
            ?.coverUrl

        else -> null
    }
    val detailColors = if (!isDetailRoute) {
        null
    } else {
        val fallbackColors = rememberDefaultCoverColors()
        detailCoverUrl?.let(::cachedCoverColors) ?: fallbackColors
    }
    var showCoverPalette by rememberSaveable { mutableStateOf(false) }
    val onBack = if (isRoot) null else actions.back
    val showMiniPlayer = playbackState.currentTrack != null
    val detailSessionInactive = when (route) {
        is AlbumRoute -> playbackState.source != PlaybackSource.Album(route.albumId)
        is ArtistRoute -> playbackState.source != PlaybackSource.Artist(route.artistId)
        else -> false
    }
    // On a detail page whose album/artist is not the active session, only the bar steps aside;
    // the same mini player stays and slides down into its place at the bottom.
    val barHandsOffToMiniPlayer = isDetailRoute && detailSessionInactive
    var bottomBarHidden by remember { mutableStateOf(false) }
    val barShouldHide = bottomBarHidden || !isRoot
    val bottomBarOffset by animateFloatAsState(
        targetValue = if (barShouldHide) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (barShouldHide) CHROME_HIDE_MILLIS else CHROME_SHOW_MILLIS,
            easing = if (barShouldHide) FastOutLinearInEasing else LinearOutSlowInEasing,
        ),
        label = "bottomBarOffset",
    )
    val thresholdPx = with(LocalDensity.current) { BOTTOM_BAR_SCROLL_THRESHOLD.toPx() }
    val hideOnScroll = remember(thresholdPx) {
        object : NestedScrollConnection {
            private var accumulated = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (consumed.y == 0f) return Offset.Zero
                // Debounce: require a sustained same-direction drag before toggling, and drop the
                // accumulation as soon as the direction reverses.
                accumulated = if (accumulated * consumed.y < 0f) consumed.y else accumulated + consumed.y
                if (accumulated < -thresholdPx) {
                    bottomBarHidden = true
                    accumulated = 0f
                } else if (accumulated > thresholdPx) {
                    bottomBarHidden = false
                    accumulated = 0f
                }
                return Offset.Zero
            }
        }
    }
    val chromeStaysVisible = isRoot || barHandsOffToMiniPlayer
    val rootChromeOffset by animateFloatAsState(
        targetValue = if (chromeStaysVisible) 0f else 1f,
        animationSpec = tween(
            durationMillis = if (chromeStaysVisible) CHROME_SHOW_MILLIS else CHROME_HIDE_MILLIS,
            easing = if (chromeStaysVisible) LinearOutSlowInEasing else FastOutLinearInEasing,
        ),
        label = "rootChromeOffset",
    )
    val miniCoverUrl = (libraryState as? LibraryUiState.Ready)?.snapshot?.let { snapshot ->
        playbackState.currentTrack?.let { track -> snapshot.album(track.source, track.albumId)?.coverUrl }
    }
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val drawerScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.getTop(density)
    val navigationBarBottom = WindowInsets.navigationBars.getBottom(density)
    var topBarHeight by remember {
        mutableStateOf(56.dp + with(density) { statusBarTop.toDp() })
    }
    var bottomBarHeight by remember {
        mutableStateOf(56.dp + with(density) { navigationBarBottom.toDp() })
    }
    var miniPlayerHeight by remember { mutableStateOf(64.dp) }
    var railWidth by remember { mutableStateOf(72.dp) }
    val chromeMetrics = ChromeMetrics(
        topBarHeight = topBarHeight,
        bottomBarHeight = bottomBarHeight,
        miniPlayerHeight = miniPlayerHeight,
        railWidth = railWidth,
    )
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        // Only the mini player permanently occupies the bottom; the bar is allowed to cover the
        // end of the list, exactly like MDC, because scrolling down hides it.
        val rootBottomInset = if (showMiniPlayer) miniPlayerHeight else 0.dp
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
            Box(modifier = Modifier.fillMaxSize().nestedScroll(hideOnScroll)) {
                val drawerAction: (() -> Unit)? = if (compact) {
                    { drawerScope.launch { scaffoldState.drawerState.open() } }
                } else {
                    null
                }
                ChromeUnderlay(
                    compact = compact,
                    isRoot = isRoot,
                    isDetailRoute = isDetailRoute,
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    title = if (isDetailRoute) routeTitle(selectedTab.backStack.firstOrNull()) else title,
                    onBack = if (isDetailRoute) null else onBack,
                    openDrawer = if (isDetailRoute) drawerAction else openDrawer,
                    showMiniPlayer = showMiniPlayer && !isRoot && !isDetailRoute,
                    miniCoverUrl = miniCoverUrl,
                    playbackState = playbackState,
                    playbackPosition = playbackPosition,
                    onOpenNowPlaying = onOpenNowPlaying,
                    actions = actions,
                    railWidth = railWidth,
                    onTopBarSize = { topBarHeight = with(density) { it.height.toDp() } },
                    onMiniPlayerSize = { miniPlayerHeight = with(density) { it.height.toDp() } },
                    onRailSize = { railWidth = with(density) { it.width.toDp() } },
                    onSelectTab = onSelectTab,
                )
                if (isDetailRoute) {
                    Box(modifier = Modifier.fillMaxSize().consumeAllPointerInput())
                }
                TabContent(
                    tabs = tabs,
                    selectedIndex = selectedTabIndex,
                    actions = actions,
                    libraryState = libraryState,
                    playbackState = playbackState,
                    playbackPosition = playbackPosition,
                    settings = settings,
                    collapseOnPause = collapseOnPause,
                    chromeMetrics = chromeMetrics,
                    compact = compact,
                    showMiniPlayer = showMiniPlayer,
                    bottomContentInset = rootBottomInset,
                    onCoverPalette = { showCoverPalette = true },
                    modifier = Modifier.fillMaxSize(),
                )
                RootBottomChrome(
                    compact = compact,
                    railWidth = railWidth,
                    showMiniPlayer = showMiniPlayer,
                    miniCoverUrl = miniCoverUrl,
                    playbackState = playbackState,
                    playbackPosition = playbackPosition,
                    onOpenNowPlaying = onOpenNowPlaying,
                    actions = actions,
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    bottomBarOffset = bottomBarOffset,
                    bottomBarHeight = bottomBarHeight,
                    chromeOffset = rootChromeOffset,
                    onMiniPlayerSize = { miniPlayerHeight = with(density) { it.height.toDp() } },
                    onBottomBarSize = { bottomBarHeight = with(density) { it.height.toDp() } },
                    onSelectTab = onSelectTab,
                )
            }
        }
        if (showCoverPalette) {
            detailColors?.let { paletteColors ->
                CoverPaletteDialog(paletteColors.swatches, onDismiss = { showCoverPalette = false })
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ChromeUnderlay(
    compact: Boolean,
    isRoot: Boolean,
    isDetailRoute: Boolean,
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    title: String,
    onBack: (() -> Unit)?,
    openDrawer: (() -> Unit)?,
    showMiniPlayer: Boolean,
    miniCoverUrl: String?,
    playbackState: PlaybackState,
    playbackPosition: StateFlow<PlaybackPosition>,
    onOpenNowPlaying: () -> Unit,
    actions: AppActions,
    railWidth: Dp,
    onTopBarSize: (IntSize) -> Unit,
    onMiniPlayerSize: (IntSize) -> Unit,
    onRailSize: (IntSize) -> Unit,
    onSelectTab: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (isDetailRoute) Modifier.clearAndSetSemantics { } else Modifier),
    ) {
        val contentStart = if (!compact && isRoot) railWidth else 0.dp
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(start = contentStart)
                .onSizeChanged(onTopBarSize),
        ) {
            AppTopBar(title, onBack, openDrawer)
        }
        if (!compact && (isRoot || isDetailRoute)) {
            NavigationRail(
                windowInsets = NavigationRailDefaults.windowInsets,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .onSizeChanged(onRailSize),
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationRailItem(
                        selected = index == selectedTabIndex,
                        onClick = { if (isRoot) onSelectTab(index) },
                        enabled = isRoot,
                        icon = { TabIcon(tab, index == selectedTabIndex) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = contentStart),
        ) {
            if (showMiniPlayer) {
                Box(Modifier.onSizeChanged(onMiniPlayerSize)) {
                    MiniPlayer(
                        playbackState,
                        miniCoverUrl,
                        playbackPosition,
                        onOpenNowPlaying,
                        actions.togglePlayPause,
                        actions.next,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.RootBottomChrome(
    compact: Boolean,
    railWidth: Dp,
    showMiniPlayer: Boolean,
    miniCoverUrl: String?,
    playbackState: PlaybackState,
    playbackPosition: StateFlow<PlaybackPosition>,
    onOpenNowPlaying: () -> Unit,
    actions: AppActions,
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    bottomBarOffset: Float,
    bottomBarHeight: Dp,
    chromeOffset: Float,
    onMiniPlayerSize: (IntSize) -> Unit,
    onBottomBarSize: (IntSize) -> Unit,
    onSelectTab: (Int) -> Unit,
) {
    val bottomBarHeightPx = with(LocalDensity.current) { bottomBarHeight.toPx() }
    val barTranslationPx = if (compact) bottomBarHeightPx else 0f
    Column(
        // The whole bottom chrome travels together: the bar slides off-screen while the mini
        // player takes its place at the bottom instead of being left behind.
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = if (!compact) railWidth else 0.dp)
            .then(if (chromeOffset >= 1f) Modifier.clearAndSetSemantics { } else Modifier)
            .graphicsLayer {
                translationY = barTranslationPx * bottomBarOffset + size.height * chromeOffset
            },
    ) {
        AnimatedVisibility(
            visible = showMiniPlayer,
            enter = slideInVertically(
                animationSpec = tween(
                    MINI_PLAYER_ENTER_MILLIS,
                    easing = LinearOutSlowInEasing,
                ),
                initialOffsetY = { height -> height },
            ),
            exit = slideOutVertically(
                animationSpec = tween(
                    MINI_PLAYER_EXIT_MILLIS,
                    easing = FastOutLinearInEasing,
                ),
                targetOffsetY = { height -> height },
            ),
        ) {
            Box(Modifier.onSizeChanged(onMiniPlayerSize)) {
                MiniPlayer(
                    playbackState,
                    miniCoverUrl,
                    playbackPosition,
                    onOpenNowPlaying,
                    actions.togglePlayPause,
                    actions.next,
                )
            }
        }
        if (compact) {
            Box(Modifier.onSizeChanged(onBottomBarSize)) {
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
    }
}

@Composable
private fun TabIcon(tab: TabItem, selected: Boolean) {
    Icon(
        painter = painterResource(if (selected) tab.filledIcon else tab.outlinedIcon),
        contentDescription = stringResource(tab.label),
    )
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

private const val CHROME_HIDE_MILLIS = 175
private const val CHROME_SHOW_MILLIS = 225
private const val MINI_PLAYER_ENTER_MILLIS = 250
private const val MINI_PLAYER_EXIT_MILLIS = 200
private val BOTTOM_BAR_SCROLL_THRESHOLD = 24.dp

@file:Suppress("TooManyFunctions")

package com.berryos.music.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.unit.Velocity
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
import com.berryos.music.core.designsystem.LocalPageScrollable
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

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
    // MDC AppBarLayout: the scroll first moves the bar (the delta is consumed), and the page
    // content area follows the bar's bottom edge, so the freed space is filled instead of leaving
    // a gap. Releasing mid-way snaps to the nearer edge with MDC's decelerate animation. The
    // offset is kept per route: returning to a scrolled page restores its hidden bar, while a
    // newly opened page starts with the bar visible.
    val topBarOffsets = remember { mutableStateMapOf<Any, Float>() }
    val topBarHeightPx = with(density) { topBarHeight.toPx() }
    val topBarHeightState = rememberUpdatedState(topBarHeightPx)
    val routeKey: Any = route ?: selectedTab
    val routeState = rememberUpdatedState(routeKey)
    val pageScrollable = remember { mutableStateOf(true) }
    var previousRouteKey by remember { mutableStateOf<Any?>(null) }
    // A detail route inherits the bar position of the page it was opened from, so the bar keeps
    // its state while the transform runs instead of jumping to visible under it. Other routes
    // start with the bar visible.
    val inheritedOffsetPx = previousRouteKey?.let { topBarOffsets[it] } ?: 0f
    val fallbackOffsetPx = if (isDetailRoute) inheritedOffsetPx else 0f
    var topBarSnapJob by remember { mutableStateOf<Job?>(null) }
    val chromeScope = rememberCoroutineScope()
    LaunchedEffect(routeKey) {
        topBarSnapJob?.cancel()
        val previous = previousRouteKey
        if (previous != null && previous != routeKey) {
            // A snap may still be running when the user opens another page; settle the old page to
            // the nearer edge so it is never left partially collapsed.
            val height = topBarHeightState.value
            val offset = topBarOffsets[previous] ?: 0f
            if (offset > 0f && offset < height) {
                topBarOffsets[previous] = if (offset > height / 2f) height else 0f
            }
            if (isDetailRoute && routeKey !in topBarOffsets) {
                topBarOffsets[routeKey] = topBarOffsets[previous] ?: 0f
            }
        }
        previousRouteKey = routeKey
    }
    fun setTopBarOffset(offsetPx: Float) {
        topBarOffsets[routeState.value] = offsetPx.coerceIn(0f, topBarHeightState.value)
    }
    val topBarOffsetPx = (topBarOffsets[routeKey] ?: fallbackOffsetPx).coerceIn(0f, topBarHeightPx)
    val topContentInset = with(density) { (topBarHeightPx - topBarOffsetPx).coerceAtLeast(0f).toDp() }
    // Detail pages draw their own top bar and fill the screen, so the global bar does not follow
    // their scroll.
    val detailRouteState = rememberUpdatedState(isDetailRoute)
    val thresholdPx = with(density) { CHROME_SCROLL_THRESHOLD.toPx() }
    val hideOnScroll = remember(thresholdPx) {
        object : NestedScrollConnection {
            private var accumulated = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val height = topBarHeightState.value
                if (detailRouteState.value || !pageScrollable.value || height <= 0f) return Offset.Zero
                val current = topBarOffsets[routeState.value] ?: 0f
                val consumed = if (available.y < 0f) {
                    maxOf(available.y, -(height - current))
                } else if (available.y > 0f) {
                    minOf(available.y, current)
                } else {
                    0f
                }
                if (consumed != 0f) {
                    topBarSnapJob?.cancel()
                    setTopBarOffset(current - consumed)
                }
                return Offset(0f, consumed)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                snapTopBar()
                return Velocity.Zero
            }

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

            private fun snapTopBar() {
                if (detailRouteState.value) return
                val height = topBarHeightState.value
                val key = routeState.value
                val current = topBarOffsets[key] ?: 0f
                if (height <= 0f || current <= 0f || current >= height) return
                val target = if (current > height / 2f) height else 0f
                val duration = (((abs(target - current) / height) + 1f) * MDC_SNAP_MILLIS).toInt()
                topBarSnapJob?.cancel()
                topBarSnapJob = chromeScope.launch {
                    animate(current, target, animationSpec = tween(duration, easing = DecelerateEasing)) { value, _ ->
                        topBarOffsets[key] = value.coerceIn(0f, height)
                    }
                }
            }
        }
    }
    val topBarHidden = topBarOffsetPx >= topBarHeightPx - 0.5f
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
                val contentStart = if (!compact && isRoot) railWidth else 0.dp
                // Detail pages draw their own top bar and fill the screen, so the global bar stays
                // behind their content.
                if (isDetailRoute) {
                    TopBarOverlay(
                        title = routeTitle(selectedTab.backStack.firstOrNull()),
                        onBack = null,
                        openDrawer = drawerAction,
                        contentStart = contentStart,
                        offsetPx = topBarOffsetPx,
                        hidden = topBarHidden,
                        onMeasured = { topBarHeight = with(density) { it.height.toDp() } },
                    )
                }
                ChromeUnderlay(
                    compact = compact,
                    isRoot = isRoot,
                    isDetailRoute = isDetailRoute,
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    showMiniPlayer = showMiniPlayer && !isRoot && !isDetailRoute,
                    miniCoverUrl = miniCoverUrl,
                    playbackState = playbackState,
                    playbackPosition = playbackPosition,
                    onOpenNowPlaying = onOpenNowPlaying,
                    actions = actions,
                    railWidth = railWidth,
                    onMiniPlayerSize = { miniPlayerHeight = with(density) { it.height.toDp() } },
                    onRailSize = { railWidth = with(density) { it.width.toDp() } },
                    onSelectTab = onSelectTab,
                )
                if (isDetailRoute) {
                    Box(modifier = Modifier.fillMaxSize().consumeAllPointerInput())
                }
                CompositionLocalProvider(LocalPageScrollable provides pageScrollable) {
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
                        topContentInset = topContentInset,
                        compact = compact,
                        showMiniPlayer = showMiniPlayer,
                        bottomContentInset = rootBottomInset,
                        onCoverPalette = { showCoverPalette = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // Pages scroll under the bar, so the bar is drawn above their content.
                if (!isDetailRoute) {
                    TopBarOverlay(
                        title = title,
                        onBack = onBack,
                        openDrawer = openDrawer,
                        contentStart = contentStart,
                        offsetPx = topBarOffsetPx,
                        hidden = topBarHidden,
                        onMeasured = { topBarHeight = with(density) { it.height.toDp() } },
                    )
                }
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
    showMiniPlayer: Boolean,
    miniCoverUrl: String?,
    playbackState: PlaybackState,
    playbackPosition: StateFlow<PlaybackPosition>,
    onOpenNowPlaying: () -> Unit,
    actions: AppActions,
    railWidth: Dp,
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
private fun BoxScope.TopBarOverlay(
    title: String,
    onBack: (() -> Unit)?,
    openDrawer: (() -> Unit)?,
    contentStart: Dp,
    offsetPx: Float,
    hidden: Boolean,
    onMeasured: (IntSize) -> Unit,
) {
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .graphicsLayer { translationY = -offsetPx }
            .then(if (hidden) Modifier.clearAndSetSemantics { } else Modifier)
            .padding(start = contentStart)
            .onSizeChanged(onMeasured),
    ) {
        AppTopBar(title, onBack, openDrawer)
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
private const val MDC_SNAP_MILLIS = 150
private const val MINI_PLAYER_ENTER_MILLIS = 250
private const val MINI_PLAYER_EXIT_MILLIS = 200
private val CHROME_SCROLL_THRESHOLD = 12.dp

/** MDC's DecelerateInterpolator, used for the app bar snap. */
private val DecelerateEasing = Easing { fraction -> 1f - (1f - fraction) * (1f - fraction) }

package com.berryos.music.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberNavigationEventState
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationEventHandler
import art.rippledesign.compose.rui.fadeThrough
import art.rippledesign.compose.rui.navigation3.LocalRContainerTransformNavigationEventState
import art.rippledesign.compose.rui.navigation3.RContainerTransformNavController
import art.rippledesign.compose.rui.navigation3.RContainerTransformNavEntry
import art.rippledesign.compose.rui.navigation3.rContainerTransformTransition
import art.rippledesign.compose.rui.navigation3.rememberRContainerTransformNavController
import com.berryos.music.app.navigation.AboutRoute
import com.berryos.music.app.navigation.AlbumRoute
import com.berryos.music.app.navigation.AlbumsRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.AppearanceRoute
import com.berryos.music.app.navigation.ArtistRoute
import com.berryos.music.app.navigation.ArtistsRoute
import com.berryos.music.app.navigation.ContainerTransformKey
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
import com.berryos.music.app.navigation.TrackMetadataRoute
import com.berryos.music.app.navigation.albumTransformKey
import com.berryos.music.app.navigation.artistTransformKey
import com.berryos.music.core.datastore.AppSettingsRepository
import com.berryos.music.core.designsystem.ContainerTransformSpec
import com.berryos.music.core.designsystem.cachedCoverBackground
import com.berryos.music.core.designsystem.rememberDefaultCoverColors
import com.berryos.music.core.designsystem.withContainerColor
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.model.SourceKey
import com.berryos.music.core.model.album
import com.berryos.music.core.model.artist
import com.berryos.music.core.playback.PlaybackPosition
import com.berryos.music.core.playback.PlaybackSource
import com.berryos.music.core.playback.PlaybackState
import com.berryos.music.feature.album.AlbumScreen
import com.berryos.music.feature.albums.AlbumsScreen
import com.berryos.music.feature.artist.ArtistScreen
import com.berryos.music.feature.artists.ArtistsScreen
import com.berryos.music.feature.detail.TrackMetadataScreen
import com.berryos.music.feature.folder.FolderScreen
import com.berryos.music.feature.home.HomeScreen
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
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun TabContent(
    tabs: List<TabItem>,
    selectedIndex: Int,
    actions: AppActions,
    libraryState: LibraryUiState,
    playbackState: PlaybackState,
    playbackPosition: StateFlow<PlaybackPosition>,
    settings: AppSettingsRepository,
    collapseOnPause: Boolean,
    chromeMetrics: ChromeMetrics,
    topContentInset: Dp,
    compact: Boolean,
    showMiniPlayer: Boolean,
    bottomContentInset: Dp,
    onCoverPalette: () -> Unit,
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
            playbackPosition,
            settings,
            collapseOnPause,
            chromeMetrics,
            topContentInset,
            compact,
            showMiniPlayer,
            bottomContentInset,
            onCoverPalette,
            Modifier.fillMaxSize(),
        )
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    actions: AppActions,
    libraryState: LibraryUiState,
    playbackState: PlaybackState,
    playbackPosition: StateFlow<PlaybackPosition>,
    settings: AppSettingsRepository,
    collapseOnPause: Boolean,
    chromeMetrics: ChromeMetrics,
    topContentInset: Dp,
    compact: Boolean,
    showMiniPlayer: Boolean,
    bottomContentInset: Dp,
    onCoverPalette: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = rememberRContainerTransformNavController<ContainerTransformKey>()
    // MDC's scrolling view is offset by the app bar's current bottom edge; the inset tracks the
    // bar offset so the content fills the space the bar leaves instead of showing a gap. The mini
    // player inset is a container inset so the list (and its overscroll glow) ends above it.
    val rootPadding = PaddingValues(
        start = if (!compact) chromeMetrics.railWidth else 0.dp,
        top = topContentInset,
        end = 0.dp,
        bottom = bottomContentInset,
    )
    val pushedPadding = PaddingValues(
        start = 0.dp,
        top = topContentInset,
        end = 0.dp,
        bottom = if (showMiniPlayer) chromeMetrics.miniPlayerHeight else 0.dp,
    )
    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeRoute> { PaddedScreen(rootPadding) { HomeScreen(actions, libraryState) } }
            entry<AlbumsRoute>(
                metadata = mapOf(CONTAINER_TRANSFORM_TAG_KEY to ContainerTransformTag.SourceAlbums),
            ) {
                PaddedScreen(rootPadding) { AlbumsScreen(libraryState, actions, controller) }
            }
            entry<SongsRoute> { PaddedScreen(rootPadding) { SongsScreen(libraryState, actions) } }
            entry<PlaylistsRoute> { PaddedScreen(rootPadding) { PlaylistsScreen(actions) } }
            entry<ArtistsRoute>(
                metadata = mapOf(CONTAINER_TRANSFORM_TAG_KEY to ContainerTransformTag.SourceArtists),
            ) {
                PaddedScreen(rootPadding) { ArtistsScreen(libraryState, actions, controller) }
            }
            entry<SearchRoute> { PaddedScreen(pushedPadding) { SearchScreen(actions) } }
            entry<SettingsRoute> { PaddedScreen(pushedPadding) { SettingsScreen(actions) } }
            entry<AlbumRoute>(
                metadata = mapOf(CONTAINER_TRANSFORM_TAG_KEY to ContainerTransformTag.TargetAlbum),
            ) { route ->
                TransformEntry(
                    route = route,
                    coverUrl = libraryState.albumCoverUrl(route.source, route.albumId),
                    transformKey = albumTransformKey(route.source, route.albumId),
                    controller = controller,
                    showMiniPlayer = showMiniPlayer,
                    miniPlayerHeight = chromeMetrics.miniPlayerHeight,
                    sessionActive = playbackState.source == PlaybackSource.Album(route.albumId),
                ) { bottomInset ->
                    AlbumScreen(
                        route.source,
                        route.albumId,
                        libraryState,
                        actions,
                        playbackState,
                        playbackPosition,
                        collapseOnPause,
                        bottomInset = bottomInset,
                        topBar = { contentColor -> DetailTopBar(actions, contentColor, onCoverPalette) },
                    )
                }
            }
            entry<ArtistRoute>(
                metadata = mapOf(CONTAINER_TRANSFORM_TAG_KEY to ContainerTransformTag.TargetArtist),
            ) { route ->
                TransformEntry(
                    route = route,
                    coverUrl = libraryState.artistCoverUrl(route.source, route.artistId),
                    transformKey = artistTransformKey(route.source, route.artistId),
                    controller = controller,
                    showMiniPlayer = showMiniPlayer,
                    miniPlayerHeight = chromeMetrics.miniPlayerHeight,
                    sessionActive = playbackState.source == PlaybackSource.Artist(route.artistId),
                ) { bottomInset ->
                    ArtistScreen(
                        route.source,
                        route.artistId,
                        libraryState,
                        actions,
                        playbackState,
                        playbackPosition,
                        collapseOnPause,
                        bottomInset = bottomInset,
                        topBar = { contentColor -> DetailTopBar(actions, contentColor, onCoverPalette) },
                    )
                }
            }
            entry<PlaylistRoute> { route -> PaddedScreen(pushedPadding) { PlaylistScreen(route.playlistId) } }
            entry<FolderRoute> { route -> PaddedScreen(pushedPadding) { FolderScreen(route.folderId) } }
            entry<TrackMetadataRoute> { route ->
                PaddedScreen(pushedPadding) {
                    val resolved = (libraryState as? LibraryUiState.Ready)
                        ?.snapshot
                        ?.tracks
                        ?.firstOrNull { it.source == route.source && it.id == route.trackId }
                    // Keep the last resolved track while the library reloads after a save.
                    var track by remember(route) { mutableStateOf(resolved) }
                    LaunchedEffect(resolved) {
                        if (resolved != null) track = resolved
                    }
                    TrackMetadataScreen(track, settings)
                }
            }
            entry<AppearanceRoute> { PaddedScreen(pushedPadding) { AppearanceScreen(settings) } }
            entry<PlaybackSettingsRoute> { PaddedScreen(pushedPadding) { PlaybackSettingsScreen() } }
            entry<LibrarySettingsRoute> { PaddedScreen(pushedPadding) { LibrarySettingsScreen() } }
            entry<ServersRoute> { PaddedScreen(pushedPadding) { ServersScreen(actions) } }
            entry<ServerEditRoute> { route ->
                PaddedScreen(pushedPadding) { ServerEditScreen(route.serverId) }
            }
            entry<AboutRoute> { PaddedScreen(pushedPadding) { AboutScreen() } }
        },
    )
    val sceneState = rememberSceneState(
        entries = entries,
        sceneStrategies = listOf(SinglePaneSceneStrategy()),
        sceneDecoratorStrategies = emptyList(),
        sharedTransitionScope = null,
        onBack = actions.back,
    )
    val navigationEventState = rememberNavigationEventState(sceneState)
    // Block back while a return animates, but never while a predictive gesture is in progress:
    // flipping isBackEnabled mid-gesture makes the system cancel it.
    val backBlockedByTransition = controller.transitionRunning &&
        !controller.transformEntering &&
        navigationEventState.transitionState !is NavigationEventTransitionState.InProgress
    NavigationEventHandler(
        state = navigationEventState,
        isBackEnabled = sceneState.currentScene.previousEntries.isNotEmpty() && !backBlockedByTransition,
        onBackCancelled = controller::notifyGestureCancelled,
        onBackCompleted = actions.back,
    )
    Box(
        modifier = modifier.then(
            // Both scenes are composed while the transform runs; keep assistive technology on a
            // stable, empty tree instead of the intermediate state.
            if (controller.transitionRunning) Modifier.clearAndSetSemantics { } else Modifier,
        ),
    ) {
        CompositionLocalProvider(LocalRContainerTransformNavigationEventState provides navigationEventState) {
            // Container transform pairs get a duration-only scene transition: the transform draws
            // the visuals, while the transition keeps both scenes composed for its length.
            NavDisplay(
                sceneState = sceneState,
                navigationEventState = navigationEventState,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    if (isContainerTransformPair(initialState, targetState)) {
                        rContainerTransformTransition(ContainerTransformSpec.enterDurationMillis)
                    } else {
                        defaultTransitionSpec<NavKey>().invoke(this)
                    }
                },
                popTransitionSpec = {
                    if (isContainerTransformPair(initialState, targetState)) {
                        rContainerTransformTransition(ContainerTransformSpec.returnDurationMillis)
                    } else {
                        defaultPopTransitionSpec<NavKey>().invoke(this)
                    }
                },
                predictivePopTransitionSpec = { swipeEdge ->
                    if (isContainerTransformPair(initialState, targetState)) {
                        rContainerTransformTransition(ContainerTransformSpec.returnDurationMillis)
                    } else {
                        defaultPredictivePopTransitionSpec<NavKey>().invoke(this, swipeEdge)
                    }
                },
            )
        }
        if (controller.transitionRunning) {
            // The scenes under the transform must not receive taps while it animates.
            Box(modifier = Modifier.fillMaxSize().consumeAllPointerInput())
        }
    }
}

@Composable
private fun TransformEntry(
    route: NavKey,
    coverUrl: String?,
    transformKey: ContainerTransformKey,
    controller: RContainerTransformNavController<ContainerTransformKey>,
    showMiniPlayer: Boolean,
    miniPlayerHeight: Dp,
    sessionActive: Boolean,
    content: @Composable (bottomInset: Dp) -> Unit,
) {
    val defaultColors = rememberDefaultCoverColors()
    // Frozen per entry: a palette that arrives after the tap must not change the surface color
    // mid-animation.
    val containerColor = remember(route) {
        cachedCoverBackground(coverUrl) ?: defaultColors?.background
    }
    val bottomInset = if (showMiniPlayer && !sessionActive) miniPlayerHeight else 0.dp
    RContainerTransformNavEntry(
        key = transformKey,
        controller = controller,
        spec = ContainerTransformSpec.withContainerColor(containerColor),
    ) {
        content(bottomInset)
    }
}

internal fun LibraryUiState.albumCoverUrl(source: SourceKey, albumId: String): String? =
    (this as? LibraryUiState.Ready)?.snapshot?.album(source, albumId)?.coverUrl

internal fun LibraryUiState.artistCoverUrl(source: SourceKey, artistId: String): String? =
    (this as? LibraryUiState.Ready)?.snapshot?.artist(source, artistId)?.coverUrl

private const val CONTAINER_TRANSFORM_TAG_KEY = "berry.containerTransformTag"

private enum class ContainerTransformTag { SourceAlbums, SourceArtists, TargetAlbum, TargetArtist }

// Both directions are pairs: the pop must use the duration-only transition as well, otherwise
// Nav3 falls back to the default pop animation on return.
private fun isContainerTransformPair(initial: Scene<NavKey>, target: Scene<NavKey>): Boolean {
    val from = initial.entries.lastOrNull()?.metadata?.get(CONTAINER_TRANSFORM_TAG_KEY)
    val to = target.entries.lastOrNull()?.metadata?.get(CONTAINER_TRANSFORM_TAG_KEY)
    val albumsPair = (from == ContainerTransformTag.SourceAlbums && to == ContainerTransformTag.TargetAlbum) ||
        (from == ContainerTransformTag.TargetAlbum && to == ContainerTransformTag.SourceAlbums)
    val artistsPair = (from == ContainerTransformTag.SourceArtists && to == ContainerTransformTag.TargetArtist) ||
        (from == ContainerTransformTag.TargetArtist && to == ContainerTransformTag.SourceArtists)
    return albumsPair || artistsPair
}

@Composable
private fun PaddedScreen(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding)) {
        content()
    }
}

@Composable
private fun DetailTopBar(actions: AppActions, contentColor: Color?, onCoverPalette: () -> Unit) {
    AppTopBar(
        title = null,
        onBack = actions.back,
        onMenu = null,
        transparent = true,
        contentColor = contentColor,
        onCoverPalette = onCoverPalette,
    )
}

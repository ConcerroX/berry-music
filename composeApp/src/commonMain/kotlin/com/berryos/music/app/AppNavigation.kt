package com.berryos.music.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberNavigationEventState
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationEventHandler
import art.rippledesign.compose.rui.fadeThrough
import art.rippledesign.compose.rui.navigation3.LocalNavTransitionHoldMillis
import art.rippledesign.compose.rui.navigation3.LocalRContainerTransformNavigationEventState
import art.rippledesign.compose.rui.navigation3.RContainerTransformNavController
import art.rippledesign.compose.rui.navigation3.RContainerTransformNavEntry
import art.rippledesign.compose.rui.navigation3.rContainerTransformTransition
import art.rippledesign.compose.rui.navigation3.rNeutralSceneTransition
import art.rippledesign.compose.rui.navigation3.rememberNavTransitionHoldDecorator
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
import com.berryos.music.core.designsystem.coverPageBackground
import com.berryos.music.core.designsystem.rememberDefaultCoverColors
import com.berryos.music.core.designsystem.rememberDisplayCornerRadius
import com.berryos.music.core.designsystem.rememberPlatformScrimColor
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
import com.berryos.music.feature.folder.FolderScreen
import com.berryos.music.feature.home.HomeScreen
import com.berryos.music.feature.metadata.TrackMetadataScreen
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
import com.berryos.music.resources.edit
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TabContent(
    tabs: List<TabItem>,
    selectedIndex: Int,
    transformControllers: List<RContainerTransformNavController<ContainerTransformKey>>,
    actions: AppActions,
    libraryState: LibraryUiState,
    playbackState: PlaybackState,
    playbackPosition: StateFlow<PlaybackPosition>,
    settings: AppSettingsRepository,
    collapseOnPause: Boolean,
    barMetrics: BarMetrics,
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
            transformControllers[index],
            actions,
            libraryState,
            playbackState,
            playbackPosition,
            settings,
            collapseOnPause,
            barMetrics,
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
    controller: RContainerTransformNavController<ContainerTransformKey>,
    actions: AppActions,
    libraryState: LibraryUiState,
    playbackState: PlaybackState,
    playbackPosition: StateFlow<PlaybackPosition>,
    settings: AppSettingsRepository,
    collapseOnPause: Boolean,
    barMetrics: BarMetrics,
    topContentInset: Dp,
    compact: Boolean,
    showMiniPlayer: Boolean,
    bottomContentInset: Dp,
    onCoverPalette: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slidePx = with(LocalDensity.current) { WindowMotionOffset.roundToPx() }
    // MDC's scrolling view is offset by the app bar's current bottom edge; the inset tracks the
    // bar offset so the content fills the space the bar leaves instead of showing a gap. The mini
    // player inset is a container inset so the list (and its overscroll glow) ends above it.
    val rootPadding = PaddingValues(
        start = if (!compact) barMetrics.railWidth else 0.dp,
        top = topContentInset,
        end = 0.dp,
        bottom = bottomContentInset,
    )
    val pushedPadding = PaddingValues(
        start = 0.dp,
        top = topContentInset,
        end = 0.dp,
        bottom = if (showMiniPlayer) barMetrics.miniPlayerHeight else 0.dp,
    )
    val predictiveBack = rememberPredictiveBackState()
    // A back event that arrives while the committed gesture settles. Disabling back instead would
    // fall through to the activity and finish it.
    val pendingBack = remember { mutableStateOf(false) }
    val platformScrim = rememberPlatformScrimColor()
    // True while the edit page slides with the platform activity transition; both scenes use it to
    // paint the window-background strip AOSP's <extend> adds on the edge they vacate.
    val editPageSlideActive = remember { mutableStateOf(false) }
    val editOnTop = remember(backStack) {
        derivedStateOf { backStack.lastOrNull() is TrackMetadataRoute }
    }
    // Only the predictive back preview dims the page underneath; AOSP's activity open/close
    // animations leave the covered window at full brightness.
    val editScrim = remember(predictiveBack, platformScrim) {
        derivedStateOf {
            when {
                // A committed gesture fades the scrim out over the platform's post-commit phase.
                predictiveBack.settling ->
                    platformScrim.copy(alpha = platformScrim.alpha * (1f - predictiveBack.settleProgress))

                predictiveBack.active -> platformScrim

                else -> Color.Transparent
            }
        }
    }
    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberNavTransitionHoldDecorator(),
            rememberEditPageDecorator(
                predictiveBack = predictiveBack,
                scrim = editScrim,
                slideActive = editPageSlideActive,
            ),
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
                    stillInBackStack = { backStack.any { it == route } },
                    showMiniPlayer = showMiniPlayer,
                    miniPlayerHeight = barMetrics.miniPlayerHeight,
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
                    stillInBackStack = { backStack.any { it == route } },
                    showMiniPlayer = showMiniPlayer,
                    miniPlayerHeight = barMetrics.miniPlayerHeight,
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
            entry<TrackMetadataRoute>(
                metadata = mapOf(EDIT_PAGE_TAG_KEY to true),
            ) { route ->
                Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
                    val resolved = (libraryState as? LibraryUiState.Ready)
                        ?.snapshot
                        ?.tracks
                        ?.firstOrNull { it.source == route.source && it.id == route.trackId }
                    // Keep the last resolved track while the library reloads after a save.
                    var track by remember(route) { mutableStateOf(resolved) }
                    LaunchedEffect(resolved) {
                        if (resolved != null) track = resolved
                    }
                    val artistSuggestions = (libraryState as? LibraryUiState.Ready)
                        ?.snapshot
                        ?.artists
                        .orEmpty()
                        .filter { it.source == SourceKey.Local }
                        .map { it.name }
                        .distinct()
                    TrackMetadataScreen(
                        track,
                        settings,
                        artistSuggestions = artistSuggestions,
                        startInLyrics = route.startInLyrics,
                        topBar = { topBarState ->
                            AppTopBar(
                                title = stringResource(Res.string.edit),
                                onBack = actions.back,
                                onMenu = null,
                                onSave = topBarState.onSave,
                                saveEnabled = topBarState.saveEnabled,
                                tabLabels = topBarState.tabLabels,
                                selectedTabIndex = topBarState.selectedTabIndex,
                                selectedTabPosition = topBarState.selectedTabPosition,
                                onTabSelected = topBarState.onTabSelected,
                            )
                        },
                    )
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
    val sourceSceneTag = when (backStack.firstOrNull()) {
        AlbumsRoute -> ContainerTransformTag.SourceAlbums
        ArtistsRoute -> ContainerTransformTag.SourceArtists
        else -> null
    }
    val currentSceneTag = sceneState.currentScene.entries.lastOrNull()
        ?.metadata
        ?.get(CONTAINER_TRANSFORM_TAG_KEY)
    val returningToSource = sourceSceneTag != null && currentSceneTag == sourceSceneTag
    val navigationEventState = rememberNavigationEventState(sceneState)
    // The edit page previews AOSP's cross-activity back animation itself; the scene transition only
    // keeps the incoming page composed underneath for the duration of the gesture.
    LaunchedEffect(navigationEventState, backStack) {
        snapshotFlow { navigationEventState.transitionState }
            .collect { state ->
                if (state is NavigationEventTransitionState.InProgress) {
                    if (editOnTop.value) {
                        predictiveBack.onGestureProgress(state.latestEvent)
                    } else {
                        // A new gesture supersedes a back queued during the edit settle.
                        pendingBack.value = false
                    }
                } else {
                    predictiveBack.onIdle()
                }
            }
    }
    val gestureTargetTag = navigationEventState.backInfo.lastOrNull()
        ?.scene
        ?.entries
        ?.lastOrNull()
        ?.metadata
        ?.get(CONTAINER_TRANSFORM_TAG_KEY)
    SideEffect {
        controller.updateReturningToSource(returningToSource)
        controller.updateGestureReturningToSource(
            sourceSceneTag != null && gestureTargetTag == sourceSceneTag,
        )
    }
    // Block back while a return animates, but never while a predictive gesture is in progress:
    // flipping isBackEnabled mid-gesture makes the system cancel it. The edit settle is not a
    // blocking transition; its back events are queued in pendingBack instead.
    val transformBlocksBack = controller.transitionRunning &&
        !controller.transformEntering &&
        navigationEventState.transitionState !is NavigationEventTransitionState.InProgress
    val backBlockedByTransition = transformBlocksBack
    LaunchedEffect(predictiveBack.settling) {
        if (!predictiveBack.settling && pendingBack.value) {
            pendingBack.value = false
            actions.back()
        }
    }
    NavigationEventHandler(
        state = navigationEventState,
        isBackEnabled = sceneState.currentScene.previousEntries.isNotEmpty() && !backBlockedByTransition,
        onBackCancelled = controller::notifyGestureCancelled,
        onBackCompleted = {
            if (predictiveBack.settling) {
                pendingBack.value = true
            } else {
                // Must happen before the pop: the pop transition spec reads whether the gesture is
                // being settled so it can keep the outgoing scene composed.
                predictiveBack.commit()
                actions.back()
            }
        },
    )
    Box(
        modifier = modifier.then(
            // Both scenes are composed while the transform runs; keep assistive technology on a
            // stable, empty tree instead of the intermediate state.
            if (controller.transitionRunning || predictiveBack.settling) {
                Modifier.clearAndSetSemantics { }
            } else {
                Modifier
            },
        ),
    ) {
        CompositionLocalProvider(
            LocalRContainerTransformNavigationEventState provides navigationEventState,
            LocalNavTransitionHoldMillis provides
                if (predictiveBack.settling) PREDICTIVE_BACK_HOLD_MILLIS else null,
        ) {
            // Container transform pairs get a duration-only scene transition: the transform draws
            // the visuals, while the transition keeps both scenes composed for its length.
            NavDisplay(
                sceneState = sceneState,
                navigationEventState = navigationEventState,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    when {
                        isContainerTransformPair(initialState, targetState) ->
                            rContainerTransformTransition(ContainerTransformSpec.enterDurationMillis)

                        isEditPagePair(initialState, targetState) ->
                            activityTransition(slidePx, forward = true)

                        else -> defaultTransitionSpec<NavKey>().invoke(this)
                    }
                },
                popTransitionSpec = {
                    when {
                        isContainerTransformPair(initialState, targetState) ->
                            rContainerTransformTransition(
                                ContainerTransformSpec.returnDurationMillis + CONTAINER_TRANSFORM_TAIL_MILLIS,
                            )

                        isEditPagePair(initialState, targetState) ->
                            when {
                                predictiveBack.settling ->
                                    rNeutralSceneTransition(PREDICTIVE_BACK_HOLD_MILLIS)

                                // A predictive gesture is drawing both pages itself; a normal pop
                                // enter here would slide the pending scene (and its scrim) again.
                                predictiveBack.active -> rNeutralSceneTransition(0)

                                else -> activityTransition(slidePx, forward = false)
                            }

                        else -> defaultPopTransitionSpec<NavKey>().invoke(this)
                    }
                },
                predictivePopTransitionSpec = { swipeEdge ->
                    when {
                        isContainerTransformPair(initialState, targetState) ->
                            rContainerTransformTransition(
                                ContainerTransformSpec.returnDurationMillis + CONTAINER_TRANSFORM_TAIL_MILLIS,
                            )

                        // The gesture preview is drawn by the entry decorator, not the scene.
                        isEditPagePair(initialState, targetState) -> rNeutralSceneTransition(0)

                        else -> defaultPredictivePopTransitionSpec<NavKey>().invoke(this, swipeEdge)
                    }
                },
            )
        }
        if (controller.transitionRunning || predictiveBack.settling) {
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
    stillInBackStack: () -> Boolean,
    showMiniPlayer: Boolean,
    miniPlayerHeight: Dp,
    sessionActive: Boolean,
    content: @Composable (bottomInset: Dp) -> Unit,
) {
    val defaultColors = rememberDefaultCoverColors()
    // Frozen per entry: a palette that arrives after the tap must not change the surface color
    // mid-animation.
    val containerColor = remember(route) {
        coverPageBackground(coverUrl, defaultColors)
    }
    val displayCornerRadius = rememberDisplayCornerRadius()
    val scrimColor = rememberPlatformScrimColor()
    val bottomInset = if (showMiniPlayer && !sessionActive) miniPlayerHeight else 0.dp
    RContainerTransformNavEntry(
        key = transformKey,
        controller = controller,
        spec = ContainerTransformSpec
            .withContainerColor(containerColor)
            .copy(
                endCornerRadius = displayCornerRadius,
                scrimColor = scrimColor,
            ),
        stillInBackStack = stillInBackStack,
    ) {
        content(bottomInset)
    }
}

internal fun LibraryUiState.albumCoverUrl(source: SourceKey, albumId: String): String? =
    (this as? LibraryUiState.Ready)?.snapshot?.album(source, albumId)?.coverUrl

internal fun LibraryUiState.artistCoverUrl(source: SourceKey, artistId: String): String? =
    (this as? LibraryUiState.Ready)?.snapshot?.artist(source, artistId)?.coverUrl

private const val CONTAINER_TRANSFORM_TAG_KEY = "berry.containerTransformTag"
private const val CONTAINER_TRANSFORM_TAIL_MILLIS = 50

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

private const val EDIT_PAGE_TAG_KEY = "berry.editPage"

private fun Scene<NavKey>.isEditPage(): Boolean = entries.lastOrNull()?.metadata?.get(EDIT_PAGE_TAG_KEY) == true

private fun isEditPagePair(initial: Scene<NavKey>, target: Scene<NavKey>): Boolean =
    initial.isEditPage() || target.isEditPage()

@Composable
private fun rememberEditPageDecorator(
    predictiveBack: PredictiveBackState,
    scrim: State<Color>,
    slideActive: MutableState<Boolean>,
): NavEntryDecorator<NavKey> = remember(predictiveBack, scrim, slideActive) {
    NavEntryDecorator<NavKey> { entry ->
        val editPage = entry.metadata[EDIT_PAGE_TAG_KEY] == true
        val enterExit = LocalNavAnimatedContentScope.current.transition
        val inTransition = enterExit.currentState == EnterExitState.PreEnter ||
            enterExit.targetState == EnterExitState.PostExit
        val sliding = editPage && !predictiveBack.active && !predictiveBack.settling && inTransition
        DisposableEffect(editPage, sliding) {
            if (editPage) slideActive.value = sliding
            onDispose { if (editPage) slideActive.value = false }
        }
        val edgeLayer = rememberGraphicsLayer()
        val displayCornerRadius = rememberDisplayCornerRadius()
        val displayCornerShape = remember(displayCornerRadius) { RoundedCornerShape(displayCornerRadius) }
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        if (slideActive.value) {
                            // AOSP's <extend>: the platform stretches a 1px strip of the window
                            // edge into the vacated area (SurfaceAnimationRunner#edgeExtendWindow).
                            edgeLayer.record { this@drawWithContent.drawContent() }
                            drawEdgeExtension(edgeLayer, left = editPage)
                            drawLayer(edgeLayer)
                        } else {
                            drawContent()
                        }
                    }
                    .graphicsLayer {
                        applyPredictiveBackTransform(predictiveBack, editPage)
                    }
                    .then(
                        // Rounded like the platform's scaled windows; clipped in page coordinates
                        // so the offscreen layer keeps hard straight edges.
                        if (predictiveBack.active) {
                            Modifier.clip(displayCornerShape)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                entry.Content()
            }
            if (!editPage) {
                // The scrim belongs above the incoming page but below the edit page, and it must
                // not move with either of them.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val color = scrim.value
                            if (color.alpha > 0f) {
                                drawRect(color)
                            }
                        },
                )
            }
        }
    }
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
        isTransparent = true,
        contentColor = contentColor,
        onCoverPalette = onCoverPalette,
    )
}

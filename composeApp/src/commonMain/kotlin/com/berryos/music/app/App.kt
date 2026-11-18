package com.berryos.music.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.berryos.music.app.navigation.AlbumsRoute
import com.berryos.music.app.navigation.AppActions
import com.berryos.music.app.navigation.ArtistsRoute
import com.berryos.music.app.navigation.HomeRoute
import com.berryos.music.app.navigation.PlaylistsRoute
import com.berryos.music.app.navigation.SongsRoute
import com.berryos.music.app.navigation.appRouteSerialization
import com.berryos.music.core.auth.AuthController
import com.berryos.music.core.auth.AuthState
import com.berryos.music.core.datastore.AppSettingsRepository
import com.berryos.music.core.designsystem.BerryTheme
import com.berryos.music.core.designsystem.rememberLoadedCoverColors
import com.berryos.music.core.library.LibraryUiState
import com.berryos.music.core.library.filtered
import com.berryos.music.core.model.coverUrlOf
import com.berryos.music.core.playback.PlaybackController
import com.berryos.music.core.playback.PlaybackSource
import com.berryos.music.core.playback.PlaybackState
import com.berryos.music.feature.album.AlbumScreen
import com.berryos.music.feature.artist.ArtistScreen
import com.berryos.music.feature.detail.MediaDetailScreen
import com.berryos.music.feature.playlist.PlaylistScreen
import com.berryos.music.resources.Res
import com.berryos.music.resources.collapse
import com.berryos.music.resources.ic_expand_more
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
import com.berryos.music.resources.queue
import com.berryos.music.resources.tab_albums
import com.berryos.music.resources.tab_artists
import com.berryos.music.resources.tab_home
import com.berryos.music.resources.tab_playlists
import com.berryos.music.resources.tab_songs
import com.berryos.music.resources.track_count
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
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
        val track = playbackState.currentTrack ?: return@remember null
        (visibleLibrary as? LibraryUiState.Ready)?.snapshot?.coverUrlOf(track)
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

internal data class ChromeMetrics(val miniPlayerHeight: Dp, val railWidth: Dp)

@Suppress("LongMethod")
@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun AppShell(
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
    val actions = AppActions(
        open = { route ->
            // The sheet sits above the tab content; leave it before showing another page.
            if (sheetVisible) scope.launch { playbackSheetState.hide() }
            selectedTab.backStack.add(route)
        },
        back = {
            if (selectedTab.backStack.size > 1) {
                selectedTab.backStack.removeLastOrNull()
            } else if (selectedTabIndex != 0) {
                selectedTabIndex = 0
            }
        },
        play = playback::play,
        playNext = playback::playNext,
        togglePlayPause = playback::togglePlayPause,
        next = playback::next,
        previous = playback::previous,
        seekTo = playback::seekTo,
    )
    val queueCoverUrl = playbackState.currentTrack?.let { track ->
        (libraryState as? LibraryUiState.Ready)?.snapshot?.coverUrlOf(track)
    }
    val sheetTopBar: @Composable (Color?) -> Unit = { contentColor ->
        AppTopBar(
            title = null,
            onBack = { scope.launch { playbackSheetState.hide() } },
            onMenu = null,
            isTransparent = true,
            contentColor = contentColor,
            backIcon = Res.drawable.ic_expand_more,
            backLabel = Res.string.collapse,
        )
    }
    ModalBottomSheetLayout(
        sheetState = playbackSheetState,
        sheetContent = {
            Surface(modifier = Modifier.fillMaxSize()) {
                // Only the visible sheet owns a header state; a hidden sheet must not mirror the page.
                if (sheetVisible) {
                    // The player page handles back first (card, then header); this is the fallback.
                    BackHandler { scope.launch { playbackSheetState.hide() } }
                    when (val target = nowPlayingTarget) {
                        is NowPlayingTarget.Album ->
                            AlbumScreen(
                                target.source,
                                target.albumId,
                                libraryState,
                                actions,
                                playbackState,
                                playback.position,
                                collapseOnPause,
                                topBar = sheetTopBar,
                            )

                        is NowPlayingTarget.Artist ->
                            ArtistScreen(
                                target.source,
                                target.artistId,
                                libraryState,
                                actions,
                                playbackState,
                                playback.position,
                                collapseOnPause,
                                topBar = sheetTopBar,
                            )

                        is NowPlayingTarget.Playlist -> PlaylistScreen(target.playlistId)

                        NowPlayingTarget.Queue ->
                            MediaDetailScreen(
                                title = stringResource(Res.string.queue),
                                subtitle = stringResource(Res.string.track_count, playbackState.queue.size),
                                coverUrl = queueCoverUrl,
                                tracks = playbackState.queue,
                                playbackSource = PlaybackSource.Queue,
                                playbackState = playbackState,
                                playbackPosition = playback.position,
                                collapseOnPause = collapseOnPause,
                                actions = actions,
                                entryIds = playbackState.entryIds,
                                topBar = sheetTopBar,
                            )

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
            playback.position,
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

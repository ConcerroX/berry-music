package com.berryos.music.app.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclassesOfSealed

@Serializable
sealed interface AppRoute : NavKey

@Serializable
data object HomeRoute : AppRoute

@Serializable
data object AlbumsRoute : AppRoute

@Serializable
data object SongsRoute : AppRoute

@Serializable
data object PlaylistsRoute : AppRoute

@Serializable
data object ArtistsRoute : AppRoute

@Serializable
data object SearchRoute : AppRoute

@Serializable
data object SettingsRoute : AppRoute

@Serializable
data class AlbumRoute(val albumId: String) : AppRoute

@Serializable
data class ArtistRoute(val artistId: String) : AppRoute

@Serializable
data class PlaylistRoute(val playlistId: String) : AppRoute

@Serializable
data class FolderRoute(val folderId: String) : AppRoute

@Serializable
data object AppearanceRoute : AppRoute

@Serializable
data object PlaybackSettingsRoute : AppRoute

@Serializable
data object LibrarySettingsRoute : AppRoute

@Serializable
data object ServersRoute : AppRoute

@Serializable
data class ServerEditRoute(val serverId: String? = null) : AppRoute

@Serializable
data object AboutRoute : AppRoute

@OptIn(ExperimentalSerializationApi::class)
val appRouteSerialization: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<AppRoute>()
        }
    }
}

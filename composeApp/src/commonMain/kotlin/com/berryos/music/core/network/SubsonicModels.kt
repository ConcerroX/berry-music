package com.berryos.music.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubsonicEnvelope(@SerialName("subsonic-response") val response: SubsonicResponse)

@Serializable
data class SubsonicResponse(
    val status: String = "",
    @SerialName("albumList2") val albumList: SubsonicAlbumList? = null,
    val album: SubsonicAlbumDetail? = null,
    val error: SubsonicError? = null,
)

@Serializable
data class SubsonicAlbumList(val album: List<SubsonicAlbum> = emptyList())

@Serializable
data class SubsonicAlbum(
    val id: String,
    val name: String = "",
    val artist: String = "",
    val artistId: String = "",
    val songCount: Int = 0,
    val year: Int = 0,
)

@Serializable
data class SubsonicAlbumDetail(val id: String = "", val song: List<SubsonicSong> = emptyList())

@Serializable
data class SubsonicSong(
    val id: String,
    val title: String = "",
    val artist: String = "",
    val artistId: String = "",
    val album: String = "",
    val albumId: String = "",
    val duration: Int = 0,
    val track: Int = 0,
    val year: Int = 0,
)

@Serializable
data class SubsonicError(val code: Int = 0, val message: String = "")

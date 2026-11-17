package com.berryos.music.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.URLBuilder

class SubsonicClient(private val client: HttpClient, private val baseUrl: String) {
    suspend fun albumPage(username: String, jwt: String, offset: Int, size: Int): List<SubsonicAlbum> =
        request(username, jwt, "getAlbumList2") {
            parameter("type", "alphabeticalByName")
            parameter("size", size)
            parameter("offset", offset)
        }?.albumList?.album.orEmpty()

    suspend fun albumSongs(username: String, jwt: String, albumId: String): List<SubsonicSong> =
        request(username, jwt, "getAlbum") {
            parameter("id", albumId)
        }?.album?.song.orEmpty()

    fun streamUrl(username: String, jwt: String, songId: String): String {
        val builder = URLBuilder("$baseUrl/rest/stream")
        builder.parameters.append("id", songId)
        builder.appendAuth(username, jwt)
        return builder.buildString()
    }

    fun coverUrl(username: String, jwt: String, albumId: String): String {
        val builder = URLBuilder("$baseUrl/rest/getCoverArt")
        builder.parameters.append("id", albumId)
        builder.appendAuth(username, jwt)
        return builder.buildString()
    }

    private fun URLBuilder.appendAuth(username: String, jwt: String) {
        parameters.append("u", username)
        parameters.append("jwt", jwt)
        parameters.append("v", API_VERSION)
        parameters.append("c", CLIENT_NAME)
    }

    private fun HttpRequestBuilder.appendAuth(username: String, jwt: String) {
        parameter("u", username)
        parameter("jwt", jwt)
        parameter("v", API_VERSION)
        parameter("c", CLIENT_NAME)
    }

    private suspend fun request(
        username: String,
        jwt: String,
        endpoint: String,
        configure: HttpRequestBuilder.() -> Unit,
    ): SubsonicResponse? {
        val response = client.get("$baseUrl/rest/$endpoint") {
            appendAuth(username, jwt)
            parameter("f", "json")
            configure()
        }.body<SubsonicEnvelope>().response
        return response.takeIf { it.status == STATUS_OK }
    }

    private companion object {
        const val API_VERSION = "1.16.1"
        const val CLIENT_NAME = "BerryMusic"
        const val STATUS_OK = "ok"
    }
}

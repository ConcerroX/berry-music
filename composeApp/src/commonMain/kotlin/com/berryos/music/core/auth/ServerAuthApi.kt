package com.berryos.music.core.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

class ServerAuthException(val statusCode: Int) : Exception("Server auth failed ($statusCode)")

class ServerAuthApi(private val client: HttpClient, private val baseUrl: String) {
    suspend fun exchange(accessToken: String, name: String? = null, avatar: String? = null): ServerAccount {
        val response = client.post("$baseUrl/auth/logto/exchange") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            if (!name.isNullOrBlank() || !avatar.isNullOrBlank()) {
                contentType(ContentType.Application.Json)
                setBody(
                    ExchangeRequest(
                        name = name?.takeIf { it.isNotBlank() },
                        avatar = avatar?.takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
        if (!response.status.isSuccess()) {
            throw ServerAuthException(response.status.value)
        }
        val body = response.body<LogtoExchangeResponse>()
        return ServerAccount(
            id = body.id,
            username = body.username,
            name = body.name,
            avatarUrl = body.avatar,
            isAdmin = body.isAdmin,
            token = body.token,
            subsonicJwt = body.subsonicJwt,
        )
    }

    @Serializable
    private data class ExchangeRequest(val name: String? = null, val avatar: String? = null)
}

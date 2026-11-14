package com.berryos.music.core.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServerAuthApiTest {
    @Test
    fun exchangeParsesServerSession(): Unit = runBlocking {
        val client = clientWithResponse(
            status = HttpStatusCode.OK,
            body = """
                {
                  "token": "navidrome-token",
                  "subsonicJwt": "subsonic-token",
                  "id": "user-1",
                  "username": "alice",
                  "name": "Alice",
                  "isAdmin": true
                }
            """.trimIndent(),
        )
        try {
            val account = ServerAuthApi(client, "https://music.example.com").exchange("logto-token")
            assertEquals("alice", account.username)
            assertEquals("Alice", account.displayName)
            assertEquals("navidrome-token", account.token)
            assertEquals("subsonic-token", account.subsonicJwt)
        } finally {
            client.close()
        }
    }

    @Test
    fun exchangeReportsServerFailure(): Unit = runBlocking {
        val client = clientWithResponse(
            status = HttpStatusCode.ServiceUnavailable,
            body = """{"error":"not configured"}""",
        )
        try {
            assertFailsWith<ServerAuthException> {
                ServerAuthApi(client, "https://music.example.com").exchange("logto-token")
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun exchangeSendsDisplayName(): Unit = runBlocking {
        var requestBody: String? = null
        val client = HttpClient(
            MockEngine { request ->
                requestBody = (request.body as TextContent).text
                respond(
                    content = """{"token":"t","id":"user-1","username":"alice"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        try {
            ServerAuthApi(client, "https://music.example.com").exchange("logto-token", "Alice")
            assertEquals("""{"name":"Alice"}""", requestBody)
        } finally {
            client.close()
        }
    }

    private fun clientWithResponse(status: HttpStatusCode, body: String): HttpClient = HttpClient(
        MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    ) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}

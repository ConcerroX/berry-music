package com.berryos.music.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SubsonicClientTest {
    @Test
    fun albumPageParsesAlbums(): Unit = runBlocking {
        val client = clientWithBody(
            """
            {"subsonic-response":{"status":"ok","albumList2":{"album":[
              {"id":"a1","name":"Album","artist":"Artist","artistId":"ar1","songCount":2,"year":2020}
            ]}}}
            """.trimIndent(),
        )
        try {
            val albums = SubsonicClient(client, "https://music.example.com").albumPage("alice", "jwt", 0, 500)
            assertEquals(1, albums.size)
            assertEquals("a1", albums.first().id)
            assertEquals(2, albums.first().songCount)
        } finally {
            client.close()
        }
    }

    @Test
    fun albumSongsParsesSongs(): Unit = runBlocking {
        val client = clientWithBody(
            """
            {"subsonic-response":{"status":"ok","album":{"id":"a1","song":[
              {"id":"s1","title":"Song","artist":"Artist","artistId":"ar1","album":"Album","albumId":"a1","duration":61,"track":1,"year":2020}
            ]}}}
            """.trimIndent(),
        )
        try {
            val songs = SubsonicClient(client, "https://music.example.com").albumSongs("alice", "jwt", "a1")
            assertEquals(1, songs.size)
            assertEquals(61, songs.first().duration)
        } finally {
            client.close()
        }
    }

    @Test
    fun streamUrlCarriesIdAndToken() {
        val url = SubsonicClient(HttpClient(MockEngine { respond("") }), "https://music.example.com")
            .streamUrl("alice", "token", "s1")
        assertEquals(true, url.startsWith("https://music.example.com/rest/stream?"))
        assertEquals(true, url.contains("id=s1"))
        assertEquals(true, url.contains("jwt=token"))
    }

    private fun clientWithBody(body: String): HttpClient = HttpClient(
        MockEngine {
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    ) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}

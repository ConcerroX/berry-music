package com.berryos.music.core.plugin.contract

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PluginJsonParserTest {

    private val parser = PluginJsonParser(
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        },
    )

    @Test
    fun parsesAliasedSongFieldsAndSanitizesMaps() {
        val results = parser.parseSongResults(
            rawJson = """
                {
                  "items": [
                    {
                      "songId": "42",
                      "songName": "Title",
                      "singer": ["A", "B"],
                      "albumName": "Album",
                      "durationMs": 1234,
                      "year": "2020",
                      "trackerNumber": "3",
                      "coverUrl": "https://example.com/c.jpg",
                      "fields": {"title": "Title", "genre": "Pop", "artist": "", "bogus": "x"},
                      "internal": {"platform_id": "p1", "empty": ""}
                    }
                  ]
                }
            """.trimIndent(),
            pluginId = "plugin",
            pluginName = "Plugin",
        )

        assertEquals(1, results.size)
        val song = results.single()
        assertEquals("42", song.id)
        assertEquals("Title", song.title)
        assertEquals("A/B", song.artist)
        assertEquals("Album", song.album)
        assertEquals(1234L, song.duration)
        assertEquals("2020", song.date)
        assertEquals("3", song.trackNumber)
        assertEquals("https://example.com/c.jpg", song.picUrl)
        assertEquals(mapOf("title" to "Title", "genre" to "Pop"), song.fields)
        assertEquals(mapOf("platform_id" to "p1", "empty" to ""), song.internal)
    }

    @Test
    fun dropsCoverResultsMissingJudgmentMetadataForApi4() {
        val results = parser.parseCoverResults(
            rawJson = """
                [
                  {"title": "T", "artist": "A", "album": "Al", "year": "2024", "picUrl": "https://x/1.jpg"},
                  {"title": "T", "artist": "A", "album": "Al", "picUrl": "https://x/2.jpg"}
                ]
            """.trimIndent(),
            pluginId = "plugin",
            pluginName = "Plugin",
            enforceApi4Contract = true,
        )

        assertEquals(1, results.size)
        assertEquals("https://x/1.jpg", results.single().picUrl)
    }

    @Test
    fun parsesStructuredLyricsCandidate() {
        val fallback = com.berryos.music.core.plugin.model.SongSearchResult(
            id = "song",
            pluginId = "plugin",
            pluginName = "Plugin",
        )
        val candidates = parser.parseLyricsCandidates(
            rawJson = """
                [
                  {
                    "type": "structured",
                    "tags": {"ti": "Song", "ar": "Artist", "al": "Album", "date": "2024"},
                    "original": [[0, 1000, [[0, 500, "Hello"], [500, 1000, "world"]]]],
                    "translated": [[0, 1000, "Translation"]],
                    "romanization": null,
                    "timing": "Word",
                    "language": "en",
                    "bodyDur": "1s"
                  }
                ]
            """.trimIndent(),
            pluginId = "plugin",
            pluginName = "Plugin",
            fallbackSong = fallback,
            enforceApi4Contract = true,
        )

        val lyrics = candidates.single().lyrics
        assertEquals("Song", candidates.single().song.title)
        assertEquals("Word", lyrics.timing)
        assertEquals("en", lyrics.language)
        assertEquals("1s", lyrics.bodyDur)
        assertEquals(2, lyrics.original.single().words.size)
        assertEquals("Translation", lyrics.translated?.single()?.words?.single()?.text)
    }

    @Test
    fun returnsEmptyForNullLyrics() {
        val fallback = com.berryos.music.core.plugin.model.SongSearchResult(
            id = "song",
            pluginId = "plugin",
            pluginName = "Plugin",
        )
        val candidates = parser.parseLyricsCandidates(
            rawJson = "null",
            pluginId = "plugin",
            pluginName = "Plugin",
            fallbackSong = fallback,
        )

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun ignoresUnknownPayloadType() {
        val fallback = com.berryos.music.core.plugin.model.SongSearchResult(
            id = "song",
            pluginId = "plugin",
            pluginName = "Plugin",
        )
        val candidates = parser.parseLyricsCandidates(
            rawJson = """{"type": "unknown"}""",
            pluginId = "plugin",
            pluginName = "Plugin",
            fallbackSong = fallback,
        )

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun parsesRawLrcPayload() {
        val fallback = com.berryos.music.core.plugin.model.SongSearchResult(
            id = "song",
            pluginId = "plugin",
            pluginName = "Plugin",
        )
        val candidates = parser.parseLyricsCandidates(
            rawJson = """
                {
                  "type": "rawPlainLrc",
                  "tags": {"ti": "Song", "ar": "Artist", "al": "Album", "date": "2024"},
                  "rawPlainLrc": "[00:00.00]line"
                }
            """.trimIndent(),
            pluginId = "plugin",
            pluginName = "Plugin",
            fallbackSong = fallback,
            enforceApi4Contract = true,
        )

        val lyrics = candidates.single().lyrics
        assertEquals(com.berryos.music.core.plugin.model.LyricsPayloadType.RAW_PLAIN_LRC, lyrics.payloadType)
        assertEquals("[00:00.00]line", lyrics.rawPlainLrc)
    }
}

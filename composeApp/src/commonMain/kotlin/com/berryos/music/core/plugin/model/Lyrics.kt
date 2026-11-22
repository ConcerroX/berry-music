package com.berryos.music.core.plugin.model

import kotlinx.serialization.Serializable

// Ported from Lyrico (Apache License 2.0): com.lonx.lyrico.data.model.lyrics.Lyrics

@Serializable
data class LyricsWord(
    val start: Long,
    val end: Long,
    val text: String,
    val ruby: List<LyricsRubySyllable> = emptyList(),
)

@Serializable
data class LyricsRubySyllable(val start: Long?, val end: Long?, val text: String)

@Serializable
data class LyricsLine(
    val start: Long,
    val end: Long,
    val words: List<LyricsWord>,
    val extensions: Map<String, String> = emptyMap(),
)

/**
 * Structured payload performer, mapped to TTML head `<ttm:agent>`.
 */
@Serializable
data class LyricsAgentEntry(val id: String, val type: String? = null, val name: String? = null)

/**
 * Structured payload head metadata element node, mapped to a TTML `<head>` element.
 */
@Serializable
data class LyricsMetadataElement(
    val name: String,
    val namespace: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val text: String? = null,
    val children: List<LyricsMetadataElement> = emptyList(),
)

@Serializable
data class LyricsResult(
    val tags: Map<String, String>,
    val original: List<LyricsLine>,
    val translated: List<LyricsLine>?,
    val romanization: List<LyricsLine>?,
    val payloadType: LyricsPayloadType = LyricsPayloadType.STRUCTURED,
    val isWordByWord: Boolean = true,
    val rawPlainLrc: String = "",
    val rawVerbatimLrc: String = "",
    val rawEnhancedLrc: String = "",
    val rawTtml: String = "",
    val rawMultiPersonEnhancedLrc: String = "",
    val agents: List<LyricsAgentEntry> = emptyList(),
    val metadata: List<LyricsMetadataElement> = emptyList(),
    val timing: String = "",
    val language: String = "",
    val translatedLang: String = "",
    val romanizationLang: String = "",
    val bodyDur: String = "",
)

@Serializable
data class LyricsCandidateResult(val song: SongSearchResult, val lyrics: LyricsResult)

@Serializable
enum class LyricsPayloadType {
    STRUCTURED,
    RAW_PLAIN_LRC,
    RAW_VERBATIM_LRC,
    RAW_ENHANCED_LRC,
    RAW_TTML,
    RAW_MULTI_PERSON_ENHANCED_LRC,
}

fun List<LyricsLine>.isWordByWord(): Boolean = any { it.words.size > 1 }

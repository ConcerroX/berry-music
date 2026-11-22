package com.berryos.music.core.plugin.contract

import com.berryos.music.core.plugin.lyrics.TtmlTime
import com.berryos.music.core.plugin.model.LyricsAgentEntry
import com.berryos.music.core.plugin.model.LyricsCandidateResult
import com.berryos.music.core.plugin.model.LyricsLine
import com.berryos.music.core.plugin.model.LyricsMetadataElement
import com.berryos.music.core.plugin.model.LyricsPayloadType
import com.berryos.music.core.plugin.model.LyricsResult
import com.berryos.music.core.plugin.model.LyricsRubySyllable
import com.berryos.music.core.plugin.model.LyricsWord
import com.berryos.music.core.plugin.model.SongSearchResult
import com.berryos.music.core.plugin.model.isWordByWord
import com.berryos.music.core.plugin.model.sanitizePluginInternal
import com.berryos.music.core.plugin.model.sanitizeStandardFields
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

// Ported from Lyrico (Apache License 2.0): com.lonx.lyrico.plugin.source.PluginJsonParser
class PluginJsonParser(
    private val json: Json
) {
    fun parseSongResults(
        rawJson: String,
        pluginId: String,
        pluginName: String
    ): List<SongSearchResult> {
        return parseSongResultItems(
            rawJson = rawJson,
            pluginId = pluginId,
            pluginName = pluginName,
            requireId = true
        )
    }

    fun parseCoverResults(
        rawJson: String,
        pluginId: String,
        pluginName: String,
        enforceApi4Contract: Boolean = false
    ): List<SongSearchResult> {
        return parseSongResultItems(
            rawJson = rawJson,
            pluginId = pluginId,
            pluginName = pluginName,
            requireId = false,
            enforceCoverJudgmentMetadata = enforceApi4Contract
        )
    }

    private fun parseSongResultItems(
        rawJson: String,
        pluginId: String,
        pluginName: String,
        requireId: Boolean,
        enforceCoverJudgmentMetadata: Boolean = false
    ): List<SongSearchResult> {
        val root = json.parseToJsonElement(rawJson)
        val items = when (root) {
            is JsonArray -> root
            is JsonObject -> root.array("items", "results", "songs", "data") ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }

        return items.mapIndexedNotNull { index, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            val coverUrl = obj.string("picUrl", "coverUrl", "cover_url", "artworkUrl").orEmpty()
            val id = obj.string("id", "songId", "trackId")
                ?: if (requireId) return@mapIndexedNotNull null else coverUrl.ifBlank {
                    "$pluginId:cover:$index"
                }
            val title = obj.string("title", "name", "songName").orEmpty()
            val artist = obj.string("artist", "artists", "singer").orEmpty()
            val album = obj.string("album", "albumName").orEmpty()
            val date = obj.string("year", "date", "releaseDate", "release_date").orEmpty()
            if (enforceCoverJudgmentMetadata && listOf(
                    title,
                    artist,
                    album,
                    date,
                    coverUrl
                ).any { it.isBlank() }
            ) {
                return@mapIndexedNotNull null
            }
            val duration = obj.long("duration", "durationMs", "duration_ms") ?: 0L
            val fields = obj.stringMap("fields", "metadata").orEmpty().sanitizeStandardFields()
            val internal = obj.stringMap("internal").orEmpty().sanitizePluginInternal()

            SongSearchResult(
                id = id,
                pluginId = pluginId,
                pluginName = pluginName,
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                date = date,
                trackNumber = obj.string("trackNumber", "trackerNumber", "track_number").orEmpty(),
                picUrl = coverUrl,
                fields = fields,
                internal = internal
            )
        }
    }

    fun parseLyricsCandidates(
        rawJson: String,
        pluginId: String,
        pluginName: String,
        fallbackSong: SongSearchResult,
        enforceApi4Contract: Boolean = false
    ): List<LyricsCandidateResult> {
        val root = json.parseToJsonElement(rawJson)
        if (root is JsonNull) return emptyList()

        val candidateElements = when (root) {
            is JsonArray -> root.toList()
            is JsonObject -> root.array("items", "results", "candidates")?.toList()
                ?: listOf(root)
            else -> listOf(root)
        }

        return candidateElements.mapIndexedNotNull { index, element ->
            val obj = element as? JsonObject
            val tags = obj?.stringMap("tags").orEmpty()
            if (enforceApi4Contract) {
                val judgmentFields = listOf(
                    tags["ti"],
                    tags["ar"],
                    tags["al"],
                    tags["date"]
                )
                if (judgmentFields.any { it.isNullOrBlank() }) {
                    return@mapIndexedNotNull null
                }
            }
            val lyrics = parseLyrics(element.toString()) ?: return@mapIndexedNotNull null
            val candidateId = if (candidateElements.size == 1) {
                fallbackSong.id
            } else {
                "${fallbackSong.id}:lyrics:$index"
            }

            LyricsCandidateResult(
                song = SongSearchResult(
                    id = candidateId,
                    pluginId = pluginId,
                    pluginName = pluginName,
                    title = tags["ti"] ?: fallbackSong.title,
                    artist = tags["ar"] ?: fallbackSong.artist,
                    album = tags["al"] ?: fallbackSong.album,
                    duration = fallbackSong.duration,
                    date = tags["date"] ?: fallbackSong.date,
                    trackNumber = fallbackSong.trackNumber,
                    picUrl = fallbackSong.picUrl,
                    fields = fallbackSong.fields,
                    internal = fallbackSong.internal
                ),
                lyrics = lyrics
            )
        }
    }

    fun parseLyrics(rawJson: String): LyricsResult? {
        val root = json.parseToJsonElement(rawJson)
        if (root is JsonNull) return null

        if (root is JsonPrimitive) {
            val lrc = root.contentOrNull.orEmpty()
            return lrc.takeIf { it.isNotBlank() }?.toRawLyricsResult()
        }

        val obj = root as? JsonObject ?: return null
        if (obj.boolean("notFound") == true) return null

        val tags = obj.stringMap("tags").orEmpty()
        val payloadType = obj.primitiveString("type")
            ?.toLyricsPayloadType()
            ?: LyricsPayloadType.STRUCTURED

        val rawPlain = obj.primitiveString(
            "rawPlainLrc",
            "raw_plain_lrc",
            "plainLrc",
            "plain_lrc",
            "lrc",
            "originalLrc",
            "original_lrc"
        ).orEmpty()

        val rawOriginal = obj.primitiveString("original").orEmpty()
        val verbatim = obj.primitiveString("rawVerbatimLrc", "raw_verbatim_lrc").orEmpty()
        val enhanced = obj.primitiveString("rawEnhancedLrc", "raw_enhanced_lrc").orEmpty()
        val ttml = obj.primitiveString("rawTtml", "raw_ttml").orEmpty()
        val multiPerson = obj.primitiveString(
            "rawMultiPersonEnhancedLrc",
            "raw_multi_person_enhanced_lrc"
        ).orEmpty()

        if (payloadType != LyricsPayloadType.STRUCTURED) {
            return obj.toRawLyricsResult(
                type = payloadType,
                tags = tags,
                rawPlain = rawPlain,
                rawOriginal = rawOriginal,
                rawVerbatim = verbatim,
                rawEnhanced = enhanced,
                rawTtml = ttml,
                rawMultiPerson = multiPerson
            )
        }

        val originalLines = obj.array("original", "lines").parseCompactWordLines()

        val translatedLines = obj.array(
            "translated",
            "translation",
            "translations"
        ).parseCompactTextLines().takeIf { it.isNotEmpty() }

        // Romanization accepts word-level entries (same shape as original) or whole-line text,
        // so older plugins keep working.
        val romanizationLines = obj.array(
            "romanization",
            "romanized",
            "roma"
        ).parseCompactWordLines().takeIf { it.isNotEmpty() }

        // Structured extension: performers (written to TTML head <ttm:agent>); empty for older plugins.
        val agents = obj.array("agents").parseAgentEntries()

        // Structured extension: head metadata element tree.
        // Three-way rule: official key + official shape -> keep as spec; unofficial key -> pass through;
        // official key + wrong shape -> drop the node.
        val metadata = obj.array("metadata").parseMetadataElements()

        // Structured extension: root <tt> attributes and track language codes (used when writing TTML).
        // timing = word-level flag (root itunes:timing); language = original language (root xml:lang);
        // translatedLang / romanizationLang = translation/romanization track codes (BCP47).
        val timing = obj.string("timing").orEmpty()
        val language = obj.string("language").orEmpty()
        val translatedLang = obj.string("translatedLang", "translated_lang").orEmpty()
        val romanizationLang = obj.string("romanizationLang", "romanization_lang").orEmpty()
        val rawBodyDur = obj.string("bodyDur", "body_dur").orEmpty()
        val bodyDur = rawBodyDur.takeIf(TtmlTime::isValid).orEmpty()

        if (originalLines.isEmpty()) {
            return null
        }

        val isWordByWord =  originalLines.isWordByWord()

        return LyricsResult(
            tags = tags,
            original = originalLines,
            translated = translatedLines,
            romanization = romanizationLines,
            payloadType = LyricsPayloadType.STRUCTURED,
            isWordByWord = isWordByWord,
            agents = agents,
            metadata = metadata,
            timing = timing,
            language = language,
            translatedLang = translatedLang,
            romanizationLang = romanizationLang,
            bodyDur = bodyDur
        )
    }

    private fun String.toRawLyricsResult(): LyricsResult {
        return LyricsResult(
            tags = emptyMap(),
            original = emptyList(),
            translated = null,
            romanization = null,
            payloadType = LyricsPayloadType.RAW_PLAIN_LRC,
            isWordByWord = false,
            rawPlainLrc = this
        )
    }
}

private fun JsonObject.toRawLyricsResult(
    type: LyricsPayloadType,
    tags: Map<String, String>,
    rawPlain: String,
    rawOriginal: String,
    rawVerbatim: String,
    rawEnhanced: String,
    rawTtml: String,
    rawMultiPerson: String
): LyricsResult? {
    val plain = rawPlain.ifBlank { rawOriginal }
    val hasDeclaredRaw = when (type) {
        LyricsPayloadType.RAW_PLAIN_LRC -> plain.isNotBlank()
        LyricsPayloadType.RAW_VERBATIM_LRC -> rawVerbatim.isNotBlank()
        LyricsPayloadType.RAW_ENHANCED_LRC -> rawEnhanced.isNotBlank()
        LyricsPayloadType.RAW_TTML -> rawTtml.isNotBlank()
        LyricsPayloadType.RAW_MULTI_PERSON_ENHANCED_LRC -> rawMultiPerson.isNotBlank()
        LyricsPayloadType.STRUCTURED -> false
    }

    if (!hasDeclaredRaw) return null

    return LyricsResult(
        tags = tags,
        original = emptyList(),
        translated = null,
        romanization = null,
        payloadType = type,
        isWordByWord = false,
        rawPlainLrc = plain,
        rawVerbatimLrc = rawVerbatim,
        rawEnhancedLrc = rawEnhanced,
        rawTtml = rawTtml,
        rawMultiPersonEnhancedLrc = rawMultiPerson
    )
}

private fun String.toLyricsPayloadType(): LyricsPayloadType? {
    return when (trim()) {
        "structured", "STRUCTURED" -> LyricsPayloadType.STRUCTURED
        "rawPlainLrc", "raw_plain_lrc", "RAW_PLAIN_LRC", "plainLrc", "plain_lrc", "lrc" ->
            LyricsPayloadType.RAW_PLAIN_LRC
        "rawVerbatimLrc", "raw_verbatim_lrc", "RAW_VERBATIM_LRC" ->
            LyricsPayloadType.RAW_VERBATIM_LRC
        "rawEnhancedLrc", "raw_enhanced_lrc", "RAW_ENHANCED_LRC" ->
            LyricsPayloadType.RAW_ENHANCED_LRC
        "rawTtml", "raw_ttml", "RAW_TTML", "ttml" ->
            LyricsPayloadType.RAW_TTML
        "rawMultiPersonEnhancedLrc", "raw_multi_person_enhanced_lrc", "RAW_MULTI_PERSON_ENHANCED_LRC" ->
            LyricsPayloadType.RAW_MULTI_PERSON_ENHANCED_LRC
        else -> null
    }
}

/**
 * Compact word-level format for original / romanization:
 *
 * [
 *   [lineStart, lineEnd, [[wordStart, wordEnd, text, ruby?], ...]]
 * ]
 *
 * Whole-line text is also accepted:
 *
 * [
 *   [lineStart, lineEnd, text]
 * ]
 *
 * An optional fourth element carries line-level extensions keyed by namespaced TTML attribute names:
 *
 * [
 *   [lineStart, lineEnd, words, {"ttm:agent": "v1", "itunes:song-part": "Verse"}]
 * ]
 *
 * Values must be strings (JsonPrimitive); non-string values and non-object fourth elements are ignored.
 */
private fun JsonArray?.parseCompactWordLines(): List<LyricsLine> {
    return this?.mapNotNull { element ->
        val line = element as? JsonArray ?: return@mapNotNull null
        val start = line.longAt(0) ?: return@mapNotNull null
        val end = line.longAt(1) ?: start
        val wordsArray = line.arrayAt(2)
        val text = line.stringAt(2)
        // Fourth element: line-level extensions (ttm:agent / itunes:song-part, ...).
        // Only unprefixed, ttm: and itunes: keys are kept; other prefixes cannot be declared in the
        // output root and would produce invalid XML, so they are filtered here.
        val extensions = line.objectAt(3)
            ?.parseStringMap()
            ?.filterKeys { key -> key.substringBefore(':', "").let { it == "" || it == "ttm" || it == "itunes" } }
            .orEmpty()

        val words = when {
            wordsArray != null -> {
                wordsArray.mapNotNull { wordElement ->
                    val word = wordElement as? JsonArray ?: return@mapNotNull null
                    val wordStart = word.longAt(0) ?: start
                    val wordEnd = word.longAt(1) ?: end
                    val wordText = word.stringAt(2).orEmpty()

                    if (wordText.isEmpty()) {
                        return@mapNotNull null
                    }

                    LyricsWord(
                        start = wordStart,
                        end = wordEnd,
                        text = wordText,
                        ruby = word.arrayAt(3).parseRubySyllables()
                    )
                }
            }

            !text.isNullOrEmpty() -> {
                listOf(
                    LyricsWord(
                        start = start,
                        end = end,
                        text = text
                    )
                )
            }

            else -> emptyList()
        }

        if (words.isEmpty()) return@mapNotNull null

        LyricsLine(
            start = start,
            end = end,
            words = words,
            extensions = extensions
        )
    }.orEmpty()
}

/**
 * Ruby uses the same fixed tuple as words: [[startMs?, endMs?, text], ...].
 * Missing times are JSON null; entries without a valid syllable are treated as "no ruby".
 */
private fun JsonArray?.parseRubySyllables(): List<LyricsRubySyllable> {
    return this?.mapNotNull { element ->
        val syllable = element as? JsonArray ?: return@mapNotNull null
        val text = syllable.stringAt(2)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        LyricsRubySyllable(
            start = syllable.longAt(0),
            end = syllable.longAt(1),
            text = text
        )
    }.orEmpty()
}

/**
 * Compact translation format (whole-line text only; translation has no word-level semantics):
 *
 * [
 *   [lineStart, lineEnd, text]
 * ]
 */
private fun JsonArray?.parseCompactTextLines(): List<LyricsLine> {
    return this?.mapNotNull { element ->
        val line = element as? JsonArray ?: return@mapNotNull null
        val start = line.longAt(0) ?: return@mapNotNull null
        val end = line.longAt(1) ?: start
        val text = line.stringAt(2).orEmpty()

        if (text.isBlank()) return@mapNotNull null

        LyricsLine(
            start = start,
            end = end,
            words = listOf(
                LyricsWord(
                    start = start,
                    end = end,
                    text = text
                )
            )
        )
    }.orEmpty()
}

private fun JsonArray.longAt(index: Int): Long? {
    return (getOrNull(index) as? JsonPrimitive)?.let { primitive ->
        primitive.longOrNull ?: primitive.contentOrNull?.toLongOrNull()
    }
}

private fun JsonArray.stringAt(index: Int): String? {
    return (getOrNull(index) as? JsonPrimitive)?.contentOrNull
}

private fun JsonArray.arrayAt(index: Int): JsonArray? {
    return getOrNull(index) as? JsonArray
}

private fun JsonArray.objectAt(index: Int): JsonObject? {
    return getOrNull(index) as? JsonObject
}

/**
 * Compact agents format (array of objects):
 *
 * [
 *   { "id": "v1", "type": "person", "name": "Artist A" },
 *   { "id": "v1000", "type": "group" }
 * ]
 *
 * id is required (the entry is dropped without it); type/name are optional.
 * type is passed through as a raw string so no information is lost.
 */
private fun JsonArray?.parseAgentEntries(): List<LyricsAgentEntry> {
    return this?.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val id = obj.primitiveString("id") ?: return@mapNotNull null
        LyricsAgentEntry(
            id = id,
            type = obj.primitiveString("type"),
            name = obj.primitiveString("name")
        )
    }.orEmpty()
}

/*
 * Metadata element tree parsing follows a three-way rule:
 *
 * 1. official key + official shape -> keep as specified;
 * 2. unofficial key -> pass the whole element tree through;
 * 3. official key + wrong shape -> drop the subtree.
 *
 * Official keys are case-sensitive: AMLL element names are lowercase, so camelCase names
 * (e.g. songWriters) take the pass-through branch without normalization.
 */

// AMLL TTML head element names (lowercase, case-sensitive)
private const val META_NAME_SONGWRITERS = "songwriters"
private const val META_NAME_SONGWRITER = "songwriter"

// Official keys that already have dedicated fields (translated/romanization/agents) and must not repeat.
private val META_NAMES_DUPLICATED = setOf("translations", "transliterations", "ttm:agent")

private fun JsonArray?.parseMetadataElements(): List<LyricsMetadataElement> {
    return this?.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val node = obj.parseMetadataElement() ?: return@mapNotNull null
        // Dispatch on the top-level element name; children are parsed recursively without re-checking.
        when (node.name) {
            META_NAME_SONGWRITERS -> {
                // Official shape: children are songwriter elements with non-blank text.
                val valid = node.children.isNotEmpty() &&
                    node.children.all { it.name == META_NAME_SONGWRITER && !it.text.isNullOrBlank() }
                if (valid) {
                    node
                } else {
                    null
                }
            }

            in META_NAMES_DUPLICATED -> null

            else -> node // Unofficial key: pass through unchanged.
        }
    }.orEmpty()
}

/** Single metadata node: { name, namespace, attributes, text, children }; name is required. */
private fun JsonObject.parseMetadataElement(): LyricsMetadataElement? {
    val name = primitiveString("name")?.takeIf { it.isNotBlank() } ?: return null
    val namespace = primitiveString("namespace")
    // A prefixed name outside ttm/itunes/xml needs a namespace URI, otherwise the output XML is invalid.
    val prefix = name.substringBefore(':', "")
    if (prefix.isNotEmpty() && prefix != "ttm" && prefix != "itunes" && prefix != "xml" &&
        namespace.isNullOrBlank()
    ) {
        return null
    }
    val attributes = this["attributes"] as? JsonObject
    val children = this["children"] as? JsonArray
    return LyricsMetadataElement(
        name = name,
        namespace = namespace,
        attributes = attributes.parseStringMap(),
        text = primitiveString("text"),
        // Children are parsed without validation so unofficial trees pass through intact.
        children = children.parseMetadataTree()
    )
}

/** Plain parse without the three-way rule; used for children so the passed-through tree stays complete. */
private fun JsonArray?.parseMetadataTree(): List<LyricsMetadataElement> {
    return this?.mapNotNull { element ->
        (element as? JsonObject)?.parseMetadataElement()
    }.orEmpty()
}

/** JsonObject -> Map<String, String>, keeping string values (JsonPrimitive) only. */
private fun JsonObject?.parseStringMap(): Map<String, String> {
    if (this == null) return emptyMap()
    return mapValuesNotNull { (_, value) ->
        (value as? JsonPrimitive)?.contentOrNull
    }
}

private fun JsonObject.string(vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { key ->
        val value = this[key] ?: return@firstNotNullOfOrNull null
        when (value) {
            is JsonPrimitive -> value.contentOrNull

            is JsonArray -> value.joinToString("/") { item ->
                when (item) {
                    is JsonPrimitive -> item.contentOrNull.orEmpty()
                    is JsonObject -> item.string("name", "title", "value").orEmpty()
                    else -> ""
                }
            }.takeIf { it.isNotBlank() }

            else -> null
        }
    }
}

private fun JsonObject.primitiveString(vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { key ->
        (this[key] as? JsonPrimitive)?.contentOrNull
    }
}

private fun JsonObject.long(vararg keys: String): Long? {
    return keys.firstNotNullOfOrNull { key ->
        val value = this[key] ?: return@firstNotNullOfOrNull null
        when (value) {
            is JsonPrimitive -> value.longOrNull ?: value.contentOrNull?.toLongOrNull()
            else -> null
        }
    }
}

private fun JsonObject.boolean(key: String): Boolean? {
    return (this[key] as? JsonPrimitive)?.booleanOrNull
}

private fun JsonObject.array(vararg keys: String): JsonArray? {
    return keys.firstNotNullOfOrNull { key ->
        this[key] as? JsonArray
    }
}

private fun JsonObject.stringMap(vararg keys: String): Map<String, String>? {
    val obj = keys.firstNotNullOfOrNull { key ->
        this[key] as? JsonObject
    } ?: return null

    return obj.mapValuesNotNull { (_, value) ->
        when (value) {
            is JsonPrimitive -> value.contentOrNull
            else -> value.toString()
        }
    }
}

private inline fun <K, V, R : Any> Map<K, V>.mapValuesNotNull(
    transform: (Map.Entry<K, V>) -> R?
): Map<K, R> {
    return mapNotNull { entry ->
        transform(entry)?.let { entry.key to it }
    }.toMap()
}

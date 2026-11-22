package com.berryos.music.core.plugin.model

import kotlinx.serialization.Serializable

// Ported from Lyrico (Apache License 2.0): com.lonx.lyrico.data.model.lyrics.LyricLineTrack
@Serializable
enum class LyricLineTrack {
    ORIGINAL,
    ROMANIZATION,
    TRANSLATION,
}

val DefaultLyricLineOrder = listOf(
    LyricLineTrack.ORIGINAL,
    LyricLineTrack.ROMANIZATION,
    LyricLineTrack.TRANSLATION,
)

fun List<LyricLineTrack>.normalizedLyricLineOrder(): List<LyricLineTrack> {
    return (this + DefaultLyricLineOrder).distinct()
        .filter { it in DefaultLyricLineOrder }
}

fun visibleLyricLineTracks(
    showRomanization: Boolean,
    showTranslation: Boolean,
    onlyTranslationIfAvailable: Boolean,
): List<LyricLineTrack> {
    if (showTranslation && onlyTranslationIfAvailable) {
        return listOf(LyricLineTrack.TRANSLATION)
    }

    return buildList {
        add(LyricLineTrack.ORIGINAL)
        if (showRomanization) add(LyricLineTrack.ROMANIZATION)
        if (showTranslation) add(LyricLineTrack.TRANSLATION)
    }
}

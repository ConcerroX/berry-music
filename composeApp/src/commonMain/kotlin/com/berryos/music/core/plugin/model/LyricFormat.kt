package com.berryos.music.core.plugin.model

import kotlinx.serialization.Serializable

// Ported from Lyrico (Apache License 2.0): com.lonx.lyrico.data.model.lyrics.LyricFormat
@Serializable
enum class LyricFormat {
    PLAIN_LRC,
    VERBATIM_LRC,
    ENHANCED_LRC,
    TTML,
}

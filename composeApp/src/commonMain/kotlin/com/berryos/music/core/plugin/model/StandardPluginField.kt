package com.berryos.music.core.plugin.model

/**
 * Keys accepted inside a plugin result's `fields` object.
 *
 * Ported from Lyrico (Apache License 2.0): com.lonx.lyrico.data.model.metadata.StandardPluginField.
 * Lyrico also maps each key to a tag-field target; writing results into files is out of scope here.
 */
enum class StandardPluginField(val key: String) {
    TITLE("title"),
    ARTIST("artist"),
    ALBUM("album"),
    ALBUM_ARTIST("album_artist"),
    GENRE("genre"),
    DATE("date"),
    TRACK_NUMBER("track_number"),
    DISC_NUMBER("disc_number"),
    COMPOSER("composer"),
    LYRICIST("lyricist"),
    COMMENT("comment"),
    LYRICS("lyrics"),
    COVER_URL("cover_url"),
    LANGUAGE("language"),
    COPYRIGHT("copyright"),
    RATING("rating"),
    REPLAY_GAIN_TRACK_GAIN("replaygain_track_gain"),
    REPLAY_GAIN_TRACK_PEAK("replaygain_track_peak"),
    REPLAY_GAIN_ALBUM_GAIN("replaygain_album_gain"),
    REPLAY_GAIN_ALBUM_PEAK("replaygain_album_peak"),
    REPLAY_GAIN_REFERENCE_LOUDNESS("replaygain_reference_loudness"),
    ;

    companion object {
        private val byKey = entries.associateBy { it.key }

        fun fromKey(key: String): StandardPluginField? = byKey[key]
    }
}

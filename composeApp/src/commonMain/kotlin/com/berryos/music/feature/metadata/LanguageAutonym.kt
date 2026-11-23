package com.berryos.music.feature.metadata

/**
 * Returns the language's own name when the platform has data for it, or null when it only knows
 * the code. Platforms provide these names from their locale database (ICU on Android).
 */
internal expect fun platformLanguageAutonym(code: String, alpha2: String): String?

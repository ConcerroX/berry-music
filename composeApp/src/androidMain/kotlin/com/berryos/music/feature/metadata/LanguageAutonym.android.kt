package com.berryos.music.feature.metadata

import java.util.Locale

private val availableLanguages: Set<String> by lazy {
    Locale.getAvailableLocales()
        .mapNotNullTo(mutableSetOf()) { it.language.takeIf(String::isNotEmpty) }
}

internal actual fun platformLanguageAutonym(code: String, alpha2: String): String? {
    val tag = if (alpha2.isNotEmpty()) alpha2 else code
    val locale = runCatching { Locale.forLanguageTag(tag) }.getOrNull() ?: return null
    // An unsupported display locale makes ICU fall back to the system language, which would show
    // translated names instead of autonyms; only query languages the platform has data for.
    if (locale.language.isEmpty() || locale.language !in availableLanguages) return null
    val name = locale.getDisplayLanguage(locale)
    return name.takeIf {
        it.isNotEmpty() &&
            !it.equals(tag, ignoreCase = true) &&
            !it.equals(code, ignoreCase = true) &&
            !it.equals(locale.language, ignoreCase = true) &&
            !it.equals(locale.toLanguageTag(), ignoreCase = true)
    }
}

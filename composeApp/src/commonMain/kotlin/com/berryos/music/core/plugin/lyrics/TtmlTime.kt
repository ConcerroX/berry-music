package com.berryos.music.core.plugin.lyrics

// Ported from Lyrico (Apache License 2.0): com.lonx.lyrico.utils.lyrics.document.TtmlTime
internal object TtmlTime {
    private const val MILLIS_PER_SECOND = 1000.0

    private val milliseconds = Regex("""^(\d+(?:\.\d+)?)ms$""")
    private val seconds = Regex("""^(\d+(?:\.\d+)?)s$""")
    private val plainSeconds = Regex("""^(\d+(?:\.\d+)?)$""")
    private val clockHours = Regex("""^(\d+):(\d{2}):(\d{2})(?:\.(\d+))?$""")
    private val clockMinutes = Regex("""^(\d+):(\d{2})(?:\.(\d+))?$""")

    fun parseMsOrNull(value: String?): Long? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return milliseconds.matchEntire(text)?.groupValues?.get(1)?.toFiniteDoubleOrNull()?.toLong()
            ?: seconds.matchEntire(text)?.groupValues?.get(1)?.toFiniteDoubleOrNull()
                ?.times(MILLIS_PER_SECOND)?.toLong()
            ?: plainSeconds.matchEntire(text)?.groupValues?.get(1)?.toFiniteDoubleOrNull()
                ?.times(MILLIS_PER_SECOND)?.toLong()
            ?: clockHours.matchEntire(text)?.let { match ->
                clockMs(
                    hours = match.groupValues[1],
                    minutes = match.groupValues[2],
                    seconds = match.groupValues[3],
                    fraction = match.groupValues[4],
                )
            }
            ?: clockMinutes.matchEntire(text)?.let { match ->
                clockMs(
                    hours = "0",
                    minutes = match.groupValues[1],
                    seconds = match.groupValues[2],
                    fraction = match.groupValues[3],
                )
            }
    }

    fun isValid(value: String?): Boolean = parseMsOrNull(value) != null

    private fun clockMs(hours: String, minutes: String, seconds: String, fraction: String): Long? {
        val hourValue = hours.toLongOrNull()
        val minuteValue = minutes.toLongOrNull()
        val secondValue = seconds.toLongOrNull()
        if (hourValue == null || minuteValue == null || secondValue == null) return null
        val millis = fraction.takeIf { it.isNotEmpty() }
            ?.padEnd(3, '0')
            ?.take(3)
            ?.toLongOrNull()
            ?: 0L
        val total = ((hourValue * 3600.0) + (minuteValue * 60.0) + secondValue) * MILLIS_PER_SECOND + millis
        return total.takeIf { it >= 0 && it <= Long.MAX_VALUE.toDouble() }?.toLong()
    }

    private fun String.toFiniteDoubleOrNull(): Double? = toDoubleOrNull()?.takeIf { it.isFinite() }
}

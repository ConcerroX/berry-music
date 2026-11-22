package com.berryos.music.core.plugin.lyrics

// Ported from Lyrico (Apache License 2.0): com.lonx.lyrico.utils.lyrics.document.TtmlTime
internal object TtmlTime {
    private val milliseconds = Regex("""^(\d+(?:\.\d+)?)ms$""")
    private val seconds = Regex("""^(\d+(?:\.\d+)?)s$""")
    private val plainSeconds = Regex("""^(\d+(?:\.\d+)?)$""")
    private val clockHours = Regex("""^(\d+):(\d{2}):(\d{2})(?:\.(\d+))?$""")
    private val clockMinutes = Regex("""^(\d+):(\d{2})(?:\.(\d+))?$""")

    fun parseMsOrNull(value: String?): Long? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        milliseconds.matchEntire(text)?.let { match ->
            return match.groupValues[1].toFiniteDoubleOrNull()?.toLong()
        }
        seconds.matchEntire(text)?.let { match ->
            return match.groupValues[1].toFiniteDoubleOrNull()?.times(1000)?.toLong()
        }
        plainSeconds.matchEntire(text)?.let { match ->
            return match.groupValues[1].toFiniteDoubleOrNull()?.times(1000)?.toLong()
        }
        clockHours.matchEntire(text)?.let { match ->
            return clockMs(
                hours = match.groupValues[1],
                minutes = match.groupValues[2],
                seconds = match.groupValues[3],
                fraction = match.groupValues[4],
            )
        }
        clockMinutes.matchEntire(text)?.let { match ->
            return clockMs(
                hours = "0",
                minutes = match.groupValues[1],
                seconds = match.groupValues[2],
                fraction = match.groupValues[3],
            )
        }
        return null
    }

    fun isValid(value: String?): Boolean = parseMsOrNull(value) != null

    private fun clockMs(hours: String, minutes: String, seconds: String, fraction: String): Long? {
        val hourValue = hours.toLongOrNull() ?: return null
        val minuteValue = minutes.toLongOrNull() ?: return null
        val secondValue = seconds.toLongOrNull() ?: return null
        val milliseconds = fraction.takeIf { it.isNotEmpty() }
            ?.padEnd(3, '0')
            ?.take(3)
            ?.toLongOrNull()
            ?: 0L
        val total = ((hourValue * 3600.0) + (minuteValue * 60.0) + secondValue) * 1000.0 + milliseconds
        return total.takeIf { it >= 0 && it <= Long.MAX_VALUE.toDouble() }?.toLong()
    }

    private fun String.toFiniteDoubleOrNull(): Double? {
        return toDoubleOrNull()?.takeIf { it.isFinite() }
    }
}

package io.github.ieswar23.vibely.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/** Formats engagement numbers the way social apps do: 987, 4,210, 48.2K, 1.2M. */
object CountFormatter {

    fun compact(value: Long, locale: Locale = Locale.US): String {
        val absValue = abs(value)
        return when {
            absValue < 10_000 -> NumberFormat.getIntegerInstance(locale).format(value)
            absValue < 1_000_000 -> scaled(value, 1_000, "K")
            absValue < 1_000_000_000 -> scaled(value, 1_000_000, "M")
            else -> scaled(value, 1_000_000_000, "B")
        }
    }

    fun compact(value: Int, locale: Locale = Locale.US): String = compact(value.toLong(), locale)

    /** "1 like", "1,204 likes". */
    fun likes(count: Int): String = if (count == 1) "1 like" else "${grouped(count)} likes"

    fun grouped(value: Int, locale: Locale = Locale.US): String =
        NumberFormat.getIntegerInstance(locale).format(value)

    /** Integer maths, truncating to one decimal so 99,990 becomes 99.9K rather than a misleading 100K. */
    private fun scaled(value: Long, unit: Long, suffix: String): String {
        val tenths = value / (unit / 10)
        val whole = tenths / 10
        val fraction = abs(tenths % 10)
        return if (fraction == 0L) "$whole$suffix" else "$whole.$fraction$suffix"
    }
}

package io.github.ieswar23.vibely.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Human friendly relative timestamps, e.g. "Just now", "5m", "3h", "2d", "4w" or "Mar 4".
 *
 * Pure and deterministic: callers pass "now" explicitly which makes the formatter trivially testable.
 */
object TimeAgo {

    const val MINUTE = 60_000L
    const val HOUR = 60 * MINUTE
    const val DAY = 24 * HOUR
    const val WEEK = 7 * DAY

    /** Compact form used in feed headers and comment rows. */
    fun short(
        timestamp: Long,
        now: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.US,
    ): String {
        val diff = now - timestamp
        return when {
            diff < MINUTE -> "Just now"
            diff < HOUR -> "${diff / MINUTE}m"
            diff < DAY -> "${diff / HOUR}h"
            diff < WEEK -> "${diff / DAY}d"
            diff < 5 * WEEK -> "${diff / WEEK}w"
            else -> absoluteDate(timestamp, now, timeZone, locale)
        }
    }

    /** Long form used for accessibility and the story viewer, e.g. "5 minutes ago". */
    fun long(
        timestamp: Long,
        now: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.US,
    ): String {
        val diff = now - timestamp
        return when {
            diff < MINUTE -> "Just now"
            diff < HOUR -> plural(diff / MINUTE, "minute")
            diff < DAY -> plural(diff / HOUR, "hour")
            diff < 2 * DAY -> "Yesterday"
            diff < WEEK -> plural(diff / DAY, "day")
            diff < 5 * WEEK -> plural(diff / WEEK, "week")
            else -> absoluteDate(timestamp, now, timeZone, locale)
        }
    }

    /** Groups timestamps for the activity screen. */
    fun bucket(timestamp: Long, now: Long, timeZone: TimeZone = TimeZone.getDefault()): TimeBucket {
        val today = Calendar.getInstance(timeZone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return when {
            timestamp >= today -> TimeBucket.TODAY
            now - timestamp < WEEK -> TimeBucket.THIS_WEEK
            else -> TimeBucket.EARLIER
        }
    }

    private fun plural(value: Long, unit: String): String =
        if (value == 1L) "1 $unit ago" else "$value ${unit}s ago"

    private fun absoluteDate(timestamp: Long, now: Long, timeZone: TimeZone, locale: Locale): String {
        val then = Calendar.getInstance(timeZone).apply { timeInMillis = timestamp }
        val current = Calendar.getInstance(timeZone).apply { timeInMillis = now }
        val pattern = if (then.get(Calendar.YEAR) == current.get(Calendar.YEAR)) "MMM d" else "MMM d, yyyy"
        return SimpleDateFormat(pattern, locale).apply { this.timeZone = timeZone }.format(Date(timestamp))
    }
}

enum class TimeBucket(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This week"),
    EARLIER("Earlier"),
}

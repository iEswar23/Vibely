package io.github.ieswar23.vibely.util

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.vibely.util.TimeAgo.DAY
import io.github.ieswar23.vibely.util.TimeAgo.HOUR
import io.github.ieswar23.vibely.util.TimeAgo.MINUTE
import io.github.ieswar23.vibely.util.TimeAgo.WEEK
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class TimeAgoTest {

    private val utc = TimeZone.getTimeZone("UTC")

    // 2026-10-01T12:00:00Z
    private val now = 1_790_856_000_000L

    private fun short(ago: Long) = TimeAgo.short(now - ago, now, utc, Locale.US)
    private fun long(ago: Long) = TimeAgo.long(now - ago, now, utc, Locale.US)

    @Test
    fun `under a minute is just now`() {
        assertThat(short(0)).isEqualTo("Just now")
        assertThat(short(59_000)).isEqualTo("Just now")
    }

    @Test
    fun `timestamps slightly in the future are treated as just now`() {
        assertThat(TimeAgo.short(now + 5_000, now, utc)).isEqualTo("Just now")
    }

    @Test
    fun `minutes hours days and weeks use compact units`() {
        assertThat(short(MINUTE)).isEqualTo("1m")
        assertThat(short(59 * MINUTE)).isEqualTo("59m")
        assertThat(short(HOUR)).isEqualTo("1h")
        assertThat(short(23 * HOUR + 59 * MINUTE)).isEqualTo("23h")
        assertThat(short(DAY)).isEqualTo("1d")
        assertThat(short(6 * DAY)).isEqualTo("6d")
        assertThat(short(WEEK)).isEqualTo("1w")
        assertThat(short(4 * WEEK)).isEqualTo("4w")
    }

    @Test
    fun `older than five weeks in the same year shows month and day`() {
        // 2026-03-04T09:00:00Z
        assertThat(TimeAgo.short(1_772_614_800_000L, now, utc, Locale.US)).isEqualTo("Mar 4")
    }

    @Test
    fun `previous years include the year`() {
        // 2025-12-24T09:00:00Z
        assertThat(TimeAgo.short(1_766_566_800_000L, now, utc, Locale.US)).isEqualTo("Dec 24, 2025")
    }

    @Test
    fun `long form pluralises and says yesterday`() {
        assertThat(long(MINUTE)).isEqualTo("1 minute ago")
        assertThat(long(5 * MINUTE)).isEqualTo("5 minutes ago")
        assertThat(long(2 * HOUR)).isEqualTo("2 hours ago")
        assertThat(long(DAY + HOUR)).isEqualTo("Yesterday")
        assertThat(long(3 * DAY)).isEqualTo("3 days ago")
        assertThat(long(2 * WEEK)).isEqualTo("2 weeks ago")
    }

    @Test
    fun `buckets split by calendar day and rolling week`() {
        // 00:30 today vs 23:30 yesterday are an hour apart but land in different buckets.
        assertThat(TimeAgo.bucket(1_790_814_600_000L, now, utc)).isEqualTo(TimeBucket.TODAY)
        assertThat(TimeAgo.bucket(1_790_811_000_000L, now, utc)).isEqualTo(TimeBucket.THIS_WEEK)
        assertThat(TimeAgo.bucket(now - 6 * DAY, now, utc)).isEqualTo(TimeBucket.THIS_WEEK)
        assertThat(TimeAgo.bucket(now - 8 * DAY, now, utc)).isEqualTo(TimeBucket.EARLIER)
    }
}

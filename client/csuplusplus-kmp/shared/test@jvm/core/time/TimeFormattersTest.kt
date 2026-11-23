package ink.qingshuige.csuplusplus.core.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class TimeFormattersTest {

    private val utcPlus8Offset: (Long) -> Int = { 8 * 3600 }
    private val nowMillis = Instant.parse("2026-11-23T10:30:00+08:00").toEpochMilliseconds()

    @Test
    fun formatsRecentMinutesAndHours() {
        assertEquals(
            "刚刚",
            formatFriendlyTime("2026-11-23T10:29:30+08:00", nowMillis, utcPlus8Offset),
        )
        assertEquals(
            "15分钟前",
            formatFriendlyTime("2026-11-23T10:15:00+08:00", nowMillis, utcPlus8Offset),
        )
        assertEquals(
            "3小时前",
            formatFriendlyTime("2026-11-23T07:20:00+08:00", nowMillis, utcPlus8Offset),
        )
    }

    @Test
    fun formatsYesterdayAndDaysAgoUsingLocalCalendarDays() {
        assertEquals(
            "昨天",
            formatFriendlyTime("2026-11-22T23:50:00+08:00", nowMillis, utcPlus8Offset),
        )
        assertEquals(
            "昨天",
            formatFriendlyTime("2026-11-22T01:00:00+08:00", nowMillis, utcPlus8Offset),
        )
        assertEquals(
            "2天前",
            formatFriendlyTime("2026-11-21T20:00:00+08:00", nowMillis, utcPlus8Offset),
        )
        assertEquals(
            "6天前",
            formatFriendlyTime("2026-11-17T08:00:00+08:00", nowMillis, utcPlus8Offset),
        )
    }

    @Test
    fun formatsSameYearAndCrossYearDatesWithTimezoneAwareness() {
        assertEquals(
            "11月16日",
            formatFriendlyTime("2026-11-16T22:00:00+08:00", nowMillis, utcPlus8Offset),
        )
        assertEquals(
            "1月5日",
            formatFriendlyTime("2026-01-05T02:00:00.1234567Z", nowMillis, utcPlus8Offset),
        )
        // UTC 2025-12-31T18:00:00Z corresponds to 2026-01-01T02:00:00+08:00 (same year in UTC+8)
        assertEquals(
            "1月1日",
            formatFriendlyTime("2025-12-31T18:00:00Z", nowMillis, utcPlus8Offset),
        )
        assertEquals(
            "2025年12月31日",
            formatFriendlyTime("2025-12-31T15:00:00Z", nowMillis, utcPlus8Offset),
        )
    }

    @Test
    fun handlesInvalidOrBlankInputsGracefully() {
        assertEquals("", formatFriendlyTime("   ", nowMillis, utcPlus8Offset))
        assertEquals("not-a-date", formatFriendlyTime(" not-a-date ", nowMillis, utcPlus8Offset))
        assertEquals(
            "刚刚",
            formatFriendlyTime("2026-11-23T10:30:20+08:00", nowMillis, utcPlus8Offset),
        )
        assertEquals(
            "11月25日",
            formatFriendlyTime("2026-11-25T10:30:00+08:00", nowMillis, utcPlus8Offset),
        )
    }
}

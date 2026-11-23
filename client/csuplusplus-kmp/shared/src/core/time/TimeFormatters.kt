package ink.qingshuige.csuplusplus.core.time

import kotlin.math.floor
import kotlin.time.Clock
import kotlin.time.Instant

internal expect fun localUtcOffsetSeconds(epochMillis: Long): Int

/**
 * 将 ISO-8601 时间字符串格式化为易读的相对/日期时间文本（基于本地时区）。
 *
 * 格式规则：
 * - 1 分钟内：`刚刚`
 * - 1 小时内：`n分钟前`
 * - 当天内：`n小时前`
 * - 昨天：`昨天`
 * - 2~6 天前：`n天前`
 * - 同年更早：`M月d日`
 * - 跨年：`yyyy年M月d日`
 *
 * 若无法解析 [isoTimestamp]，则返回去首尾空白后的原字符串。
 */
fun formatFriendlyTime(
    isoTimestamp: String,
    nowEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
    utcOffsetProvider: (epochMillis: Long) -> Int = ::localUtcOffsetSeconds,
): String {
    val trimmed = isoTimestamp.trim()
    if (trimmed.isEmpty()) return ""

    val targetEpochMillis = parseIsoEpochMillis(trimmed) ?: return trimmed
    return formatFriendlyEpochMillis(
        targetEpochMillis = targetEpochMillis,
        nowEpochMillis = nowEpochMillis,
        utcOffsetProvider = utcOffsetProvider,
    )
}

/**
 * [formatFriendlyTime] 的字符串扩展版本。
 */
fun String.toFriendlyTime(
    nowEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
    utcOffsetProvider: (epochMillis: Long) -> Int = ::localUtcOffsetSeconds,
): String = formatFriendlyTime(
    isoTimestamp = this,
    nowEpochMillis = nowEpochMillis,
    utcOffsetProvider = utcOffsetProvider,
)

/**
 * 将毫秒时间戳格式化为易读的相对/日期时间文本（基于本地时区）。
 */
fun formatFriendlyEpochMillis(
    targetEpochMillis: Long,
    nowEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
    utcOffsetProvider: (epochMillis: Long) -> Int = ::localUtcOffsetSeconds,
): String {
    val targetOffsetSeconds = utcOffsetProvider(targetEpochMillis)
    val nowOffsetSeconds = utcOffsetProvider(nowEpochMillis)

    val targetLocalDay = localEpochDay(targetEpochMillis, targetOffsetSeconds)
    val nowLocalDay = localEpochDay(nowEpochMillis, nowOffsetSeconds)
    val dayDiff = nowLocalDay - targetLocalDay

    val targetDate = civilFromEpochDays(targetLocalDay)
    val nowDate = civilFromEpochDays(nowLocalDay)

    val diffMillis = nowEpochMillis - targetEpochMillis
    if (diffMillis < 0L) {
        if (diffMillis > -60_000L) return "刚刚"
        return formatCalendarDate(targetDate, nowDate.year)
    }

    if (diffMillis < 60_000L) return "刚刚"
    if (diffMillis < 3_600_000L) return "${diffMillis / 60_000L}分钟前"

    return when {
        dayDiff == 0L -> "${diffMillis / 3_600_000L}小时前"
        dayDiff == 1L -> "昨天"
        dayDiff in 2L..6L -> "${dayDiff}天前"
        else -> formatCalendarDate(targetDate, nowDate.year)
    }
}

private fun parseIsoEpochMillis(raw: String): Long? {
    val normalized = if (raw.length > 10 && raw[10] == ' ') {
        raw.substring(0, 10) + 'T' + raw.substring(11)
    } else {
        raw
    }
    return Instant.parseOrNull(normalized)?.toEpochMilliseconds()
}

private fun formatCalendarDate(date: CivilDate, currentYear: Int): String =
    if (date.year == currentYear) {
        "${date.month}月${date.day}日"
    } else {
        "${date.year}年${date.month}月${date.day}日"
    }

private fun localEpochDay(epochMillis: Long, utcOffsetSeconds: Int): Long {
    val epochSeconds = floor(epochMillis / 1000.0).toLong() + utcOffsetSeconds
    return epochSeconds.floorDiv(86_400L)
}

private data class CivilDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

private fun civilFromEpochDays(epochDays: Long): CivilDate {
    val z = epochDays + 719_468L
    val era = (if (z >= 0L) z else z - 146_096L) / 146_097L
    val doe = z - era * 146_097L
    val yoe = (doe - doe / 1460L + doe / 36_524L - doe / 146_096L) / 365L
    val y = yoe + era * 400L
    val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
    val mp = (5L * doy + 2L) / 153L
    val d = doy - (153L * mp + 2L) / 5L + 1L
    val m = mp + (if (mp < 10L) 3L else -9L)
    val year = (y + (if (m <= 2L) 1L else 0L)).toInt()
    return CivilDate(
        year = year,
        month = m.toInt(),
        day = d.toInt(),
    )
}

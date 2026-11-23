package ink.qingshuige.csuplusplus.core.time

import java.util.TimeZone

internal actual fun localUtcOffsetSeconds(epochMillis: Long): Int =
    TimeZone.getDefault().getOffset(epochMillis) / 1000

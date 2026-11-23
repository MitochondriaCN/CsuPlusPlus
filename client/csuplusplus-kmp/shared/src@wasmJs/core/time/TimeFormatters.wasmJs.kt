package ink.qingshuige.csuplusplus.core.time

import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(ms) => new Date(ms).getTimezoneOffset()")
private external fun jsTimezoneOffsetMinutes(epochMillis: Double): Int

internal actual fun localUtcOffsetSeconds(epochMillis: Long): Int =
    -jsTimezoneOffsetMinutes(epochMillis.toDouble()) * 60

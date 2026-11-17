package ink.qingshuige.csuplusplus

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

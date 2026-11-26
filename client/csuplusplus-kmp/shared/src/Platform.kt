package ink.qingshuige.csuplusplus

interface Platform {
    val name: String
    val buildCode: Long
    val buildName: String
}

expect fun getPlatform(): Platform

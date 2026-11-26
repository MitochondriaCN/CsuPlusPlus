package ink.qingshuige.csuplusplus

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val buildCode: Long = DEFAULT_BUILD_CODE
    override val buildName: String = DEFAULT_BUILD_NAME
}

actual fun getPlatform(): Platform = JVMPlatform()

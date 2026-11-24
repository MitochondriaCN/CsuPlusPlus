package ink.qingshuige.csuplusplus

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val buildCode: Long = DEFAULT_BUILD_CODE
    override val buildName: String = DEFAULT_BUILD_NAME
}

actual fun getPlatform(): Platform = WasmPlatform()

package ink.qingshuige.csuplusplus

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override val buildCode: Long
        get() = NSBundle.mainBundle
            .objectForInfoDictionaryKey("CFBundleVersion")
            ?.toString()
            ?.toLongOrNull()
            ?: DEFAULT_BUILD_CODE

    override val buildName: String
        get() = NSBundle.mainBundle
            .objectForInfoDictionaryKey("CFBundleShortVersionString")
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_BUILD_NAME
}

actual fun getPlatform(): Platform = IOSPlatform()

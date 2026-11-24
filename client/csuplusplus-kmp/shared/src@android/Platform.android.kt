package ink.qingshuige.csuplusplus

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import ink.qingshuige.csuplusplus.core.network.getAndroidAppContext

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    override val buildCode: Long
        get() {
            val info = readPackageInfo() ?: return DEFAULT_BUILD_CODE
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        }

    override val buildName: String
        get() = readPackageInfo()?.versionName?.takeIf { it.isNotBlank() } ?: DEFAULT_BUILD_NAME

    private fun readPackageInfo(): PackageInfo? {
        val context = getAndroidAppContext() ?: return null
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        }.getOrNull()
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()

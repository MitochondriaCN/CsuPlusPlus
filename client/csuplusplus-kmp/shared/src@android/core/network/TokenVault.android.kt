package ink.qingshuige.csuplusplus.core.network

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeConfig
import java.io.File

private object AndroidTokenVaultHolder {
    @Volatile
    var appContext: Context? = null

    @Volatile
    var ksafe: KSafe? = null
}

internal actual val ksafeNeedsAsyncInit: Boolean = false

@Composable
internal actual fun BindPlatformTokenStorageContext() {
    val context = LocalContext.current
    if (AndroidTokenVaultHolder.appContext == null) {
        AndroidTokenVaultHolder.appContext =
            runCatching { context.applicationContext }.getOrNull() ?: context
    }
}

internal actual fun getOrCreateAuthKSafe(): KSafe? {
    AndroidTokenVaultHolder.ksafe?.let { return it }
    val context = AndroidTokenVaultHolder.appContext ?: return null
    return runCatching {
        val vaultDir = runCatching { File(context.noBackupFilesDir, "ksafe") }.getOrNull()
        KSafe(
            context = context,
            fileName = AUTH_VAULT_FILE_NAME,
            config = KSafeConfig(appNamespace = APP_NAMESPACE),
            baseDir = vaultDir,
        ).also { AndroidTokenVaultHolder.ksafe = it }
    }.getOrNull()
}

internal actual suspend fun awaitKSafeReady(ksafe: KSafe) = Unit

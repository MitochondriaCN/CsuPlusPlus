package ink.qingshuige.csuplusplus.core.network

import androidx.compose.runtime.Composable
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeConfig

private object IosTokenVaultHolder {
    val ksafe: KSafe? by lazy {
        runCatching {
            KSafe(
                fileName = AUTH_VAULT_FILE_NAME,
                config = KSafeConfig(appNamespace = APP_NAMESPACE),
            )
        }.getOrNull()
    }
}

internal actual val ksafeNeedsAsyncInit: Boolean = false

@Composable
internal actual fun BindPlatformTokenStorageContext() = Unit

internal actual fun getOrCreateAuthKSafe(): KSafe? = IosTokenVaultHolder.ksafe

internal actual suspend fun awaitKSafeReady(ksafe: KSafe) = Unit

package ink.qingshuige.csuplusplus.core.network

import androidx.compose.runtime.Composable
import eu.anifantakis.lib.ksafe.KSafe

internal const val AUTH_VAULT_FILE_NAME = "auth_vault"
internal const val APP_NAMESPACE = "ink.qingshuige.csuplusplus"

internal expect val ksafeNeedsAsyncInit: Boolean

@Composable
internal expect fun BindPlatformTokenStorageContext()

internal expect fun getOrCreateAuthKSafe(): KSafe?

internal expect suspend fun awaitKSafeReady(ksafe: KSafe)

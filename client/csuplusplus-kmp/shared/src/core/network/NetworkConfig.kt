package ink.qingshuige.csuplusplus.core.network

/**
 * 网络层基础配置。
 */
data class NetworkConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val connectTimeoutMillis: Long = 10_000L,
    val requestTimeoutMillis: Long = 30_000L,
    val socketTimeoutMillis: Long = 30_000L,
    val isDebugLogging: Boolean = true,
) {
    val normalizedBaseUrl: String
        get() = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"

    companion object {
        const val DEFAULT_ROOT_URL = "https://csupp.qingshuige.ink"
        const val DEFAULT_BASE_URL = "$DEFAULT_ROOT_URL/api"
    }
}

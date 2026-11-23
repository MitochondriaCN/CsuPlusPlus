package ink.qingshuige.csuplusplus.core.network

/**
 * 统一网络错误模型。
 */
sealed interface ApiError {
    data class Http(
        val statusCode: Int,
        val problem: ProblemDetails? = null,
        val rawBody: String? = null,
    ) : ApiError

    data class Network(val cause: Throwable) : ApiError

    data class Serialization(val cause: Throwable) : ApiError

    data class Unknown(val cause: Throwable) : ApiError
}

/**
 * 包装 [ApiError] 的异常类，可直接配合 Kotlin [Result] 使用。
 */
class ApiException(
    val error: ApiError,
    override val cause: Throwable? = when (error) {
        is ApiError.Network -> error.cause
        is ApiError.Serialization -> error.cause
        is ApiError.Unknown -> error.cause
        is ApiError.Http -> null
    },
) : Exception(error.toUserMessage(), cause) {

    val userMessage: String
        get() = message ?: "请求失败，请稍后重试"

    val statusCode: Int?
        get() = (error as? ApiError.Http)?.statusCode

    val problem: ProblemDetails?
        get() = (error as? ApiError.Http)?.problem
}

fun Throwable.asApiException(): ApiException = when (this) {
    is ApiException -> this
    else -> ApiException(ApiError.Unknown(this))
}

val Throwable.userMessage: String
    get() = (this as? ApiException)?.userMessage ?: message ?: "请求失败，请稍后重试"

private fun ApiError.toUserMessage(): String = when (this) {
    is ApiError.Http -> problem?.displayMessage ?: when (statusCode) {
        400 -> "请求参数有误"
        401 -> "登录已失效，请重新登录"
        403 -> "没有权限执行此操作"
        404 -> "请求的资源不存在"
        409 -> "资源已存在或状态冲突"
        413 -> "文件大小超出限制"
        415 -> "不支持的文件格式"
        429 -> "请求过于频繁，请稍后重试"
        in 500..599 -> "服务器开小差了（$statusCode）"
        else -> "请求失败（HTTP $statusCode）"
    }

    is ApiError.Network -> "网络连接异常，请检查网络或服务地址"
    is ApiError.Serialization -> "响应数据解析失败"
    is ApiError.Unknown -> cause.message ?: "未知错误"
}

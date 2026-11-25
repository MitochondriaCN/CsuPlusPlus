package ink.qingshuige.csuplusplus.core.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.util.AttributeKey
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

internal val SkipAuthAttributeKey = AttributeKey<Boolean>("SkipApiAuth")

/**
 * Ktor 请求统一封装：
 * - 自动处理路径拼接（与 [NetworkConfig.normalizedBaseUrl] 组合）
 * - 统一校验 HTTP 状态码并解析 ASP.NET Core [ProblemDetails]
 * - 统一收敛网络/序列化异常为 [Result] + [ApiException]，同时透传 [CancellationException]
 */
class ApiClient(
    @PublishedApi internal val httpClient: HttpClient,
    @PublishedApi internal val json: Json,
    private val config: NetworkConfig = NetworkConfig(),
) {
    suspend inline fun <reified T> get(
        path: String,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): Result<T> = execute(HttpMethod.Get, path, body = null, block = block)

    suspend inline fun <reified T> post(
        path: String,
        body: Any? = null,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): Result<T> = execute(HttpMethod.Post, path, body = body, block = block)

    suspend inline fun <reified T> put(
        path: String,
        body: Any? = null,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): Result<T> = execute(HttpMethod.Put, path, body = body, block = block)

    suspend inline fun <reified T> patch(
        path: String,
        body: Any? = null,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): Result<T> = execute(HttpMethod.Patch, path, body = body, block = block)

    suspend inline fun <reified T> delete(
        path: String,
        body: Any? = null,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): Result<T> = execute(HttpMethod.Delete, path, body = body, block = block)

    suspend inline fun <reified T> postMultipart(
        path: String,
        noinline queryAndHeaders: HttpRequestBuilder.() -> Unit = {},
        noinline formBuilder: FormBuilder.() -> Unit,
    ): Result<T> = safeNetworkCall {
        val response = httpClient.request {
            method = HttpMethod.Post
            url(normalizePath(path))
            queryAndHeaders()
            setBody(MultiPartFormDataContent(formData(formBuilder)))
        }
        decodeResponse(response)
    }

    /**
     * 从外部完整 URL 下载二进制数据（如对象存储预签名链接），跳过业务鉴权与基础路径拼接。
     */
    suspend fun downloadBytes(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): Result<ByteArray> = safeNetworkCall {
        val response = httpClient.request {
            method = HttpMethod.Get
            url(url)
            attributes.put(SkipAuthAttributeKey, true)
            accept(ContentType.Any)
            timeout {
                requestTimeoutMillis = config.downloadRequestTimeoutMillis
                socketTimeoutMillis = config.downloadSocketTimeoutMillis
            }
            block()
        }
        if (!response.status.isSuccess()) {
            throw buildHttpException(response)
        }
        response.body<ByteArray>()
    }

    @PublishedApi
    internal suspend inline fun <reified T> execute(
        httpMethod: HttpMethod,
        path: String,
        body: Any?,
        noinline block: HttpRequestBuilder.() -> Unit,
    ): Result<T> = safeNetworkCall {
        val response = httpClient.request {
            method = httpMethod
            url(normalizePath(path))
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            block()
        }
        decodeResponse(response)
    }

    @PublishedApi
    internal suspend inline fun <reified T> decodeResponse(response: HttpResponse): T {
        // 成功与否全看StatusCode
        if (!response.status.isSuccess()) {
            throw buildHttpException(response)
        }
        if (T::class == Unit::class) {
            @Suppress("UNCHECKED_CAST")
            return Unit as T
        }
        return response.body<T>()
    }

    /**
     * 安全执行（网络请求的）代码，并映射错误类型。用于在整个网络请求过程中捕获异常。
     */
    @PublishedApi
    internal inline fun <T> safeNetworkCall(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(ApiException(ApiError.Network(e)))
        } catch (e: IOException) {
            Result.failure(ApiException(ApiError.Network(e)))
        } catch (e: JsonConvertException) {
            Result.failure(ApiException(ApiError.Serialization(e)))
        } catch (e: SerializationException) {
            Result.failure(ApiException(ApiError.Serialization(e)))
        } catch (e: Throwable) {
            Result.failure(ApiException(ApiError.Unknown(e)))
        }
    }

    /**
     * 利用 [HttpResponse] 中的 [ProblemDetails] 构建 [ApiException]
     */
    @PublishedApi
    internal suspend fun buildHttpException(response: HttpResponse): ApiException {
        val rawBody = runCatching { response.bodyAsText() }.onFailure {
            if (it is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }.getOrNull()
        val problem = rawBody
            ?.takeIf { it.isNotBlank() }
            ?.let { text -> runCatching { json.decodeFromString<ProblemDetails>(text) }.getOrNull() }
        return ApiException(
            ApiError.Http(
                statusCode = response.status.value,
                problem = problem,
                rawBody = rawBody,
            )
        )
    }

    @PublishedApi
    internal fun normalizePath(path: String): String = path.trimStart('/')
}

package ink.qingshuige.csuplusplus.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 对应 ASP.NET Core 返回的 RFC 7807 / 9457 ProblemDetails（含 ValidationProblemDetails）。
 */
@Serializable
data class ProblemDetails(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val instance: String? = null,
    val errors: Map<String, List<String>>? = null,
    val existingFile: JsonElement? = null,
) {
    val firstValidationError: String?
        get() = errors?.values?.firstOrNull()?.firstOrNull()

    val displayMessage: String?
        get() = firstValidationError ?: detail ?: title
}

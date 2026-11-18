package ink.qingshuige.csuplusplus.data.dto

import io.ktor.client.request.*
import kotlinx.serialization.Serializable

/**
 * 通用分页请求参数，对应后端 `PageRequest`。
 */
data class PageQuery(
    val page: Int = 1,
    val pageSize: Int = 20,
)

fun HttpRequestBuilder.pageParams(query: PageQuery) {
    parameter("page", query.page)
    parameter("pageSize", query.pageSize)
}

/**
 * 通用分页响应结构，对应后端 `PagedResponse<T>`。
 */
@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Long,
) {
    val totalPages: Long
        get() = if (pageSize <= 0) 0 else (totalCount + pageSize - 1) / pageSize

    val hasNextPage: Boolean
        get() = page < totalPages
}

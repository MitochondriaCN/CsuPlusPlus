package ink.qingshuige.csuplusplus.core.pagination

import ink.qingshuige.csuplusplus.core.network.userMessage
import ink.qingshuige.csuplusplus.data.dto.PageQuery
import ink.qingshuige.csuplusplus.data.dto.PagedResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * 轻量分页列表状态
 *
 * @property items 当前已累积加载的全部列表项。
 * @property page 当前已加载到的页码；`0` 表示尚未完成首次加载。
 * @property pageSize 每页条数，默认 `20`。
 * @property totalCount 后端返回的总条数。
 * @property hasNextPage 是否还有下一页。
 * @property isRefreshing 是否正在刷新第一页。
 * @property isAppending 是否正在追加加载下一页。
 * @property errorMessage 最近一次请求失败时的可读错误提示；无错误时为 `null`。
 */
data class PagedState<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 20,
    val totalCount: Long = 0,
    val hasNextPage: Boolean = false,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val errorMessage: String? = null,
) {
    /** 是否处于首屏加载中（列表为空且正在刷新）。 */
    val isFirstLoading: Boolean
        get() = isRefreshing && items.isEmpty()

    /** 是否处于首屏加载失败（列表为空且存在错误信息）。 */
    val isFirstError: Boolean
        get() = items.isEmpty() && errorMessage != null

    /**
     * 列表是否为空。
     *
     * 在尚未完成首次请求（`page == 0`）时返回 `false`。
     */
    val isEmpty: Boolean
        get() = page > 0 && !isRefreshing && errorMessage == null && items.isEmpty()

    /** 当前是否允许触发下一页加载。 */
    val canLoadMore: Boolean
        get() = hasNextPage && !isRefreshing && !isAppending && errorMessage == null
}

/**
 * 在保留分页元数据（[PagedResponse.page]、[PagedResponse.pageSize]、[PagedResponse.totalCount]）的前提下，
 * 将分页响应中的列表项映射为另一种类型（常用于 DTO 转 UI 模型）。
 */
inline fun <T, R> PagedResponse<T>.mapItems(transform: (T) -> R): PagedResponse<R> =
    PagedResponse(
        items = items.map(transform),
        page = page,
        pageSize = pageSize,
        totalCount = totalCount,
    )

/**
 * 配合 [PagedState] 与 [PagedResponse] 使用的轻量分页控制器。
 *
 * 特性：
 * - 统一管理首页刷新（[refresh]）、追加下一页（[loadNextPage]）、失败重试（[retry]）与局部列表更新（[updateItems]）。
 * - 刷新时自动取消未完成的旧请求，避免并发返回导致页码错乱，并透传 [CancellationException]。
 *
 * 示例：
 * ```kotlin
 * private val coursePaginator = Paginator(
 *     scope = viewModelScope,
 *     onStateChange = { paged -> _uiState.update { it.copy(courses = paged) } },
 *     fetchPage = { query ->
 *         courseApi.list(query).map { resp ->
 *             resp.mapItems { CourseSummary(id = it.id.toString(), name = it.name) }
 *         }
 *     },
 * )
 *
 * init { coursePaginator.refresh() }
 * ```
 *
 * @param scope 用于执行分页请求的协程作用域（通常为 `viewModelScope`）。
 * @param pageSize 每页请求条数，默认 `20`。
 * @param onStateChange 状态每次变更时的回调，可用于同步外层页面 `UiState`。
 * @param fetchPage 实际执行分页请求的函数，入参为 [PageQuery]，返回 [PagedResponse]。
 */
class Paginator<T>(
    private val scope: CoroutineScope,
    private val pageSize: Int = 20,
    private val onStateChange: (PagedState<T>) -> Unit = {},
    private val fetchPage: suspend (PageQuery) -> Result<PagedResponse<T>>,
    private val getKey: (T) -> String
) {
    private enum class RequestType { REFRESH, APPEND }

    private val _state = MutableStateFlow(PagedState<T>(pageSize = pageSize))

    /** 当前分页状态流。 */
    val state: StateFlow<PagedState<T>> = _state.asStateFlow()

    private var activeJob: Job? = null
    private var lastRequest: RequestType = RequestType.REFRESH

    /**
     * 刷新并重新请求第 1 页；若当前有进行中的请求会先取消。
     *
     * @param clearItems 为 `true` 时会先清空现有列表并重置页码（适用于切换搜索词或筛选条件）；
     * 为 `false` 时保留现有数据并在顶部展示刷新状态。
     */
    fun refresh(clearItems: Boolean = false) {
        activeJob?.cancel()
        lastRequest = RequestType.REFRESH
        activeJob = scope.launch {
            commit {
                it.copy(
                    // 置为加载中
                    items = if (clearItems) emptyList() else it.items,
                    page = if (clearItems) 0 else it.page,
                    pageSize = pageSize,
                    isRefreshing = true,
                    isAppending = false,
                    errorMessage = null,
                )
            }

            fetchPage(PageQuery(page = 1, pageSize = pageSize)).fold(
                onSuccess = { resp ->
                    commit {
                        it.copy(
                            items = resp.items,
                            page = resp.page,
                            pageSize = resp.pageSize,
                            totalCount = resp.totalCount,
                            hasNextPage = resp.hasNextPage,
                            isRefreshing = false,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    commit {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = error.userMessage,
                        )
                    }
                },
            )
        }
    }

    /**
     * 加载下一页。
     */
    fun loadNextPage() {
        val current = _state.value
        if (!current.canLoadMore) return
        appendPage(current.page + 1)
    }

    /**
     * 重试上一次失败的请求：若上次失败的是刷新（或尚未成功加载过第 1 页），则执行 [refresh]；
     * 否则重新请求 `page + 1`。
     */
    fun retry() {
        val current = _state.value
        if (current.isRefreshing || current.isAppending) return
        if (lastRequest == RequestType.REFRESH || current.page == 0) {
            refresh()
        } else {
            appendPage(current.page + 1)
        }
    }

    /**
     * 在不重新请求网络的情况下本地更新列表项（如新增、编辑或删除单项），并同步通知 [state] 与 [onStateChange]。
     */
    fun updateItems(transform: (List<T>) -> List<T>) {
        commit { it.copy(items = transform(it.items)) }
    }

    private fun appendPage(targetPage: Int) {
        activeJob?.cancel()
        lastRequest = RequestType.APPEND
        activeJob = scope.launch {
            commit {
                it.copy(
                    isAppending = true,
                    errorMessage = null,
                )
            }

            fetchPage(PageQuery(page = targetPage, pageSize = pageSize)).fold(
                onSuccess = { resp ->
                    commit {
                        it.copy(
                            items = (it.items + resp.items).distinctBy { item -> getKey(item) },
                            page = resp.page,
                            pageSize = resp.pageSize,
                            totalCount = resp.totalCount,
                            hasNextPage = resp.hasNextPage,
                            isAppending = false,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    commit {
                        it.copy(
                            isAppending = false,
                            errorMessage = error.userMessage,
                        )
                    }
                },
            )
        }
    }

    /**
     * 保留当前 [_state] 的前提下，使用 [transform] 更新之，并在更新后通过 `onStateChange` 通知最新状态。
     *
     * @param transform 接收当前 [PagedState] 并返回更新后 [PagedState] 的转换函数。
     */
    private inline fun commit(transform: (PagedState<T>) -> PagedState<T>) {
        onStateChange(_state.updateAndGet(transform))
    }
}

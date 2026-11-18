package ink.qingshuige.csuplusplus.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ink.qingshuige.csuplusplus.core.pagination.PagedState
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 基于 [LazyColumn] 的轻量分页列表组件，配合 [PagedState] 与 [ink.qingshuige.csuplusplus.core.pagination.Paginator] 使用。
 *
 * 自动处理以下状态与行为：
 * - 触底预加载：通过 `snapshotFlow` 监听列表可见区，当允许加载更多且距末尾不超过 [prefetchDistance] 项时自动调用 [onLoadMore]。
 * - 首屏加载中、首屏错误重试、空数据占位（均有默认 Material 3 实现，可通过插槽参数覆盖）。
 * - 列表已有数据时的顶部刷新进度条（`isRefreshing`）与底部追加加载/失败重试/无更多数据提示。
 *
 * 使用示例：
 * ```kotlin
 * PaginatedLazyColumn(
 *     pagedState = uiState.courses,
 *     onLoadMore = viewModel::loadMoreCourses,
 *     onRetry = viewModel::retryCourses,
 *     modifier = Modifier.fillMaxSize(),
 *     contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
 *     verticalArrangement = Arrangement.spacedBy(12.dp),
 *     itemKey = { it.id },
 * ) { course ->
 *     CourseCard(course = course)
 * }
 * ```
 *
 * @param pagedState 当前分页状态。
 * @param onLoadMore 触发加载下一页的回调（通常传入 `paginator::loadNextPage`）。
 * @param onRetry 触发失败重试的回调（通常传入 `paginator::retry`）。
 * @param modifier 外层容器修饰符。
 * @param listState 底层 [LazyColumn] 的滚动状态。
 * @param contentPadding 列表内容内边距。
 * @param verticalArrangement 列表项垂直排列方式。
 * @param prefetchDistance 距离最后一项还剩多少个可见项时提前触发 [onLoadMore]，默认 `3`。
 * @param itemKey 列表项稳定 key 提取函数（推荐传入，如 `{ it.id }`）。
 * @param headerContent 可选的列表头部插槽。
 * @param loadingContent 首屏加载中展示的内容。
 * @param emptyContent 列表为空时展示的内容。
 * @param errorContent 首屏加载失败时展示的内容，接收错误信息与重试回调。
 * @param appendLoadingContent 追加加载下一页时在底部展示的内容。
 * @param appendErrorContent 追加加载失败（或非空刷新失败）时在底部展示的内容，接收错误信息与重试回调。
 * @param noMoreContent 没有更多数据时在底部展示的内容；为 `null` 时不显示。
 * @param itemContent 单个列表项的渲染内容。
 */
@Composable
fun <T> PaginatedLazyColumn(
    pagedState: PagedState<T>,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    prefetchDistance: Int = 3,
    itemKey: ((T) -> Any)? = null,
    headerContent: (LazyListScope.() -> Unit)? = null,
    loadingContent: @Composable () -> Unit = { DefaultPageLoading() },
    emptyContent: @Composable () -> Unit = { DefaultPageEmpty() },
    errorContent: @Composable (message: String, onRetry: () -> Unit) -> Unit = { message, retry ->
        DefaultPageError(message = message, onRetry = retry)
    },
    appendLoadingContent: @Composable () -> Unit = { DefaultAppendLoading() },
    appendErrorContent: @Composable (message: String, onRetry: () -> Unit) -> Unit = { message, retry ->
        DefaultAppendError(message = message, onRetry = retry)
    },
    noMoreContent: (@Composable () -> Unit)? = null,
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    val currentState by rememberUpdatedState(pagedState)
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(listState, prefetchDistance) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val shouldCheck = currentState.items.isNotEmpty() && currentState.canLoadMore
            Triple(total, lastVisible, shouldCheck)
        }
            .distinctUntilChanged()
            .collect { (total, lastVisible, shouldCheck) ->
                if (shouldCheck && total > 0 && lastVisible >= total - 1 - prefetchDistance) {
                    currentOnLoadMore()
                }
            }
    }

    Box(modifier = modifier) {
        when {
            headerContent == null && pagedState.isFirstLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    loadingContent()
                }
            }

            headerContent == null && pagedState.isFirstError -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    errorContent(pagedState.errorMessage.orEmpty(), onRetry)
                }
            }

            headerContent == null && pagedState.isEmpty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    emptyContent()
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    verticalArrangement = verticalArrangement,
                ) {
                    headerContent?.invoke(this)

                    when {
                        pagedState.isFirstLoading -> {
                            item(key = "__pagination_first_loading__") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    loadingContent()
                                }
                            }
                        }

                        pagedState.isFirstError -> {
                            item(key = "__pagination_first_error__") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    errorContent(pagedState.errorMessage.orEmpty(), onRetry)
                                }
                            }
                        }

                        pagedState.isEmpty -> {
                            item(key = "__pagination_empty__") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    emptyContent()
                                }
                            }
                        }

                        else -> {
                            items(
                                items = pagedState.items,
                                key = itemKey,
                                itemContent = itemContent,
                            )

                            when {
                                pagedState.isAppending -> {
                                    item(key = "__pagination_append_loading__") {
                                        appendLoadingContent()
                                    }
                                }

                                pagedState.errorMessage != null -> {
                                    item(key = "__pagination_append_error__") {
                                        appendErrorContent(pagedState.errorMessage, onRetry)
                                    }
                                }

                                !pagedState.hasNextPage && pagedState.items.isNotEmpty() && noMoreContent != null -> {
                                    item(key = "__pagination_no_more__") {
                                        noMoreContent()
                                    }
                                }
                            }
                        }
                    }
                }

                if (pagedState.isRefreshing && pagedState.items.isNotEmpty()) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultPageLoading() {
    CircularProgressIndicator()
}

@Composable
private fun DefaultPageEmpty() {
    Text(
        text = "暂无数据",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DefaultPageError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        FilledTonalButton(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun DefaultAppendLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.5.dp,
        )
    }
}

@Composable
private fun DefaultAppendError(
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = onRetry) {
            Text("重试")
        }
    }
}

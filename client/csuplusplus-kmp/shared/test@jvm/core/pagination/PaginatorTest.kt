package ink.qingshuige.csuplusplus.core.pagination

import ink.qingshuige.csuplusplus.data.dto.PagedResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PaginatorTest {

    @Test
    fun removeItemsRemovesMatchingEntriesAndUpdatesTotalCount() = runBlocking {
        var latestState = PagedState<Int>()
        val paginator = Paginator(
            scope = this,
            pageSize = 20,
            onStateChange = { latestState = it },
            fetchPage = { query ->
                Result.success(
                    PagedResponse(
                        items = listOf(1, 2, 3),
                        page = query.page,
                        pageSize = query.pageSize,
                        totalCount = 5,
                    ),
                )
            },
            getKey = { it.toString() },
        )

        paginator.refresh()
        kotlinx.coroutines.yield()
        assertEquals(listOf(1, 2, 3), latestState.items)
        assertEquals(5L, latestState.totalCount)

        paginator.removeItems { it == 2 }
        assertEquals(listOf(1, 3), latestState.items)
        assertEquals(4L, latestState.totalCount)

        paginator.removeItems { it == 999 }
        assertEquals(listOf(1, 3), latestState.items)
        assertEquals(4L, latestState.totalCount)

        paginator.clear()
        assertEquals(emptyList(), latestState.items)
        assertEquals(0, latestState.page)
        assertEquals(0L, latestState.totalCount)
    }

    @Test
    fun fetchAllPagesCollectsAcrossMultiplePages() = runBlocking {
        val result = fetchAllPages(pageSize = 2) { query ->
            val items = when (query.page) {
                1 -> listOf("a", "b")
                2 -> listOf("c")
                else -> emptyList()
            }
            Result.success(
                PagedResponse(
                    items = items,
                    page = query.page,
                    pageSize = query.pageSize,
                    totalCount = 3,
                ),
            )
        }

        assertEquals(listOf("a", "b", "c"), result.getOrThrow())
    }
}

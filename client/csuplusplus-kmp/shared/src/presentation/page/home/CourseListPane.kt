package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.composables.icons.lucide.*
import ink.qingshuige.csuplusplus.core.pagination.PagedState
import ink.qingshuige.csuplusplus.presentation.component.PaginatedLazyColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CourseListPane(
    universityName: String?,
    universityEmblem: Any? = null,
    searchQuery: String,
    activeSearchQuery: String,
    coursesState: PagedState<CourseSummary>,
    highlightedCourseId: Long?,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onClearSearch: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onCourseClick: (CourseSummary) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var requestSearchFocus by remember { mutableStateOf(false) }

    val isSearching = activeSearchQuery.isNotBlank()
    val subtitle = when {
        isSearching && coursesState.isFirstLoading -> "搜索课程中..."
        isSearching && coursesState.page > 0 -> "找到 ${coursesState.totalCount} 门匹配课程"
        isSearching -> "搜索课程"
        coursesState.isFirstLoading -> "加载课程中..."
        coursesState.page > 0 -> "共 ${coursesState.totalCount} 门课程"
        else -> "发现与浏览课程资料"
    }

    LaunchedEffect(searchQuery, activeSearchQuery) {
        if ((searchQuery.isNotEmpty() || activeSearchQuery.isNotEmpty()) && !isSearchExpanded) {
            isSearchExpanded = true
        }
    }

    LaunchedEffect(activeSearchQuery) {
        listState.scrollToItem(0)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedContent(
                targetState = isSearchExpanded,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                },
                label = "CourseListTopBar",
            ) { expanded ->
                if (expanded) {
                    LaunchedEffect(requestSearchFocus) {
                        if (requestSearchFocus) {
                            searchFocusRequester.requestFocus()
                            requestSearchFocus = false
                        }
                    }

                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    requestSearchFocus = false
                                    isSearchExpanded = false
                                    onClearSearch()
                                },
                            ) {
                                Icon(Lucide.ArrowLeft, contentDescription = "关闭搜索")
                            }
                        },
                        title = {
                            CourseSearchField(
                                query = searchQuery,
                                onQueryChange = onSearchQueryChange,
                                onSearch = {
                                    focusManager.clearFocus()
                                    onSearchSubmit()
                                },
                                onClear = {
                                    onClearSearch()
                                    requestSearchFocus = true
                                },
                                focusRequester = searchFocusRequester,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        },
                        scrollBehavior = scrollBehavior,
                    )
                } else {
                    TopAppBar(
                        title = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "课程列表",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        actions = {
                            universityName?.takeIf { it.isNotBlank() }?.let { name ->
                                UniversityBadge(
                                    name = name,
                                    emblem = universityEmblem,
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    isSearchExpanded = true
                                    requestSearchFocus = true
                                },
                            ) {
                                Icon(Lucide.Search, contentDescription = "搜索课程")
                            }
                            IconButton(onClick = onRefresh) {
                                Icon(Lucide.RefreshCw, contentDescription = "刷新课程")
                            }
                        },
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        },
    ) { innerPadding ->
        PaginatedLazyColumn(
            pagedState = coursesState,
            onLoadMore = onLoadMore,
            onRetry = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            listState = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            itemKey = { it.id },
            emptyContent = {
                if (isSearching) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "未找到与「$activeSearchQuery」相关的课程",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        FilledTonalButton(
                            onClick = {
                                focusManager.clearFocus()
                                requestSearchFocus = false
                                isSearchExpanded = false
                                onClearSearch()
                            },
                        ) {
                            Text("查看全部课程")
                        }
                    }
                } else {
                    Text(
                        text = "暂无课程",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        ) { course ->
            CourseCard(
                course = course,
                isSelected = course.id == highlightedCourseId,
                onClick = { onCourseClick(course) },
            )
        }
    }
}

@Composable
private fun UniversityBadge(
    name: String,
    emblem: Any? = null,
    modifier: Modifier = Modifier,
) {
    val normalizedEmblem = if (emblem is String) emblem.trim().takeIf { it.isNotEmpty() } else emblem

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 5.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (normalizedEmblem) {
                null -> DefaultUniversityEmblem()
                is Painter -> {
                    Image(
                        painter = normalizedEmblem,
                        contentDescription = "校徽",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape),
                    )
                }

                else -> {
                    val painter = rememberAsyncImagePainter(model = normalizedEmblem)
                    val state by painter.state.collectAsState()
                    if (state is AsyncImagePainter.State.Success) {
                        Image(
                            painter = painter,
                            contentDescription = "校徽",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        DefaultUniversityEmblem()
                    }
                }
            }
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp),
            )
        }
    }
}

@Composable
private fun DefaultUniversityEmblem() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.size(26.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Lucide.GraduationCap,
                contentDescription = "校徽",
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun CourseSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Lucide.Search,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "搜索课程名称或简称",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "清除搜索",
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseCard(
    course: CourseSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val titleColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryTextColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val pillContainerColor = if (isSelected) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val pillContentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CourseTitleText(
                name = course.name,
                alias = course.alias,
                titleStyle = MaterialTheme.typography.titleLarge,
                aliasStyle = MaterialTheme.typography.bodySmall,
                titleColor = titleColor,
                aliasColor = secondaryTextColor,
                maxLines = 2,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Lucide.GraduationCap,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = secondaryTextColor,
                )
                Text(
                    text = course.universityName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!course.collegeName.isNullOrBlank() || course.hours != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    course.collegeName?.takeIf { it.isNotBlank() }?.let { college ->
                        MetadataPill(
                            text = college,
                            containerColor = pillContainerColor,
                            contentColor = pillContentColor,
                        )
                    }
                    course.hours?.let { hours ->
                        MetadataPill(
                            text = "$hours 学时",
                            icon = Lucide.Clock,
                            containerColor = pillContainerColor,
                            contentColor = pillContentColor,
                        )
                    }
                }
            }
        }
    }
}

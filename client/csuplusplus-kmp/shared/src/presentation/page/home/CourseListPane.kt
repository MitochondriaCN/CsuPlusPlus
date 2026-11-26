package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import ink.qingshuige.csuplusplus.core.pagination.PagedState
import ink.qingshuige.csuplusplus.presentation.component.PaginatedLazyColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CourseListPane(
    scope: CourseScopeUiState,
    fallbackUniversityName: String?,
    universityEmblem: Any? = null,
    searchQuery: String,
    activeSearchQuery: String,
    coursesState: PagedState<CourseSummary>,
    highlightedCourseId: Long?,
    onSelectUniversity: (Long) -> Unit,
    onSelectCollege: (Long?) -> Unit,
    onSwitchBackToHomeUniversity: () -> Unit,
    onRetryUniversities: () -> Unit,
    onRetryColleges: () -> Unit,
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

    val displayedUniversityName = (scope.selectedUniversityName ?: fallbackUniversityName)
        ?.takeIf { it.isNotBlank() }
    val isSearching = activeSearchQuery.isNotBlank()
    val subtitle = when {
        scope.isViewingOtherUniversity -> "暂不支持跨校查看课程"
        isSearching && coursesState.isFirstLoading -> {
            if (scope.selectedCollegeId != null) "全校搜索中..." else "搜索课程中..."
        }

        isSearching && coursesState.page > 0 -> {
            if (scope.selectedCollegeId != null) {
                "全校找到 ${coursesState.totalCount} 门匹配课程"
            } else {
                "找到 ${coursesState.totalCount} 门匹配课程"
            }
        }

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

    LaunchedEffect(scope.isViewingOtherUniversity) {
        if (scope.isViewingOtherUniversity) {
            focusManager.clearFocus()
            requestSearchFocus = false
            isSearchExpanded = false
        }
    }

    LaunchedEffect(activeSearchQuery, scope.selectedCollegeId, scope.selectedUniversityId) {
        listState.scrollToItem(0)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactHeader = maxWidth < 460.dp
        val badgeMaxTextWidth = if (isCompactHeader) 68.dp else 120.dp

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AnimatedContent(
                    targetState = isSearchExpanded && !scope.isViewingOtherUniversity,
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
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                            actions = {
                                if (displayedUniversityName != null) {
                                    Row(
                                        modifier = Modifier.padding(end = if (isCompactHeader) 2.dp else 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(if (isCompactHeader) 4.dp else 6.dp),
                                    ) {
                                        UniversitySelectorBadge(
                                            universityName = displayedUniversityName,
                                            emblem = universityEmblem,
                                            scope = scope,
                                            compact = isCompactHeader,
                                            maxTextWidth = badgeMaxTextWidth,
                                            onSelectUniversity = onSelectUniversity,
                                            onRetryUniversities = onRetryUniversities,
                                        )
                                        CollegeSelectorBadge(
                                            scope = scope,
                                            compact = isCompactHeader,
                                            maxTextWidth = badgeMaxTextWidth,
                                            onSelectCollege = onSelectCollege,
                                            onRetryColleges = onRetryColleges,
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        isSearchExpanded = true
                                        requestSearchFocus = true
                                    },
                                    enabled = !scope.isViewingOtherUniversity,
                                    modifier = if (isCompactHeader) Modifier.size(40.dp) else Modifier,
                                ) {
                                    Icon(Lucide.Search, contentDescription = "搜索课程")
                                }
                                IconButton(
                                    onClick = onRefresh,
                                    modifier = if (isCompactHeader) Modifier.size(40.dp) else Modifier,
                                ) {
                                    Icon(Lucide.RefreshCw, contentDescription = "刷新课程")
                                }
                            },
                            scrollBehavior = scrollBehavior,
                        )
                    }
                }
            },
        ) { innerPadding ->
            if (scope.isViewingOtherUniversity) {
                CrossUniversityUnsupportedPlaceholder(
                    universityName = displayedUniversityName ?: "所选大学",
                    homeUniversityName = scope.homeUniversityName,
                    collegeCount = scope.colleges.size,
                    isLoadingColleges = scope.isLoadingColleges,
                    onSwitchBack = onSwitchBackToHomeUniversity,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            } else {
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
                        when {
                            isSearching -> {
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
                            }

                            scope.selectedCollegeId != null -> {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        text = "「${scope.selectedCollegeName ?: "该学院"}」暂无课程",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                    FilledTonalButton(onClick = { onSelectCollege(null) }) {
                                        Text("查看全部学院")
                                    }
                                }
                            }

                            else -> {
                                Text(
                                    text = "暂无课程",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
    }
}

@Composable
private fun UniversitySelectorBadge(
    universityName: String,
    emblem: Any?,
    scope: CourseScopeUiState,
    compact: Boolean,
    maxTextWidth: Dp,
    onSelectUniversity: (Long) -> Unit,
    onRetryUniversities: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        ScopeSelectorBadge(
            label = universityName,
            fallbackIcon = Lucide.GraduationCap,
            emblem = if (scope.isViewingOtherUniversity) null else emblem,
            emblemContentDescription = "校徽",
            maxTextWidth = maxTextWidth,
            compact = compact,
            onClick = if (scope.canSwitchUniversity) {
                { expanded = true }
            } else {
                null
            },
        )

        if (scope.canSwitchUniversity) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 360.dp),
            ) {
                when {
                    scope.isLoadingUniversities && scope.universities.isEmpty() -> {
                        DropdownMenuItem(
                            text = { Text("加载大学列表中...") },
                            leadingIcon = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            },
                            enabled = false,
                            onClick = {},
                        )
                    }

                    scope.universitiesError != null && scope.universities.isEmpty() -> {
                        DropdownMenuItem(
                            text = { Text("加载失败，点击重试") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Lucide.RefreshCw,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            onClick = onRetryUniversities,
                        )
                    }

                    scope.universities.isEmpty() -> {
                        DropdownMenuItem(
                            text = { Text("暂无可选大学") },
                            enabled = false,
                            onClick = {},
                        )
                    }

                    else -> {
                        scope.universities.forEach { university ->
                            val selected = university.id == scope.selectedUniversityId
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = university.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                trailingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Lucide.Check,
                                            contentDescription = "已选中",
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else {
                                    null
                                },
                                onClick = {
                                    expanded = false
                                    onSelectUniversity(university.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollegeSelectorBadge(
    scope: CourseScopeUiState,
    compact: Boolean,
    maxTextWidth: Dp,
    onSelectCollege: (Long?) -> Unit,
    onRetryColleges: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val canSelectCollege = scope.selectedUniversityId != null

    Box {
        ScopeSelectorBadge(
            label = scope.selectedCollegeName ?: "全部学院",
            fallbackIcon = Lucide.Building2,
            emblemContentDescription = "学院",
            maxTextWidth = maxTextWidth,
            compact = compact,
            onClick = if (canSelectCollege) {
                { expanded = true }
            } else {
                null
            },
        )

        if (canSelectCollege) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 360.dp),
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "全部学院",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingIcon = if (scope.selectedCollegeId == null) {
                        {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = "已选中",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelectCollege(null)
                    },
                )

                HorizontalDivider()

                when {
                    scope.isLoadingColleges && scope.colleges.isEmpty() -> {
                        DropdownMenuItem(
                            text = { Text("加载学院列表中...") },
                            leadingIcon = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            },
                            enabled = false,
                            onClick = {},
                        )
                    }

                    scope.collegesError != null && scope.colleges.isEmpty() -> {
                        DropdownMenuItem(
                            text = { Text("加载失败，点击重试") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Lucide.RefreshCw,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            onClick = onRetryColleges,
                        )
                    }

                    scope.colleges.isEmpty() -> {
                        DropdownMenuItem(
                            text = { Text("该校暂无学院") },
                            enabled = false,
                            onClick = {},
                        )
                    }

                    else -> {
                        scope.colleges.forEach { college ->
                            val selected = college.id == scope.selectedCollegeId
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = college.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                trailingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Lucide.Check,
                                            contentDescription = "已选中",
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else {
                                    null
                                },
                                onClick = {
                                    expanded = false
                                    onSelectCollege(college.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CrossUniversityUnsupportedPlaceholder(
    universityName: String,
    homeUniversityName: String?,
    collegeCount: Int,
    isLoadingColleges: Boolean,
    onSwitchBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Lucide.Building2,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Text(
                text = "暂不支持跨校查看课程",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            val collegeSummary = if (isLoadingColleges) {
                "正在同步该校学院列表..."
            } else {
                "已同步该校 $collegeCount 个学院，可提前在右上角查看或选择学院；后端补充跨校课程查询接口后即可直接展示。"
            }
            Text(
                text = "当前已切换至「$universityName」。$collegeSummary",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (homeUniversityName != null) {
                FilledTonalButton(onClick = onSwitchBack) {
                    Text("切回$homeUniversityName")
                }
            }
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

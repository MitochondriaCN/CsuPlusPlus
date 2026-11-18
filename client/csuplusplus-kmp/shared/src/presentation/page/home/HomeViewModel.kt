package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CourseSummary(
    val id: String,
    val code: String,
    val name: String,
    val teacher: String,
    val term: String,
    val credits: Double,
    val fileCount: Int,
)

enum class CourseFileType(val label: String) {
    PDF("PDF"),
    SLIDES("PPT"),
    ARCHIVE("ZIP"),
    CODE("CODE"),
}

data class CourseFile(
    val id: String,
    val name: String,
    val sizeText: String,
    val updatedAt: String,
    val type: CourseFileType,
)

data class CourseDetail(
    val summary: CourseSummary,
    val location: String,
    val schedule: String,
    val description: String,
    val files: List<CourseFile>,
)

sealed interface CourseDetailUiState {
    data object Empty : CourseDetailUiState
    data object Loading : CourseDetailUiState
    data class Success(val detail: CourseDetail) : CourseDetailUiState
}

data class HomeUiState(
    val isLoadingCourses: Boolean = true,
    val courses: List<CourseSummary> = emptyList(),
    val selectedCourseId: String? = null,
    val detailState: CourseDetailUiState = CourseDetailUiState.Empty,
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var detailJob: Job? = null

    init {
        loadCourses()
    }

    fun refreshCourses() {
        loadCourses()
    }

    fun selectCourse(courseId: String) {
        val currentState = _uiState.value
        if (currentState.selectedCourseId == courseId &&
            currentState.detailState is CourseDetailUiState.Success
        ) {
            return
        }

        val summary = currentState.courses.find { it.id == courseId } ?: return

        detailJob?.cancel()
        _uiState.update {
            it.copy(
                selectedCourseId = courseId,
                detailState = CourseDetailUiState.Loading,
            )
        }

        detailJob = viewModelScope.launch {
            // 模拟请求课程详情与课件列表接口
            delay(220)
            val detail = mockCourseDetail(summary)
            _uiState.update {
                it.copy(detailState = CourseDetailUiState.Success(detail))
            }
        }
    }

    private fun loadCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCourses = true) }
            // 模拟请求课程列表接口
            delay(180)
            _uiState.update {
                it.copy(
                    isLoadingCourses = false,
                    courses = mockCourses,
                )
            }
        }
    }

    private fun mockCourseDetail(summary: CourseSummary): CourseDetail = when (summary.id) {
        "cs-101" -> CourseDetail(
            summary = summary,
            location = "中南大学新校区 B 座 204",
            schedule = "周一 1-2 节 · 周三 3-4 节",
            description = "系统讲解线性表、树、图、查找与排序等核心数据结构与算法分析方法，结合上机实验夯实工程编码能力。",
            files = listOf(
                CourseFile("f1", "第01章_绪论与复杂度分析.pdf", "2.4 MB", "2026-09-04", CourseFileType.PDF),
                CourseFile("f2", "第02章_线性表与链表实现.pptx", "5.1 MB", "2026-09-11", CourseFileType.SLIDES),
                CourseFile("f3", "实验一_平衡二叉树模板代码.zip", "640 KB", "2026-09-25", CourseFileType.ARCHIVE),
                CourseFile("f4", "图论最短路径参考源码.kt", "18 KB", "2026-10-12", CourseFileType.CODE),
            ),
        )

        "cs-202" -> CourseDetail(
            summary = summary,
            location = "中南大学新校区 C 座 308",
            schedule = "周二 3-4 节 · 周四 1-2 节",
            description = "围绕进程线程调度、内存虚拟化、文件系统与并发同步原语展开，配套 Nachos / xv6 实验。",
            files = listOf(
                CourseFile("f5", "OS_Lab1_系统调用与进程切换指南.pdf", "3.8 MB", "2026-09-09", CourseFileType.PDF),
                CourseFile("f6", "第04讲_页面置换与TLB机制.pptx", "4.6 MB", "2026-09-20", CourseFileType.SLIDES),
                CourseFile("f7", "xv6_lab_starter.tar.gz", "12.3 MB", "2026-10-03", CourseFileType.ARCHIVE),
            ),
        )

        "cs-305" -> CourseDetail(
            summary = summary,
            location = "中南大学信息楼 108",
            schedule = "周二 5-6 节 · 周五 1-2 节",
            description = "自顶向下剖析应用层、传输层（TCP/UDP 拥塞控制）、网络层路由协议及链路层核心原理。",
            files = listOf(
                CourseFile("f8", "Wireshark_TCP三次握手抓包实验.pdf", "1.9 MB", "2026-09-14", CourseFileType.PDF),
                CourseFile("f9", "第05章_OSPF与BGP路由协议.pptx", "6.2 MB", "2026-10-08", CourseFileType.SLIDES),
            ),
        )

        "cs-401" -> CourseDetail(
            summary = summary,
            location = "中南大学新校区 A 座 412",
            schedule = "周一 5-6 节 · 周四 3-4 节",
            description = "涵盖词法分析、LL/LR 语法分析、语义制导翻译、中间代码生成与常见优化技术。",
            files = listOf(
                CourseFile("f10", "ANTLR4_入门与表达式求值.pdf", "2.1 MB", "2026-09-18", CourseFileType.PDF),
                CourseFile("f11", "SysY_文法定义与测试用例.zip", "4.4 MB", "2026-10-15", CourseFileType.ARCHIVE),
                CourseFile("f12", "MiniLexer.kt", "12 KB", "2026-10-19", CourseFileType.CODE),
            ),
        )

        else -> CourseDetail(
            summary = summary,
            location = "中南大学新校区 D 座 105",
            schedule = "周三 5-6 节",
            description = "以敏捷迭代和现代软件架构为核心，演练需求建模、自动化测试与持续集成实践。",
            files = listOf(
                CourseFile("f13", "课程项目需求规格说明书模板.pdf", "980 KB", "2026-09-12", CourseFileType.PDF),
                CourseFile("f14", "第03讲_领域驱动设计与分层架构.pptx", "3.5 MB", "2026-09-28", CourseFileType.SLIDES),
            ),
        )
    }

    private companion object {
        val mockCourses = listOf(
            CourseSummary(
                id = "cs-101",
                code = "COMP2011",
                name = "数据结构与算法",
                teacher = "张明远",
                term = "2026 秋季",
                credits = 4.0,
                fileCount = 4,
            ),
            CourseSummary(
                id = "cs-202",
                code = "COMP3002",
                name = "操作系统原理",
                teacher = "李思远",
                term = "2026 秋季",
                credits = 3.5,
                fileCount = 3,
            ),
            CourseSummary(
                id = "cs-305",
                code = "COMP3018",
                name = "计算机网络",
                teacher = "王若晴",
                term = "2026 秋季",
                credits = 3.0,
                fileCount = 2,
            ),
            CourseSummary(
                id = "cs-401",
                code = "COMP4005",
                name = "编译原理与实践",
                teacher = "陈子昂",
                term = "2026 秋季",
                credits = 3.5,
                fileCount = 3,
            ),
            CourseSummary(
                id = "cs-410",
                code = "SOFT3020",
                name = "软件工程导论",
                teacher = "周嘉禾",
                term = "2026 秋季",
                credits = 2.5,
                fileCount = 2,
            ),
        )
    }
}

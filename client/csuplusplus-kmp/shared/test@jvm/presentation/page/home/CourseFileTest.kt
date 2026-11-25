package ink.qingshuige.csuplusplus.presentation.page.home

import ink.qingshuige.csuplusplus.core.time.formatFriendlyTime
import ink.qingshuige.csuplusplus.data.dto.FileMetadataResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class CourseFileTest {

    private val utcPlus8Offset: (Long) -> Int = { 8 * 3600 }
    private val nowMillis = Instant.parse("2026-11-25T10:00:00+08:00").toEpochMilliseconds()

    @Test
    fun toCourseFileMapsMetadataAndFormatsCreatedAt() {
        val dto = FileMetadataResponse(
            id = 7L,
            guidFilename = "a1b2c3.pdf",
            friendlyFilename = "高数课件.pdf",
            extension = ".pdf",
            sha256Hash = "deadbeef",
            uploaderUserId = 1L,
            uploaderUserNickname = "Tester",
            createdAt = "2026-11-25T08:00:00+08:00",
        )

        val courseFile = dto.toCourseFile { timestamp ->
            formatFriendlyTime(timestamp, nowMillis, utcPlus8Offset)
        }

        assertEquals(7L, courseFile.id)
        assertEquals("高数课件.pdf", courseFile.name)
        assertEquals("PDF", courseFile.extension)
        assertEquals(1L, courseFile.uploaderUserId)
        assertEquals("Tester", courseFile.uploaderName)
        assertEquals("2小时前", courseFile.createdAt)
        assertEquals(CourseFileType.PDF, courseFile.type)
    }

    @Test
    fun toCourseFileUsesDefaultFriendlyFormatterForHistoricalDate() {
        val dto = FileMetadataResponse(
            id = 8L,
            guidFilename = "notes.md",
            friendlyFilename = null,
            extension = "md",
            sha256Hash = "cafebabe",
            uploaderUserId = 2L,
            uploaderUserNickname = "Alice",
            createdAt = "2024-06-15T12:00:00Z",
        )

        val courseFile = dto.toCourseFile()

        assertEquals("notes.md", courseFile.name)
        assertEquals("2024年6月15日", courseFile.createdAt)
        assertEquals(CourseFileType.MARKDOWN, courseFile.type)
    }

    @Test
    fun toCourseFileMapsMarkdownAndImageExtensions() {
        fun fileWithExt(extension: String) = FileMetadataResponse(
            id = 9L,
            guidFilename = "sample.$extension",
            friendlyFilename = null,
            extension = extension,
            sha256Hash = "deadbeef",
            uploaderUserId = 1L,
            uploaderUserNickname = "Tester",
            createdAt = "2026-11-25T08:00:00+08:00",
        ).toCourseFile { "刚刚" }

        assertEquals(CourseFileType.MARKDOWN, fileWithExt(".md").type)
        assertEquals(CourseFileType.MARKDOWN, fileWithExt("markdown").type)
        assertEquals("MD", fileWithExt("md").type.label)

        listOf(".png", "jpg", "JPEG", "gif", "webp").forEach { ext ->
            val mapped = fileWithExt(ext)
            assertEquals(CourseFileType.IMAGE, mapped.type)
            assertEquals("IMAGE", mapped.type.label)
        }

        assertEquals(CourseFileType.CODE, fileWithExt(".kt").type)
    }

    @Test
    fun formatCourseFileSubtitleIncludesCreatedAtAndSkipsBlankParts() {
        assertEquals(
            "PDF · 上传者 Tester · 2小时前",
            formatCourseFileSubtitle(
                typeLabel = "PDF",
                uploaderName = "Tester",
                createdAt = "2小时前",
            ),
        )
        assertEquals(
            "PDF · 上传者 Tester",
            formatCourseFileSubtitle(
                typeLabel = "PDF",
                uploaderName = "Tester",
                createdAt = "   ",
            ),
        )
        assertEquals(
            "PDF · 2小时前",
            formatCourseFileSubtitle(
                typeLabel = "PDF",
                uploaderName = " ",
                createdAt = "2小时前",
            ),
        )
    }
}

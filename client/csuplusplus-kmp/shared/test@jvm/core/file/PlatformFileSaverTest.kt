package ink.qingshuige.csuplusplus.core.file

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlatformFileSaverTest {

    @Test
    fun resolvesNameWithEmbeddedExtensionWithoutDuplication() {
        val parts = resolveDownloadFileParts(
            suggestedName = "高等数学第一章.pdf",
            fallbackExtension = "PDF",
        )
        assertEquals("高等数学第一章", parts.baseName)
        assertEquals("pdf", parts.extension)
        assertEquals("高等数学第一章.pdf", parts.fullName)
    }

    @Test
    fun appendsFallbackExtensionWhenNameHasNoExtension() {
        val parts = resolveDownloadFileParts(
            suggestedName = "实验指导书",
            fallbackExtension = ".DOCX",
        )
        assertEquals("实验指导书", parts.baseName)
        assertEquals("docx", parts.extension)
        assertEquals("实验指导书.docx", parts.fullName)
    }

    @Test
    fun sanitizesInvalidPathCharacters() {
        val parts = resolveDownloadFileParts(
            suggestedName = "2026:第1讲/课件*?.pptx",
            fallbackExtension = "PPTX",
        )
        assertEquals("2026_第1讲_课件__", parts.baseName)
        assertEquals("pptx", parts.extension)
        assertEquals("2026_第1讲_课件__.pptx", parts.fullName)
    }

    @Test
    fun handlesMissingExtensionGracefully() {
        val parts = resolveDownloadFileParts(
            suggestedName = "README",
            fallbackExtension = "  ",
        )
        assertEquals("README", parts.baseName)
        assertNull(parts.extension)
        assertEquals("README", parts.fullName)
    }
}

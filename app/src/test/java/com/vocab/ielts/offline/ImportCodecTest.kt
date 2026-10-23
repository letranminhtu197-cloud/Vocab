package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportCodecTest {
    @Test fun plainDashLinesBecomeWords() {
        val result = ImportCodec.parse(
            """
            allocate - phân bổ
            mitigate - giảm nhẹ
            """.trimIndent()
        )

        assertEquals(null, result.error)
        assertEquals(listOf("allocate", "mitigate"), result.words.map { it.term })
        assertEquals(listOf("phân bổ", "giảm nhẹ"), result.words.map { it.meaning })
    }

    @Test fun thirdPartBecomesExampleSentence() {
        val result = ImportCodec.parse("allocate - phân bổ - We allocate resources carefully.")

        assertEquals(null, result.error)
        assertEquals("We allocate resources carefully.", result.words.single().example)
    }

    @Test fun colonAndPipeSeparatorsAreSupported() {
        val result = ImportCodec.parse("mitigate: giảm nhẹ\necosystem | hệ sinh thái")

        assertEquals(null, result.error)
        assertEquals(listOf("mitigate", "ecosystem"), result.words.map { it.term })
    }

    @Test fun headerlessCsvIsAccepted() {
        val result = ImportCodec.parse("allocate,phân bổ\nmitigate,giảm nhẹ")

        assertEquals(null, result.error)
        assertEquals(2, result.words.size)
    }

    @Test fun tsvWithHeaderUsesTheExistingColumnMapping() {
        val result = ImportCodec.parse("term\tmeaning\ttopic\nallocate\tphân bổ\tEducation")

        assertEquals(null, result.error)
        assertEquals("Education", result.words.single().topic)
    }

    @Test fun commentsAndBlankLinesAreIgnoredAndBadLinesCounted() {
        val result = ImportCodec.parse("# ghi chú\n\nallocate - phân bổ\nkhông có dấu phân cách")

        assertEquals(null, result.error)
        assertEquals(1, result.words.size)
        assertEquals(1, result.invalidRows)
    }

    @Test fun emptyTextReturnsError() {
        val result = ImportCodec.parse("   \n  ")

        assertNotNull(result.error)
        assertTrue(result.words.isEmpty())
    }

    @Test fun bulkFileWithManyLinesIsParsedCompletely() {
        val lines = (1..50).joinToString("\n") { "word$it - nghĩa $it" }
        val result = ImportCodec.parse(lines)

        assertEquals(null, result.error)
        assertEquals(50, result.words.size)
        assertEquals("word50", result.words.last().term)
    }
}

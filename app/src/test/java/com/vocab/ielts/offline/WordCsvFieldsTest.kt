package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordCsvFieldsTest {
    @Test fun exportAndParseKeepExampleTranslationAndPausedFlag() {
        val word = WordEntity(
            term = "allocate",
            meaning = "phân bổ",
            example = "We allocate resources.",
            exampleMeaning = "Chúng tôi phân bổ nguồn lực.",
            paused = true
        )

        val exported = CsvCodec.export(listOf(word))
        val parsed = CsvCodec.parse(exported)

        assertNull(parsed.error)
        assertEquals("Chúng tôi phân bổ nguồn lực.", parsed.words.single().exampleMeaning)
        assertTrue(parsed.words.single().paused)
    }

    @Test fun legacyCsvWithoutNewColumnsUsesDefaults() {
        val parsed = CsvCodec.parse("term,meaning,example\nallocate,phân bổ,We allocate resources.")

        assertNull(parsed.error)
        assertEquals("", parsed.words.single().exampleMeaning)
        assertFalse(parsed.words.single().paused)
    }

    @Test fun vietnameseColumnHeadersAreAccepted() {
        val parsed = CsvCodec.parse(
            "từ,nghĩa,nghĩacâuvídụ,tạmdừng\nallocate,phân bổ,dịch nghĩa,có"
        )

        assertNull(parsed.error)
        assertEquals("dịch nghĩa", parsed.words.single().exampleMeaning)
        assertTrue(parsed.words.single().paused)
    }
}

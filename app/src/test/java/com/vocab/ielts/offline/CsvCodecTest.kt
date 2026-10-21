package com.vocab.ielts.offline

import org.junit.Assert.*
import org.junit.Test

class CsvCodecTest {
    @Test fun importsFlexibleHeadersAndOptionalColumns() {
        val result = CsvCodec.parse("word,meaning,example,topic\nallocate,phân bổ,\"The city, however, must act\",Economy")
        assertNull(result.error)
        assertEquals(1, result.words.size)
        assertEquals("allocate", result.words[0].term)
        assertEquals("The city, however, must act", result.words[0].example)
    }

    @Test fun parsesQuotedNewlinesAndEscapedQuotes() {
        val result = CsvCodec.parse("term,meaning,example\n\"raise awareness\",\"nâng cao nhận thức\",\"Schools say \"\"learn well\"\".\nPractice daily.\"")
        assertNull(result.error)
        assertEquals("Schools say \"learn well\".\nPractice daily.", result.words.single().example)
    }

    @Test fun reportsMissingRequiredHeadersAndSkipsInvalidRows() {
        assertNotNull(CsvCodec.parse("hello,world\nhi,there").error)
        val result = CsvCodec.parse("term,meaning\ngood,tốt\n,thiếu từ\nword,")
        assertEquals(1, result.words.size)
        assertEquals(2, result.invalidRows)
    }

    @Test fun rejectsMalformedQuotePlacement() {
        assertNotNull(CsvCodec.parse("term,meaning\\n\"word\"tail,meaning").error)
        assertNotNull(CsvCodec.parse("term,meaning\\nwo\"rd,meaning").error)
        assertNotNull(CsvCodec.parse("term,meaning\\n\"word,meaning").error)
    }

    @Test fun exportCanBeImportedWithoutLosingFields() {
        val original = WordEntity(term = "impact", ipa = "/ˈɪmpækt/", partOfSpeech = "noun", meaning = "tác động", example = "A, B and C", topic = "Society", srsLevel = 2, reviewCount = 3)
        val restored = CsvCodec.parse(CsvCodec.export(listOf(original))).words.single()
        assertEquals(original.term, restored.term)
        assertEquals(original.ipa, restored.ipa)
        assertEquals(original.meaning, restored.meaning)
        assertEquals(original.example, restored.example)
        assertEquals(original.srsLevel, restored.srsLevel)
        assertEquals(original.reviewCount, restored.reviewCount)
    }
}

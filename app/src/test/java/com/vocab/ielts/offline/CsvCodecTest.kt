package com.vocab.ielts.offline

import org.junit.Assert.*
import org.junit.Test

class CsvCodecTest {
    @Test fun rejectsMalformedUtf8InsteadOfReplacingCharacters() {
        val malformed = byteArrayOf(0x74, 0x65, 0x72, 0x6d, 0x2c, 0x6d, 0x65, 0x61, 0x6e, 0x69, 0x6e, 0x67, 0x0a, 0xC3.toByte(), 0x28, 0x2c, 0x78)
        assertTrue(runCatching { CsvCodec.decodeUtf8(malformed) }.isFailure)
    }

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
        assertNotNull(CsvCodec.parse("term,meaning\n\"word\"tail,meaning").error)
        assertNotNull(CsvCodec.parse("term,meaning\nwo\"rd,meaning").error)
        assertNotNull(CsvCodec.parse("term,meaning\n\"word,meaning").error)
    }

    @Test fun rejectsInvalidSrsMetadataInsteadOfSilentlyResettingProgress() {
        val invalidLevel = CsvCodec.parse("term,meaning,srsLevel\nallocate,phân bổ,9")
        assertNotNull(invalidLevel.error)
        assertTrue(invalidLevel.words.isEmpty())

        val negativeReviews = CsvCodec.parse("term,meaning,reviewCount\nallocate,phân bổ,-1")
        assertNotNull(negativeReviews.error)
        assertTrue(negativeReviews.words.isEmpty())
    }

    @Test fun rejectsMalformedReviewTimestamp() {
        val result = CsvCodec.parse("term,meaning,nextReviewAtMillis\nallocate,phân bổ,not-a-timestamp")
        assertNotNull(result.error)
        assertTrue(result.words.isEmpty())
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

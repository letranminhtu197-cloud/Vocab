package com.vocab.ielts.offline

import org.junit.Assert.*
import org.junit.Test

class CsvCodecTest {
    @Test fun rejectsMalformedUtf8InsteadOfReplacingCharacters() {
        val malformed = byteArrayOf(0x74, 0x65, 0x72, 0x6d, 0x2c, 0x6d, 0x65, 0x61, 0x6e, 0x69, 0x6e, 0x67, 0x0a, 0xC3.toByte(), 0x28, 0x2c, 0x78)
        assertTrue(runCatching { CsvCodec.decodeUtf8(malformed) }.isFailure)
    }

    @Test fun streamImportRejectsFilesOverConfiguredSizeLimit() {
        val oversized = ByteArray(1025) { 'a'.code.toByte() }
        val result = runCatching {
            CsvCodec.readUtf8(java.io.ByteArrayInputStream(oversized), maxBytes = 1024)
        }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("vượt giới hạn"))
    }

    @Test fun streamImportAcceptsUtf8WithinSizeLimit() {
        val input = "term,meaning\nimpact,tác động".toByteArray(Charsets.UTF_8)
        val decoded = CsvCodec.readUtf8(java.io.ByteArrayInputStream(input), maxBytes = 1024)
        assertEquals("term,meaning\nimpact,tác động", decoded)
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

    @Test fun importsAndExportsRichIeltsVocabularyFields() {
        val result = CsvCodec.parse("term,meaning,collocations,synonym,wordFamily,notes,tags\nimpact,tác động,\"have an impact on\",\"effect; influence\",\"impactful; impactfully\",\"Use in Task 2\",\"environment; society\"")
        assertNull(result.error)
        val word = result.words.single()
        assertEquals("have an impact on", word.collocations)
        assertEquals("effect; influence", word.synonyms)
        assertEquals("impactful; impactfully", word.wordFamily)
        assertEquals("Use in Task 2", word.notes)
        assertEquals("environment; society", word.tags)
        val restored = CsvCodec.parse(CsvCodec.export(listOf(word))).words.single()
        assertEquals(word.collocations, restored.collocations)
        assertEquals(word.synonyms, restored.synonyms)
        assertEquals(word.wordFamily, restored.wordFamily)
        assertEquals(word.notes, restored.notes)
        assertEquals(word.tags, restored.tags)
    }

    @Test fun exportCanBeImportedWithoutLosingFields() {
        val original = WordEntity(term = "impact", ipa = "/ˈɪmpækt/", partOfSpeech = "noun", meaning = "tác động", example = "A, B and C", topic = "Society", collocations = "have an impact on", synonyms = "effect; influence", wordFamily = "impactful", notes = "Essay use", tags = "environment, society", srsLevel = 2, reviewCount = 3)
        val restored = CsvCodec.parse(CsvCodec.export(listOf(original))).words.single()
        assertEquals(original.term, restored.term)
        assertEquals(original.ipa, restored.ipa)
        assertEquals(original.meaning, restored.meaning)
        assertEquals(original.example, restored.example)
        assertEquals(original.collocations, restored.collocations)
        assertEquals(original.synonyms, restored.synonyms)
        assertEquals(original.wordFamily, restored.wordFamily)
        assertEquals(original.notes, restored.notes)
        assertEquals(original.tags, restored.tags)
        assertEquals(original.srsLevel, restored.srsLevel)
        assertEquals(original.reviewCount, restored.reviewCount)
        assertEquals(original.isNew, restored.isNew)
    }
    @Test fun importsTheRequestedIeltsCsvHeaderWithSynonymAlias() {
        val result = CsvCodec.parse("word,phonetic,pos,meaning,collocation,synonym,topic\nallocate,/ˈæləkeɪt/,verb,phân bổ,allocate resources,assign; distribute,Economy")
        assertNull(result.error)
        val word = result.words.single()
        assertEquals("allocate", word.term)
        assertEquals("/ˈæləkeɪt/", word.ipa)
        assertEquals("verb", word.partOfSpeech)
        assertEquals("allocate resources", word.collocations)
        assertEquals("assign; distribute", word.synonyms)
        assertEquals("Economy", word.topic)
    }

}

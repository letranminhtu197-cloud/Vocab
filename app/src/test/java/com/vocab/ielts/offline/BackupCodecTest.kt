package com.vocab.ielts.offline

import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    @Test fun backupRoundTripPreservesVocabularyAndSrsProgress() {
        val word = WordEntity(
            term = "allocate", ipa = "/ˈæləkeɪt/", partOfSpeech = "verb",
            meaning = "phân bổ", example = "Allocate funds, carefully.",
            topic = "Economy", srsLevel = 3, nextReviewAtMillis = 123456789L,
            reviewCount = 7, lapseCount = 1, createdAtMillis = 111L, lastReviewedAtMillis = 222L
        )
        val restored = BackupCodec.restore(BackupCodec.create(listOf(word)))
        assertNull(restored.error)
        val copy = restored.words.single()
        assertEquals(word.term, copy.term)
        assertEquals(word.example, copy.example)
        assertEquals(word.srsLevel, copy.srsLevel)
        assertEquals(word.nextReviewAtMillis, copy.nextReviewAtMillis)
        assertEquals(word.reviewCount, copy.reviewCount)
        assertEquals(word.lapseCount, copy.lapseCount)
        assertEquals(word.createdAtMillis, copy.createdAtMillis)
        assertEquals(word.lastReviewedAtMillis, copy.lastReviewedAtMillis)
    }

    @Test fun rejectsNonZipAndUnknownBackupFormat() {
        assertNotNull(BackupCodec.restore("not a zip".toByteArray()).error)
    }
}

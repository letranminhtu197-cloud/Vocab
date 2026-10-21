package com.vocab.ielts.offline

import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    @Test fun zipBackupRoundTripPreservesSrsAndTextFields() {
        val original = WordEntity(
            term = "allocate", ipa = "/ˈæləkeɪt/", partOfSpeech = "verb",
            meaning = "phân bổ", example = "Allocate funds, carefully.",
            topic = "Economy", srsLevel = 3, nextReviewAtMillis = 123456789L,
            reviewCount = 7, lapseCount = 1, createdAtMillis = 111L, lastReviewedAtMillis = 222L
        )
        val restored = BackupCodec.restore(BackupCodec.create(listOf(original)))
        assertNull(restored.error)
        val copy = restored.words.single()
        assertEquals(original.term, copy.term)
        assertEquals(original.example, copy.example)
        assertEquals(original.srsLevel, copy.srsLevel)
        assertEquals(original.nextReviewAtMillis, copy.nextReviewAtMillis)
        assertEquals(original.reviewCount, copy.reviewCount)
        assertEquals(original.lapseCount, copy.lapseCount)
        assertEquals(original.createdAtMillis, copy.createdAtMillis)
        assertEquals(original.lastReviewedAtMillis, copy.lastReviewedAtMillis)
    }

    @Test fun rejectsInvalidArchive() {
        assertNotNull(BackupCodec.restore("not a zip".toByteArray()).error)
    }
}

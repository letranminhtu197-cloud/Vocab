package com.vocab.ielts.offline

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordStatusTest {
    private val now = 1_000_000L

    @Test fun brandNewWordIsDueAndNewButNotWeak() {
        val word = WordEntity(term = "mitigate", meaning = "giảm nhẹ")

        assertTrue(WordStatus.isDue(word, now))
        assertTrue(WordStatus.isNew(word))
        assertFalse(WordStatus.isWeak(word))
    }

    @Test fun scheduledWordIsDueOnlyAfterItsTimestamp() {
        val word = WordEntity(term = "allocate", meaning = "phân bổ", nextReviewAtMillis = now + 60_000L, reviewCount = 3, srsLevel = 2)

        assertFalse(WordStatus.isDue(word, now))
        assertTrue(WordStatus.isDue(word, now + 60_000L))
    }

    @Test fun wordWithFewSuccessfulReviewsIsWeak() {
        val word = WordEntity(term = "deteriorate", meaning = "xấu đi", reviewCount = 2, srsLevel = 1)

        assertTrue(WordStatus.isWeak(word))
    }

    @Test fun lapsedWordStaysWeakEvenAtHighLevel() {
        val word = WordEntity(term = "abundant", meaning = "dồi dào", reviewCount = 9, srsLevel = 4, lapseCount = 1)

        assertTrue(WordStatus.isWeak(word))
    }

    @Test fun masteredWordIsNotWeak() {
        val word = WordEntity(term = "sustainable", meaning = "bền vững", reviewCount = 6, srsLevel = 3)

        assertFalse(WordStatus.isWeak(word))
    }

    @Test fun countsMatchIndividualChecks() {
        val words = listOf(
            WordEntity(term = "a", meaning = "x"),
            WordEntity(term = "b", meaning = "y", nextReviewAtMillis = now + 10, reviewCount = 2, srsLevel = 2),
            WordEntity(term = "c", meaning = "z", nextReviewAtMillis = now + 10, reviewCount = 1, srsLevel = 1)
        )

        assertTrue(WordStatus.dueCount(words, now) == 1)
        assertTrue(WordStatus.weakCount(words) == 1)
    }
}

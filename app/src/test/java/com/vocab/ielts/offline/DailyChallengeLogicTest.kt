package com.vocab.ielts.offline

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyChallengeLogicTest {
    private val now = 1_800_000_000_000L
    private val date = LocalDate.of(2026, 10, 23)

    private fun words(count: Int): List<WordEntity> = (1..count).map { index ->
        WordEntity(
            id = index.toLong(),
            term = "word$index",
            meaning = "nghĩa $index",
            nextReviewAtMillis = if (index % 2 == 0) now - 1 else now + 86_400_000L,
            reviewCount = if (index % 3 == 0) 0 else 2,
            srsLevel = if (index % 3 == 0) 0 else 3
        )
    }

    @Test fun challengePicksAtMostFiveDistinctWordsFromTheNotebook() {
        val picked = DailyChallengeLogic.selectWords(words(12), date, now)

        assertTrue(picked.size <= DailyChallengeLogic.SIZE)
        assertEquals(picked.size, picked.map { it.id }.distinct().size)
        assertTrue(picked.all { it.id in words(12).map { w -> w.id } })
    }

    @Test fun sameDayAlwaysProducesTheSameChallenge() {
        val first = DailyChallengeLogic.selectWords(words(12), date, now).map { it.id }
        val second = DailyChallengeLogic.selectWords(words(12).reversed(), date, now).map { it.id }

        assertEquals(first, second)
    }

    @Test fun differentDaysProduceDifferentChallengeSets() {
        val today = DailyChallengeLogic.selectWords(words(12), date, now).map { it.id }.toSet()
        val tomorrow = DailyChallengeLogic.selectWords(words(12), date.plusDays(1), now).map { it.id }.toSet()

        assertNotEquals(today, tomorrow)
    }

    @Test fun challengeIsSafeWithAnEmptyNotebook() {
        assertTrue(DailyChallengeLogic.selectWords(emptyList(), date, now).isEmpty())
    }

    @Test fun challengePrefersCardsThatNeedPractice() {
        val picked = DailyChallengeLogic.selectWords(words(20), date, now)

        assertTrue(picked.isNotEmpty())
        // Nửa pool ưu tiên gồm từ đến hạn/chưa ôn, nên bộ chọn phải nằm trong nhóm đó.
        val candidates = words(20)
            .sortedWith(compareByDescending<WordEntity> { StudyQueueLogic.priorityScore(it, now) })
            .take(DailyChallengeLogic.SIZE * 2)
            .map { it.id }
            .toSet()
        assertTrue(picked.all { it.id in candidates })
    }

    @Test fun pausedWordsAreNeverChosenForTheChallenge() {
        val words = (1..8).map {
            WordEntity(id = it.toLong(), term = "w$it", meaning = "m$it", paused = it <= 4)
        }

        val picked = DailyChallengeLogic.selectWords(words, date, now)

        assertTrue(picked.isNotEmpty())
        assertTrue(picked.none { it.paused })
    }

    @Test fun completionKeyIsScopedToTheDate() {
        assertEquals("daily_challenge_done_2026-10-23", DailyChallengeLogic.isCompletedKey(date))
    }
}

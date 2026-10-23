package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyQueueLogicTest {
    private val now = 1_800_000_000_000L

    private fun word(
        term: String,
        nextReviewAtMillis: Long,
        reviewCount: Int = 1,
        srsLevel: Int = 2,
        lapseCount: Int = 0
    ) = WordEntity(
        term = term,
        meaning = "meaning",
        nextReviewAtMillis = nextReviewAtMillis,
        reviewCount = reviewCount,
        srsLevel = srsLevel,
        lapseCount = lapseCount
    )

    @Test fun dueAndNewCardsRankAheadOfMasteredFutureCards() {
        val mastered = word("mastered", now + 86_400_000L, reviewCount = 8, srsLevel = 5)
        val due = word("due", now - 1L, reviewCount = 3, srsLevel = 2)
        val new = word("new", 0L, reviewCount = 0, srsLevel = 0)

        val result = StudyQueueLogic.prioritize(listOf(mastered, due, new), now)

        assertEquals(listOf("new", "due", "mastered"), result.map { it.term })
    }

    @Test fun repeatedlyForgottenCardsReceiveHigherPriority() {
        val weak = word("weak", now + 86_400_000L, reviewCount = 3, srsLevel = 1, lapseCount = 2)
        val stable = word("stable", now + 86_400_000L, reviewCount = 5, srsLevel = 3)

        assertTrue(
            StudyQueueLogic.priorityScore(weak, now) >
                StudyQueueLogic.priorityScore(stable, now)
        )
    }

    @Test fun pausedWordsAreExcludedFromTheLessonDeck() {
        val paused = WordEntity(term = "paused", meaning = "m", paused = true)
        val active = WordEntity(term = "active", meaning = "a")

        assertEquals(
            listOf("active"),
            StudyQueueLogic.prioritize(listOf(paused, active), now).map { it.term }
        )
    }

    @Test fun equalPriorityCardsUseStableAlphabeticalTieBreak() {
        val result = StudyQueueLogic.prioritize(
            listOf(word("zebra", 0L, reviewCount = 0), word("apple", 0L, reviewCount = 0)),
            now
        )

        assertEquals(listOf("apple", "zebra"), result.map { it.term })
    }

    @Test fun priorityReasonExplainsEachQueueGroup() {
        assertEquals("Từ mới", StudyQueueLogic.priorityReason(word("new", 0L, reviewCount = 0), now))
        assertEquals("Cần củng cố", StudyQueueLogic.priorityReason(word("lapsed", now - 1L, lapseCount = 2), now))
        assertEquals("Đến hạn ôn", StudyQueueLogic.priorityReason(word("due", now - 1L), now))
        assertEquals("Từ yếu — ôn thêm", StudyQueueLogic.priorityReason(word("weak", now + 1L, lapseCount = 1), now))
        assertEquals("Ôn duy trì", StudyQueueLogic.priorityReason(word("stable", now + 1L), now))
    }
}

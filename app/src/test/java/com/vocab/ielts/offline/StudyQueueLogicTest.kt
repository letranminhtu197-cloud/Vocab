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

    @Test fun longerOverdueCardsRankAheadOfRecentlyDueCards() {
        val recentlyDue = word("recent", now - 1L, reviewCount = 3, srsLevel = 2)
        val longOverdue = word("overdue", now - 20L * 86_400_000L, reviewCount = 3, srsLevel = 2)

        assertTrue(
            StudyQueueLogic.priorityScore(longOverdue, now) >
                StudyQueueLogic.priorityScore(recentlyDue, now)
        )
    }

    @Test fun overdueUrgencyIsCappedForFairness() {
        val veryOverdue = word("old", now - 365L * 86_400_000L, reviewCount = 3, srsLevel = 2)
        val cappedOverdue = word("capped", now - 30L * 86_400_000L, reviewCount = 3, srsLevel = 2)

        assertEquals(
            StudyQueueLogic.priorityScore(cappedOverdue, now),
            StudyQueueLogic.priorityScore(veryOverdue, now)
        )
    }

    @Test fun priorityReasonExplainsWhyNewCardsComeFirst() {
        val new = word("new", 0L, reviewCount = 0, srsLevel = 0)

        assertTrue(StudyQueueLogic.priorityReason(new, now).contains("Từ mới"))
    }

    @Test fun equalPriorityCardsUseStableAlphabeticalTieBreak() {
        val result = StudyQueueLogic.prioritize(
            listOf(word("zebra", 0L, reviewCount = 0), word("apple", 0L, reviewCount = 0)),
            now
        )

        assertEquals(listOf("apple", "zebra"), result.map { it.term })
    }
}

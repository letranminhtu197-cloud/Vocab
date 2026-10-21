package com.vocab.ielts.offline

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.Duration

class SrsSchedulerTest {
    private val now = Instant.parse("2026-10-21T10:00:00Z")
    @Test fun newWordGoodStartsLevelOneAndSchedulesOneDay() {
        val result = SrsScheduler.review(ReviewState(), ReviewRating.GOOD, now)
        assertEquals(1, result.level)
        assertEquals(now.plus(Duration.ofDays(1)), result.nextReviewAt)
    }
    @Test fun againSchedulesTenMinutesAndCountsLapse() {
        val result = SrsScheduler.review(ReviewState(level=3), ReviewRating.AGAIN, now)
        assertEquals(1, result.level)
        assertEquals(now.plus(Duration.ofMinutes(10)), result.nextReviewAt)
        assertEquals(1, result.lapseCount)
    }
    @Test fun easyAdvancesTwoLevelsAndCapsAtFive() {
        assertEquals(2, SrsScheduler.review(ReviewState(), ReviewRating.EASY, now).level)
        assertEquals(5, SrsScheduler.review(ReviewState(level=4), ReviewRating.EASY, now).level)
    }
    @Test fun dueAndGoldenWindowAreBasedOnSavedTimestamp() {
        val state = ReviewState(level=1, nextReviewAt=now)
        assertTrue(SrsScheduler.isDue(state, now))
        assertEquals("GOLDEN", SrsScheduler.goldenWindow(state, now))
        assertEquals("OVERDUE", SrsScheduler.goldenWindow(state, now.plus(Duration.ofHours(13))))
    }
}

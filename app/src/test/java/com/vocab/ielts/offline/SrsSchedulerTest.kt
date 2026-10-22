package com.vocab.ielts.offline

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.Duration

class SrsSchedulerTest {
    private val now = Instant.parse("2026-10-21T10:00:00Z")
    @Test fun newWordGoodStartsLevelOneAndSchedulesFourHours() {
        val result = SrsScheduler.review(ReviewState(), ReviewRating.GOOD, now)
        assertEquals(1, result.level)
        assertEquals(now.plus(Duration.ofHours(4)), result.nextReviewAt)
    }
    @Test fun againSchedulesTenMinutesAndCountsLapse() {
        val result = SrsScheduler.review(ReviewState(level=3), ReviewRating.AGAIN, now)
        assertEquals(0, result.level)
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

    @Test fun goldenWindowStartsTwoHoursBeforeDueAndEndsTwelveHoursAfter() {
        val due = now.plus(Duration.ofDays(1))
        val state = ReviewState(level = 1, nextReviewAt = due)
        assertEquals("UPCOMING", SrsScheduler.goldenWindow(state, due.minus(Duration.ofHours(2)).minusSeconds(1)))
        assertEquals("GOLDEN", SrsScheduler.goldenWindow(state, due.minus(Duration.ofHours(2))))
        assertEquals("GOLDEN", SrsScheduler.goldenWindow(state, due))
        assertEquals("GOLDEN", SrsScheduler.goldenWindow(state, due.plus(Duration.ofHours(11)).plus(Duration.ofMinutes(59))))
        assertEquals("OVERDUE", SrsScheduler.goldenWindow(state, due.plus(Duration.ofHours(12))))
    }

    @Test fun goodRatingFollowsTheFourHourToThirtyDayLadder() {
        val levels = listOf(0, 1, 2, 3, 4, 5)
        val expectedIntervals = listOf(
            Duration.ofHours(4), Duration.ofHours(24), Duration.ofHours(72),
            Duration.ofHours(168), Duration.ofHours(720), Duration.ofHours(720)
        )
        levels.zip(expectedIntervals).forEach { (level, interval) ->
            val result = SrsScheduler.review(ReviewState(level = level), ReviewRating.GOOD, now)
            assertEquals("level=$level", now.plus(interval), result.nextReviewAt)
        }
    }

    @Test fun successfulAndFailedReviewsUpdateCountsConsistently() {
        val first = SrsScheduler.review(ReviewState(), ReviewRating.GOOD, now)
        assertEquals(1, first.reviewCount)
        assertEquals(0, first.lapseCount)
        val failed = SrsScheduler.review(first, ReviewRating.AGAIN, now.plus(Duration.ofDays(1)))
        assertEquals(2, failed.reviewCount)
        assertEquals(1, failed.lapseCount)
        assertEquals(0, failed.level)
        assertEquals(now.plus(Duration.ofDays(1)).plus(Duration.ofMinutes(10)), failed.nextReviewAt)
        val recovered = SrsScheduler.review(failed, ReviewRating.GOOD, failed.nextReviewAt!!)
        assertEquals(1, recovered.level)
        assertEquals(failed.nextReviewAt!!.plus(Duration.ofHours(4)), recovered.nextReviewAt)
    }

    @Test fun ratingLabelsMatchTheIntervalsUsedByTheScheduler() {
        assertEquals("10 phút", SrsScheduler.intervalLabel(0, ReviewRating.AGAIN))
        assertEquals("2 giờ", SrsScheduler.intervalLabel(0, ReviewRating.HARD))
        assertEquals("4 giờ", SrsScheduler.intervalLabel(0, ReviewRating.GOOD))
        assertEquals("1 ngày", SrsScheduler.intervalLabel(0, ReviewRating.EASY))
        assertEquals("12 giờ", SrsScheduler.intervalLabel(2, ReviewRating.HARD))
    }
}

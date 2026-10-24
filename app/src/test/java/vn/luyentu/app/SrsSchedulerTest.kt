package vn.luyentu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsSchedulerTest {
    private val now = 1_800_000_000_000L
    private val fresh = VocabularyWord(
        id = 1, term = "resilient", meaning = "kiên cường", example = "", ipa = "", topic = ""
    )

    @Test
    fun againSchedulesTenMinutesAndCountsLapse() {
        val result = SrsScheduler.calculate(fresh.copy(easeFactor = 1.4), 0, now)
        assertEquals(now + 10 * 60 * 1000L, result.nextReviewAt)
        assertEquals(0, result.repetitions)
        assertEquals(0, result.intervalDays)
        assertEquals(1, result.lapses)
        assertEquals(1.3, result.easeFactor, 0.0001)
    }

    @Test
    fun hardOnNewWordSchedulesTwelveHours() {
        val result = SrsScheduler.calculate(fresh, 1, now)
        assertEquals(now + 12 * 60 * 60 * 1000L, result.nextReviewAt)
        assertEquals(1, result.intervalDays)
        assertEquals(0, result.repetitions)
        assertEquals(2.35, result.easeFactor, 0.0001)
    }

    @Test
    fun goodOnNewWordSchedulesOneDay() {
        val result = SrsScheduler.calculate(fresh, 2, now)
        assertEquals(now + 24 * 60 * 60 * 1000L, result.nextReviewAt)
        assertEquals(1, result.intervalDays)
        assertEquals(1, result.repetitions)
        assertEquals(2.5, result.easeFactor, 0.0001)
    }

    @Test
    fun easyOnNewWordRaisesEaseAndUsesFourDayInterval() {
        val result = SrsScheduler.calculate(fresh, 3, now)
        assertEquals(now + 4 * 24 * 60 * 60 * 1000L, result.nextReviewAt)
        assertEquals(4, result.intervalDays)
        assertEquals(1, result.repetitions)
        assertEquals(2.65, result.easeFactor, 0.0001)
    }

    @Test
    fun goodOnMatureWordMultipliesCurrentIntervalByEase() {
        val mature = fresh.copy(repetitions = 2, intervalDays = 3, easeFactor = 2.5)
        val result = SrsScheduler.calculate(mature, 2, now)
        assertEquals(7, result.intervalDays)
        assertEquals(now + 7 * 24 * 60 * 60 * 1000L, result.nextReviewAt)
        assertEquals(3, result.repetitions)
        val levelAfter = VocabularyWord(
            id = fresh.id, term = fresh.term, meaning = fresh.meaning, example = fresh.example,
            ipa = fresh.ipa, topic = fresh.topic, intervalDays = result.intervalDays,
            repetitions = result.repetitions
        ).learningLevel
        assertEquals(3, levelAfter)
    }

    @Test
    fun easyClampsEaseFactorAndGradeIsClamped() {
        val mature = fresh.copy(repetitions = 3, intervalDays = 7, easeFactor = 2.99, lapses = 2)
        val result = SrsScheduler.calculate(mature, 99, now)
        assertEquals(3, result.grade)
        assertEquals(3.0, result.easeFactor, 0.0001)
        assertEquals(2, result.lapses)
        assertTrue(result.nextReviewAt > now)
    }

}

package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementLogicTest {
    @Test fun emptyLibraryStartsWithAllAchievementsLocked() {
        val result = AchievementLogic.evaluate(
            totalWords = 0,
            totalPracticeEvents = 0,
            activeStreak = 0,
            todayActivities = 0,
            dailyGoal = 10
        )
        assertEquals(6, result.size)
        assertTrue(result.all { !it.unlocked })
        assertEquals(0f, result.first { it.id == "first_word" }.fraction, 0f)
    }

    @Test fun achievementsUnlockAtTheirThresholds() {
        val result = AchievementLogic.evaluate(
            totalWords = 50,
            totalPracticeEvents = 100,
            activeStreak = 7,
            todayActivities = 10,
            dailyGoal = 10
        )
        assertTrue(result.all { it.unlocked })
        assertEquals(1f, result.first { it.id == "word_collector" }.fraction, 0f)
    }

    @Test fun progressIsCappedAndNegativeCountersAreSafe() {
        val result = AchievementLogic.evaluate(
            totalWords = 100,
            totalPracticeEvents = -2,
            activeStreak = -1,
            todayActivities = 20,
            dailyGoal = 10
        )
        assertEquals(1f, result.first { it.id == "word_collector" }.fraction, 0f)
        assertEquals(0f, result.first { it.id == "first_review" }.fraction, 0f)
        assertFalse(result.first { it.id == "first_review" }.unlocked)
        assertEquals(10, result.last().target)
    }
}

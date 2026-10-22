package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Test

class StudyGoalLogicTest {
    @Test
    fun normalizesUnsupportedGoalsToNearestSupportedGoal() {
        assertEquals(10, StudyGoalLogic.normalizeGoal(9))
        assertEquals(20, StudyGoalLogic.normalizeGoal(22))
        assertEquals(5, StudyGoalLogic.normalizeGoal(0))
    }

    @Test
    fun progressIsBoundedBetweenZeroAndOne() {
        assertEquals(0f, StudyGoalLogic.progress(-2, 10), 0.0001f)
        assertEquals(0.5f, StudyGoalLogic.progress(5, 10), 0.0001f)
        assertEquals(1f, StudyGoalLogic.progress(18, 10), 0.0001f)
    }

    @Test
    fun remainingNeverBecomesNegative() {
        assertEquals(4, StudyGoalLogic.remaining(6, 10))
        assertEquals(0, StudyGoalLogic.remaining(12, 10))
    }
}

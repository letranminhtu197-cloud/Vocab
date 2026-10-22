package com.vocab.ielts.offline

/** Quy tắc thuần Kotlin cho mục tiêu ngày; không phụ thuộc Android và dễ kiểm thử. */
object StudyGoalLogic {
    val supportedGoals: List<Int> = listOf(5, 10, 15, 20, 25, 30)

    fun normalizeGoal(goal: Int): Int =
        supportedGoals.minByOrNull { kotlin.math.abs(it - goal) } ?: 10

    fun progress(activities: Int, goal: Int): Float {
        val safeGoal = normalizeGoal(goal).coerceAtLeast(1)
        return (activities.coerceAtLeast(0).toFloat() / safeGoal).coerceIn(0f, 1f)
    }

    fun remaining(activities: Int, goal: Int): Int =
        (normalizeGoal(goal) - activities.coerceAtLeast(0)).coerceAtLeast(0)
}

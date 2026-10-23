package com.vocab.ielts.offline

import java.time.Duration
import java.time.Instant

enum class ReviewRating { AGAIN, HARD, GOOD, EASY }
data class ReviewState(
    val level: Int = 0,
    val nextReviewAt: Instant? = null,
    val reviewCount: Int = 0,
    val lapseCount: Int = 0,
    val consecutiveSuccessfulReviews: Int = 0
)
object SrsScheduler {
    // Original five-level review ladder: 1, 3, 7, 14, and 30 days.
    // Existing scheduled timestamps are preserved; these intervals apply to new ratings.
    private val intervals = listOf(
        Duration.ofDays(1),
        Duration.ofDays(3),
        Duration.ofDays(7),
        Duration.ofDays(14),
        Duration.ofDays(30)
    )

    private fun nextLevel(level: Int, rating: ReviewRating): Int = when (rating) {
        ReviewRating.AGAIN -> 0
        ReviewRating.HARD -> if (level == 0) 1 else level.coerceIn(1, 5)
        ReviewRating.GOOD -> if (level == 0) 1 else (level + 1).coerceAtMost(5)
        ReviewRating.EASY -> if (level == 0) 2 else (level + 2).coerceAtMost(5)
    }

    private fun delayFor(level: Int, rating: ReviewRating): Duration {
        val targetLevel = nextLevel(level, rating)
        return when (rating) {
            ReviewRating.AGAIN -> Duration.ofMinutes(10)
            ReviewRating.HARD -> maxOf(
                intervals[(targetLevel - 1).coerceIn(0, 4)].dividedBy(2),
                Duration.ofHours(1)
            )
            ReviewRating.GOOD, ReviewRating.EASY ->
                intervals[(targetLevel - 1).coerceIn(0, 4)]
        }
    }

    /** Preview the exact interval the learner will schedule for a rating. */
    fun intervalLabel(level: Int, rating: ReviewRating): String {
        val minutes = delayFor(level, rating).toMinutes()
        if (minutes < 60) return "$minutes phút"
        val hours = minutes / 60
        if (hours < 24) return "$hours giờ"
        val days = hours / 24
        val remainingHours = hours % 24
        return if (remainingHours == 0L) "$days ngày" else "$days ngày $remainingHours giờ"
    }

    fun review(state: ReviewState, rating: ReviewRating, now: Instant): ReviewState {
        val nextLevel = nextLevel(state.level, rating)
        val delay = delayFor(state.level, rating)
        return ReviewState(
            level = nextLevel,
            nextReviewAt = now.plus(delay),
            reviewCount = state.reviewCount + 1,
            lapseCount = state.lapseCount + if (rating == ReviewRating.AGAIN) 1 else 0,
            consecutiveSuccessfulReviews = if (rating == ReviewRating.AGAIN) 0 else state.consecutiveSuccessfulReviews + 1
        )
    }

    fun isDue(state: ReviewState, now: Instant): Boolean =
        state.nextReviewAt?.let { !it.isAfter(now) } ?: true

    fun goldenWindow(state: ReviewState, now: Instant): String {
        val due = state.nextReviewAt ?: return "NEW"
        return when {
            now.isBefore(due.minus(Duration.ofHours(2))) -> "UPCOMING"
            now.isBefore(due.plus(Duration.ofHours(12))) -> "GOLDEN"
            else -> "OVERDUE"
        }
    }
}

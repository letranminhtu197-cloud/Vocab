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
    private val intervals = listOf(1L, 3L, 7L, 14L, 30L)
    fun review(state: ReviewState, rating: ReviewRating, now: Instant): ReviewState {
        val nextLevel = when (rating) {
            ReviewRating.AGAIN -> 0
            ReviewRating.HARD -> if (state.level == 0) 1 else state.level.coerceIn(1, 5)
            ReviewRating.GOOD -> if (state.level == 0) 1 else (state.level + 1).coerceAtMost(5)
            ReviewRating.EASY -> if (state.level == 0) 2 else (state.level + 2).coerceAtMost(5)
        }
        val delay = when (rating) {
            ReviewRating.AGAIN -> Duration.ofMinutes(10)
            ReviewRating.HARD -> Duration.ofHours((intervals[(nextLevel - 1).coerceIn(0, 4)] * 24 / 2).coerceAtLeast(12))
            ReviewRating.GOOD, ReviewRating.EASY -> Duration.ofDays(intervals[(nextLevel - 1).coerceIn(0, 4)])
        }
        return ReviewState(
            level = nextLevel,
            nextReviewAt = now.plus(delay),
            reviewCount = state.reviewCount + 1,
            lapseCount = state.lapseCount + if (rating == ReviewRating.AGAIN) 1 else 0,
            consecutiveSuccessfulReviews = if (rating == ReviewRating.AGAIN) 0 else state.consecutiveSuccessfulReviews + 1
        )
    }
    fun isDue(state: ReviewState, now: Instant): Boolean = state.nextReviewAt?.let { !it.isAfter(now) } ?: true
    fun goldenWindow(state: ReviewState, now: Instant): String {
        val due = state.nextReviewAt ?: return "NEW"
        return when {
            now.isBefore(due.minus(Duration.ofHours(2))) -> "UPCOMING"
            !now.isBefore(due) && now.isBefore(due.plus(Duration.ofHours(12))) -> "GOLDEN"
            now.isBefore(due) -> "UPCOMING"
            else -> "OVERDUE"
        }
    }
}

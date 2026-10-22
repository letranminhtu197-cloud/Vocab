package com.vocab.ielts.offline

import java.util.concurrent.TimeUnit

/**
 * Adaptive ordering for retrieval practice.
 *
 * Due and new cards stay ahead of comfortable future cards. Within due cards,
 * older overdue items gain urgency; repeated lapses and low SRS maturity add
 * weight without allowing a single statistic to grow without limit.
 */
object StudyQueueLogic {
    fun priorityScore(word: WordEntity, nowMillis: Long): Int {
        val isDue = word.nextReviewAtMillis <= 0L || word.nextReviewAtMillis <= nowMillis
        val isNew = word.reviewCount == 0 || word.srsLevel == 0
        val lapseCount = word.lapseCount.coerceAtLeast(0)
        val overdueDays = if (word.nextReviewAtMillis > 0L && word.nextReviewAtMillis < nowMillis) {
            TimeUnit.MILLISECONDS.toDays(nowMillis - word.nextReviewAtMillis).coerceAtMost(30).toInt()
        } else 0

        return (if (isDue) 100 else 0) +
            (if (isNew) 55 else 0) +
            (overdueDays * 3).coerceAtMost(60) +
            (lapseCount * 20).coerceAtMost(60) +
            (if (word.reviewCount > 0 && word.srsLevel <= 1) 15 else 0)
    }

    /** Human-readable reason helps learners understand why a card is prioritized. */
    fun priorityReason(word: WordEntity, nowMillis: Long): String = when {
        word.reviewCount == 0 || word.srsLevel == 0 -> "Từ mới — cần chủ động nhớ lại"
        word.lapseCount > 0 -> "Từ từng quên " + word.lapseCount + " lần — nên củng cố"
        word.nextReviewAtMillis > 0L && word.nextReviewAtMillis < nowMillis -> "Đã quá hạn — ôn lại để củng cố trí nhớ"
        word.nextReviewAtMillis <= 0L || word.nextReviewAtMillis <= nowMillis -> "Đến thời điểm ôn tập"
        word.srsLevel <= 1 -> "Mức ghi nhớ còn thấp — cần luyện thêm"
        else -> "Luyện truy hồi để kiểm tra khả năng ghi nhớ"
    }

    fun prioritize(words: List<WordEntity>, nowMillis: Long): List<WordEntity> =
        words.sortedWith(
            compareByDescending<WordEntity> { priorityScore(it, nowMillis) }
                .thenBy { if (it.nextReviewAtMillis <= 0L) Long.MIN_VALUE else it.nextReviewAtMillis }
                .thenBy { it.term.lowercase() }
        )
}

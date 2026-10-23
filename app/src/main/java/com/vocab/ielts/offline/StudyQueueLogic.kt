package com.vocab.ielts.offline

/**
 * Deterministic priority ordering for a mixed study session.
 *
 * The queue favors cards that most need retrieval practice: overdue/new cards first,
 * then repeatedly forgotten cards and cards with little successful review history.
 * The caller should freeze the resulting IDs for a session so SRS updates don't
 * reshuffle the learner's current question underneath them.
 */
object StudyQueueLogic {
    fun priorityScore(word: WordEntity, nowMillis: Long): Int {
        val isDue = word.nextReviewAtMillis <= 0L || word.nextReviewAtMillis <= nowMillis
        val isNew = word.reviewCount == 0 || word.srsLevel == 0
        val isWeak = word.lapseCount > 0

        return (if (isDue) 100 else 0) +
            (if (isNew) 55 else 0) +
            (if (isWeak) 35 else 0) +
            (word.lapseCount.coerceAtLeast(0) * 20) +
            (if (word.reviewCount > 0 && word.srsLevel <= 1) 15 else 0)
    }

    fun prioritize(words: List<WordEntity>, nowMillis: Long): List<WordEntity> =
        words.filter { !it.paused }
            .sortedWith(
            compareByDescending<WordEntity> { priorityScore(it, nowMillis) }
                .thenBy { if (it.nextReviewAtMillis <= 0L) Long.MIN_VALUE else it.nextReviewAtMillis }
                .thenBy { it.term.lowercase() }
        )

    /**
     * Nhãn ngắn giải thích vì sao từ được xếp vào phiên học. Hiển thị ngay trên
     * thẻ để người học hiểu thứ tự ưu tiên thay vì thấy danh sách có vẻ ngẫu nhiên.
     */
    fun priorityReason(word: WordEntity, nowMillis: Long): String {
        val isDue = word.nextReviewAtMillis <= 0L || word.nextReviewAtMillis <= nowMillis
        return when {
            word.reviewCount == 0 -> "Từ mới"
            word.lapseCount > 0 && isDue -> "Cần củng cố"
            word.lapseCount > 0 -> "Từ yếu — ôn thêm"
            isDue -> "Đến hạn ôn"
            else -> "Ôn duy trì"
        }
    }
}

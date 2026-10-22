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
        words.sortedWith(
            compareByDescending<WordEntity> { priorityScore(it, nowMillis) }
                .thenBy { if (it.nextReviewAtMillis <= 0L) Long.MIN_VALUE else it.nextReviewAtMillis }
                .thenBy { it.term.lowercase() }
        )
}

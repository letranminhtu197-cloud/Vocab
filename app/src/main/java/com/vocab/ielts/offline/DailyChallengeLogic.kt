package com.vocab.ielts.offline

import java.time.LocalDate

/**
 * Thử thách từ vựng hằng ngày: chọn một nhóm từ cố định trong ngày từ chính sổ
 * từ của người học. Cùng một ngày luôn ra cùng bộ từ để người học có thể làm tiếp
 * sau khi đóng ứng dụng; sang ngày mới bộ từ đổi.
 */
object DailyChallengeLogic {
    const val SIZE = 5

    fun selectWords(
        words: List<WordEntity>,
        date: LocalDate,
        nowMillis: Long,
        size: Int = SIZE
    ): List<WordEntity> {
        if (words.isEmpty() || size <= 0) return emptyList()
        val reviewable = words.filter { !it.paused }
        if (reviewable.isEmpty()) return emptyList()
        val pool = reviewable
            .sortedWith(
                compareByDescending<WordEntity> { StudyQueueLogic.priorityScore(it, nowMillis) }
                    .thenBy { it.term.lowercase() }
                    .thenBy { it.id }
            )
            .take((size * 2).coerceAtMost(words.size))
        val seed = date.toEpochDay()
        return pool
            .shuffled(java.util.Random(seed))
            .take(size)
            .sortedWith(
                compareByDescending<WordEntity> { StudyQueueLogic.priorityScore(it, nowMillis) }
                    .thenBy { it.id }
            )
    }

    fun isCompletedKey(date: LocalDate): String = "daily_challenge_done_" + date.toString()
}

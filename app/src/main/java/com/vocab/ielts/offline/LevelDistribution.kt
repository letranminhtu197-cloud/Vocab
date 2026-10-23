package com.vocab.ielts.offline

/**
 * Phân bố từ vựng theo 5 cấp ghi nhớ (thang Leitner mà MochiVocab cũng dùng: cấp càng cao
 * thì khoảng cách ôn càng xa). Hàm thuần Kotlin để kiểm thử, không phụ thuộc Android.
 */
object LevelDistribution {
    const val MAX_LEVEL = 5

    fun counts(words: List<WordEntity>): List<Int> =
        List(MAX_LEVEL + 1) { level ->
            words.count { it.srsLevel.coerceIn(0, MAX_LEVEL) == level }
        }

    /** Tỷ lệ từ đã lên cấp 4–5 (nhóm "đã nhớ tốt" theo cách MochiVocab mô tả). */
    fun masteredShare(words: List<WordEntity>): Float {
        if (words.isEmpty()) return 0f
        val counts = counts(words)
        return (counts[4] + counts[5]).toFloat() / words.size
    }

    /** Nhãn ngắn cho cột trong biểu đồ. */
    fun label(level: Int): String = when (level) {
        0 -> "Mới"
        else -> "C$level"
    }
}

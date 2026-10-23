package com.vocab.ielts.offline

/**
 * Một nguồn định nghĩa duy nhất cho các trạng thái từ vựng.
 *
 * Trước đây mỗi màn hình tự viết lại điều kiện "đến hạn"/"từ yếu" nên sổ từ,
 * quiz và lộ trình có thể lệch nhau (ví dụ sổ từ coi từ ít lượt ôn là yếu còn
 * quiz thì không). Mọi màn hình phải dùng các hàm dưới đây để số liệu khớp nhau.
 */
object WordStatus {
    /** Từ chưa từng được xếp lịch (0) hoặc đã tới hạn ôn. */
    fun isDue(word: WordEntity, nowMillis: Long): Boolean =
        word.nextReviewAtMillis <= 0L || word.nextReviewAtMillis <= nowMillis

    /** Từ chưa có lượt ôn nào. */
    fun isNew(word: WordEntity): Boolean = word.reviewCount == 0

    /**
     * Từ cần luyện lại: đã từng quên ít nhất một lần, hoặc đã ôn nhưng vẫn ở
     * cấp thấp (0–1) — nhóm này chưa ổn định trong trí nhớ dài hạn.
     */
    fun isWeak(word: WordEntity): Boolean =
        word.lapseCount > 0 || (word.reviewCount > 0 && word.srsLevel <= 1)

    fun dueCount(words: List<WordEntity>, nowMillis: Long): Int =
        words.count { isDue(it, nowMillis) }

    fun weakCount(words: List<WordEntity>): Int = words.count { isWeak(it) }
}

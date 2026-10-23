package com.vocab.ielts.offline

import androidx.room.withTransaction
import java.time.Instant

/** Chuyển bản ghi từ vựng sang trạng thái SRS thuần để tính lịch ôn. */
fun WordEntity.toReviewState(): ReviewState = ReviewState(
    level = srsLevel,
    nextReviewAt = nextReviewAtMillis.takeIf { it > 0L }?.let(Instant::ofEpochMilli),
    reviewCount = reviewCount,
    lapseCount = lapseCount
)

/**
 * Áp dụng một lượt đánh giá SRS lên dữ liệu mới nhất trong database.
 *
 * Trước đây mỗi màn hình tự tính từ bản sao [WordEntity] đang hiển thị rồi ghi
 * đè cả dòng. Nếu hai lượt trả lời tới gần nhau (chuyển câu nhanh, đổi chế độ),
 * lượt sau có thể ghi đè bằng số liệu cũ và làm mất một lượt ôn. Đọc lại bên
 * trong transaction giúp mọi lượt đánh giá cộng dồn chính xác.
 */
class ReviewStore(private val database: VocabDatabase) {
    private val dao = database.wordDao()

    suspend fun rate(wordId: Long, rating: ReviewRating, nowMillis: Long): WordEntity? =
        database.withTransaction {
            val current = dao.findById(wordId) ?: return@withTransaction null
            val next = SrsScheduler.review(current.toReviewState(), rating, Instant.ofEpochMilli(nowMillis))
            val updated = current.copy(
                srsLevel = next.level,
                nextReviewAtMillis = next.nextReviewAt?.toEpochMilli() ?: 0L,
                reviewCount = next.reviewCount,
                lapseCount = next.lapseCount,
                lastReviewedAtMillis = nowMillis
            )
            dao.update(updated)
            updated
        }

    /** Đánh giá sai/đúng đơn giản dùng cho các chế độ luyện tập. */
    suspend fun rateByCorrectness(wordId: Long, correct: Boolean, nowMillis: Long): WordEntity? =
        rate(wordId, if (correct) ReviewRating.GOOD else ReviewRating.AGAIN, nowMillis)
}

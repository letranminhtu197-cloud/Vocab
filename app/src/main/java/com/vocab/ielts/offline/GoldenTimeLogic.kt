package com.vocab.ielts.offline

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Tính "thời điểm vàng": lúc sớm nhất nên nhắc người học quay lại, dựa trên lịch ôn của
 * chính các từ trong sổ. Thuần Kotlin để kiểm thử; không phụ thuộc Android.
 */
object GoldenTimeLogic {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm dd/MM")

    /** Số từ đã đến hạn ngay lúc này (chưa từng xếp lịch hoặc đã quá hạn), bỏ qua từ tạm dừng. */
    fun dueNowCount(words: List<WordEntity>, nowMillis: Long): Int =
        words.count { !it.paused && (it.nextReviewAtMillis <= 0L || it.nextReviewAtMillis <= nowMillis) }

    /** Từ nên ôn sớm nhất: ưu tiên từ đến hạn, sau đó là từ có lịch gần nhất. */
    fun soonestWord(words: List<WordEntity>, nowMillis: Long): WordEntity? =
        words.filter { !it.paused }.minByOrNull { word ->
            if (word.nextReviewAtMillis <= 0L) nowMillis else word.nextReviewAtMillis
        }

    /**
     * Mốc thời gian nhắc kế tiếp. Bỏ qua các mốc đã qua hoặc quá gần (trong vòng 1 phút)
     * để không bắn thông báo ngay khi người dùng vừa học xong.
     */
    fun nextReminderAt(words: List<WordEntity>, nowMillis: Long): Long? {
        if (words.isEmpty()) return null
        val threshold = nowMillis + 60_000L
        return words.asSequence()
            .filter { !it.paused }
            .map { if (it.nextReviewAtMillis <= 0L) nowMillis else it.nextReviewAtMillis }
            .filter { it > threshold }
            .minOrNull()
    }

    /** Nhãn thời gian dễ đọc: "hôm nay 15:40", "mai 08:00", "25/10 09:30". */
    fun label(millis: Long, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val target = Instant.ofEpochMilli(millis).atZone(zone)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return when (target.toLocalDate()) {
            today -> "hôm nay " + timeFormatter.format(target)
            today.plusDays(1) -> "mai " + timeFormatter.format(target)
            else -> dateTimeFormatter.format(target)
        }
    }

    /** Nội dung thông báo khi đến giờ ôn. */
    fun notificationMessage(words: List<WordEntity>, nowMillis: Long): String {
        val due = dueNowCount(words, nowMillis)
        return when {
            due > 0 -> "Có $due từ đang đến hạn ôn. Vào Vocab để ôn ngay nhé!"
            words.isEmpty() -> "Sổ từ đang trống. Thêm từ mới để bắt đầu học."
            else -> "Đến giờ ôn từ vựng. Mở Vocab để giữ nhịp học hôm nay."
        }
    }
}

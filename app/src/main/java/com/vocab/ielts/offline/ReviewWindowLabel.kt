package com.vocab.ielts.offline

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.time.Instant

@Composable
fun ReviewWindowLabel(word: WordEntity) {
    val state = ReviewState(
        level = word.srsLevel,
        nextReviewAt = word.nextReviewAtMillis.takeIf { it > 0L }?.let(Instant::ofEpochMilli),
        reviewCount = word.reviewCount,
        lapseCount = word.lapseCount
    )
    val window = SrsScheduler.goldenWindow(state, Instant.now())
    val label = when (window) {
        "NEW" -> "Mới — chưa ôn"
        "UPCOMING" -> "Sắp đến hạn ôn"
        "GOLDEN" -> "Thời điểm vàng — nên ôn ngay"
        else -> "Đã quá hạn — nên ôn bù"
    }
    val color = when (window) {
        "GOLDEN" -> MaterialTheme.colorScheme.primary
        "OVERDUE" -> MaterialTheme.colorScheme.error
        "UPCOMING" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(label, style = MaterialTheme.typography.labelMedium, color = color)
}
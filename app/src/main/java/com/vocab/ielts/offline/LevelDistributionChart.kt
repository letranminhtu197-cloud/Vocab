package com.vocab.ielts.offline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Biểu đồ phân bố 5 cấp ghi nhớ (cộng nhóm "Mới"). Mục tiêu là nhìn thấy ngay bao nhiêu từ
 * đã lên cấp cao — đúng cách MochiVocab mô tả biểu đồ 5 cấp độ lý tưởng trong blog của họ.
 */
@Composable
fun LevelDistributionChart(words: List<WordEntity>, modifier: Modifier = Modifier) {
    val counts = remember(words) { LevelDistribution.counts(words) }
    val masteredShare = remember(words) { LevelDistribution.masteredShare(words) }
    val maxCount = counts.max().coerceAtLeast(1)

    VocabCard(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Phân bố cấp ghi nhớ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (words.isEmpty()) {
                    "Chưa có từ nào để thống kê. Thêm từ hoặc học khóa khởi đầu để xem biểu đồ."
                } else {
                    "Cấp càng cao thì khoảng cách ôn càng xa. Đã nhớ tốt (cấp 4–5): ${(masteredShare * 100).toInt()}%."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (words.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    counts.forEachIndexed { level, count ->
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("$count", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height((10 + 70 * count / maxCount).dp)
                                    .background(levelColor(level), RoundedCornerShape(8.dp))
                            )
                            Text(
                                LevelDistribution.label(level),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun levelColor(level: Int): Color = when (level) {
    0 -> MaterialTheme.colorScheme.surfaceVariant
    1 -> MaterialTheme.colorScheme.error
    2 -> MaterialTheme.colorScheme.secondary
    3 -> MaterialTheme.colorScheme.tertiary
    4 -> MaterialTheme.colorScheme.primaryContainer
    else -> MaterialTheme.colorScheme.primary
}

package com.vocab.ielts.offline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Hiển thị huy hiệu tự tính từ số liệu cục bộ, không thêm dữ liệu mặc định vào sổ từ. */
@Composable
fun AchievementSection(
    totalWords: Int,
    totalPracticeEvents: Int,
    activeStreak: Int,
    todayActivities: Int,
    dailyGoal: Int
) {
    val achievements = remember(totalWords, totalPracticeEvents, activeStreak, todayActivities, dailyGoal) {
        AchievementLogic.evaluate(totalWords, totalPracticeEvents, activeStreak, todayActivities, dailyGoal)
    }
    val unlockedCount = achievements.count { it.unlocked }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Thành tựu cá nhân", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("$unlockedCount/${achievements.size} mở khóa", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(
            "Thành tựu chỉ dựa trên dữ liệu học của bạn trên thiết bị này.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        achievements.forEach { achievement ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (achievement.unlocked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (achievement.unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                        contentDescription = if (achievement.unlocked) "Đã mở khóa" else "Chưa mở khóa",
                        tint = if (achievement.unlocked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(achievement.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(
                                "${achievement.displayedCurrent}/${achievement.target}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            achievement.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { achievement.fraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!achievement.unlocked) {
                            Text(
                                "Còn ${achievement.target - achievement.displayedCurrent} ${achievement.unit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text("Đã đạt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

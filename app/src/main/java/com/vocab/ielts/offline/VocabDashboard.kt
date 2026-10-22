package com.vocab.ielts.offline

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Trang tổng quan tập trung vào hành động học tiếp theo, tiến độ ôn tập và mục tiêu ngày. */
@Composable
fun DashboardOverview(
    totalWords: Int,
    dueWords: Int,
    learnedWords: Int,
    activeStreak: Int,
    todayActivities: Int,
    dailyGoal: Int,
    onGoalChange: (Int) -> Unit,
    onStartReview: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenLearningPath: () -> Unit,
    onOpenStats: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("GÓC HỌC TẬP CỦA BẠN", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(
                    if (totalWords == 0) "Bắt đầu từ bộ từ của bạn" else "Mỗi ngày tiến thêm một chút",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    if (totalWords == 0)
                        "Nhập CSV hoặc tự thêm từ để tạo bộ từ IELTS của riêng bạn. Vocab không tự nạp danh sách từ."
                    else
                        "Bạn có $totalWords mục từ riêng. Ưu tiên ôn từ đến hạn trước để giữ nhịp ghi nhớ.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardMetric("Đến hạn", dueWords.toString(), Modifier.weight(1f))
                    DashboardMetric("Đã ôn", learnedWords.toString(), Modifier.weight(1f))
                    DashboardMetric("Chuỗi ngày", "$activeStreak", Modifier.weight(1f))
                }
                Button(
                    onClick = if (dueWords > 0) onStartReview else onOpenPractice,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (dueWords > 0) Icons.Default.Refresh else Icons.Default.School, contentDescription = null)
                    Text(if (dueWords > 0) "Ôn tập ngay · $dueWords từ" else "Mở khu luyện tập")
                }
            }
        }

        DailyGoalCard(activities = todayActivities, goal = dailyGoal, onGoalChange = onGoalChange)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardAction("Sổ từ", "Quản lý từ", Icons.Default.MenuBook, Modifier.weight(1f), onOpenLibrary)
            DashboardAction("Lộ trình", "Theo chủ đề", Icons.Default.School, Modifier.weight(1f), onOpenLearningPath)
            DashboardAction("Thống kê", "Xem tiến độ", Icons.Default.QueryStats, Modifier.weight(1f), onOpenStats)
        }
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DailyGoalCard(activities: Int, goal: Int, onGoalChange: (Int) -> Unit) {
    val safeGoal = StudyGoalLogic.normalizeGoal(goal)
    val progress = StudyGoalLogic.progress(activities, safeGoal)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Mục tiêu hôm nay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$activities / $safeGoal lượt luyện và ôn", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
            Text(
                if (activities >= safeGoal) "Đã đạt mục tiêu hôm nay. Hãy duy trì nhịp học bền vững!"
                else "Còn ${StudyGoalLogic.remaining(activities, safeGoal)} lượt để hoàn thành mục tiêu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Chọn mục tiêu lượt học mỗi ngày", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StudyGoalLogic.supportedGoals.forEach { option ->
                    FilterChip(
                        selected = safeGoal == option,
                        onClick = { onGoalChange(option) },
                        label = { Text("$option") }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

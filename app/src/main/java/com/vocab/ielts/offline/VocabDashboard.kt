package com.vocab.ielts.offline

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6741D9),
                                Color(0xFF8B5CE6),
                                Color(0xFFF58C82)
                            )
                        ),
                        shape = RoundedCornerShape(30.dp)
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6C4CE3),
                                    Color(0xFF9A64E8),
                                    Color(0xFFFF9A76)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌟", fontSize = 21.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "HÀNH TRÌNH HÔM NAY",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text("🚀", fontSize = 29.sp)
                }
                Text(
                    if (totalWords == 0) "Bắt đầu hành trình của bạn"
                    else if (dueWords > 0) "Một chút hôm nay, tiến xa ngày mai."
                    else "Bạn đang xây dựng thói quen tốt.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (totalWords == 0) {
                    Text("Thêm những từ bạn muốn ghi nhớ.", color = Color.White.copy(alpha = 0.94f))
                } else if (dueWords > 0) {
                    Text("$dueWords từ đang chờ bạn.", color = Color.White.copy(alpha = 0.94f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardMetric("📚", "Cần ôn", dueWords.toString(), Modifier.weight(1f))
                    DashboardMetric("✨", "Đã học", learnedWords.toString(), Modifier.weight(1f))
                    DashboardMetric("🔥", "Ngày liền", "$activeStreak", Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        when {
                            totalWords == 0 -> onOpenLibrary()
                            dueWords > 0 -> onStartReview()
                            else -> onOpenPractice()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        when {
                            totalWords == 0 -> Icons.Default.MenuBook
                            dueWords > 0 -> Icons.Default.Refresh
                            else -> Icons.Default.School
                        },
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            totalWords == 0 -> "Thêm từ đầu tiên"
                            dueWords > 0 -> "Bắt đầu ôn tập · $dueWords từ"
                            else -> "Bắt đầu luyện tập"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        DailyGoalCard(activities = todayActivities, goal = dailyGoal, onGoalChange = onGoalChange)

        // Small visual legend makes the main actions scannable at a glance.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickVisualBadge(
                icon = "🧠",
                title = "Ghi nhớ",
                subtitle = "Nhớ chủ động",
                modifier = Modifier.weight(1f),
                tint = Color(0xFFEAE2FF)
            )
            QuickVisualBadge(
                icon = "🎧",
                title = "Nghe",
                subtitle = "Luyện phát âm",
                modifier = Modifier.weight(1f),
                tint = Color(0xFFFFE7DE)
            )
            QuickVisualBadge(
                icon = "✍️",
                title = "Vận dụng",
                subtitle = "Dùng từ mới",
                modifier = Modifier.weight(1f),
                tint = Color(0xFFDDF7EB)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardAction("Sổ từ", "", Icons.Default.MenuBook, Modifier.weight(1f), onOpenLibrary)
            DashboardAction("Lộ trình", "", Icons.Default.School, Modifier.weight(1f), onOpenLearningPath)
            DashboardAction("Tiến độ", "", Icons.Default.QueryStats, Modifier.weight(1f), onOpenStats)
        }
    }
}

@Composable
private fun DashboardMetric(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.94f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 20.sp)
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6741D9))
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
                    Text("🎯  Mục tiêu hôm nay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$activities / $safeGoal lượt luyện và ôn", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = Color(0xFF7655E8),
                trackColor = Color(0xFFE9E2FF)
            )
            Text(
                if (activities >= safeGoal) "🏆 Hoàn thành mục tiêu!"
                else "⚡ Còn ${StudyGoalLogic.remaining(activities, safeGoal)} lượt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StudyGoalLogic.supportedGoals.forEach { option ->
                    FilterChip(
                        selected = safeGoal == option,
                        onClick = { onGoalChange(option) },
                        label = { Text("🎯 $option") }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickVisualBadge(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    tint: Color
) {
    Surface(
        modifier = modifier,
        color = tint,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(icon, fontSize = 25.sp)
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF30234D)
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF594F69)
            )
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
            Surface(
                color = when (title) {
                    "Sổ từ" -> MaterialTheme.colorScheme.primaryContainer
                    "Lộ trình" -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = when (title) {
                        "Sổ từ" -> MaterialTheme.colorScheme.onPrimaryContainer
                        "Lộ trình" -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    modifier = Modifier.padding(11.dp)
                )
            }
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

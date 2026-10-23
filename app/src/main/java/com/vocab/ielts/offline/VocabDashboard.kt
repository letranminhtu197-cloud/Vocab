package com.vocab.ielts.offline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A compact home screen that points to one next action and keeps settings secondary. */
@Composable
fun DashboardOverview(
    totalWords: Int,
    dueWords: Int,
    learnedWords: Int,
    activeStreak: Int,
    todayActivities: Int,
    dailyGoal: Int,
    onGoalChange: (Int) -> Unit,
    reviewLimit: Int,
    onReviewLimitChange: (Int) -> Unit,
    challengeDone: Boolean,
    challengeSize: Int,
    onStartChallenge: () -> Unit,
    goldenLabel: String,
    goldenDueNow: Int,
    onStartReview: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Hôm nay", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Một phiên ngắn, đều đặn sẽ giúp từ vựng bền hơn.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StreakPill(streak = activeStreak)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("PHIÊN HỌC TIẾP THEO", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        totalWords == 0 -> "Tạo sổ từ vựng của riêng bạn"
                        dueWords > 0 -> "$dueWords từ đang đến hạn"
                        else -> "Duy trì nhịp học hôm nay"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    when {
                        totalWords == 0 -> "Thêm từ hoặc nhập một khóa học để bắt đầu."
                        else -> goldenLabel
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = when {
                            totalWords == 0 -> onOpenLibrary
                            dueWords > 0 -> onStartReview
                            else -> onOpenPractice
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(when {
                            totalWords == 0 -> "Mở sổ từ"
                            dueWords > 0 -> "Ôn $dueWords từ"
                            else -> "Bắt đầu luyện"
                        })
                    }
                    if (totalWords > 0) {
                        OutlinedButton(onClick = onOpenPractice, modifier = Modifier.weight(1f)) {
                            Text("Chọn bài luyện")
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardMetric("Tổng từ", totalWords.toString(), Modifier.weight(1f))
                    DashboardMetric("Đã ôn", learnedWords.toString(), Modifier.weight(1f))
                    DashboardMetric("Đến hạn", goldenDueNow.toString(), Modifier.weight(1f))
                }
            }
        }

        DailyGoalCard(
            activities = todayActivities,
            goal = dailyGoal,
            onGoalChange = onGoalChange,
            reviewLimit = reviewLimit,
            onReviewLimitChange = onReviewLimitChange,
            challengeDone = challengeDone,
            challengeSize = challengeSize,
            onStartChallenge = onStartChallenge
        )

    }
}

@Composable
private fun DashboardMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DailyGoalCard(
    activities: Int,
    goal: Int,
    onGoalChange: (Int) -> Unit,
    reviewLimit: Int,
    onReviewLimitChange: (Int) -> Unit,
    challengeDone: Boolean,
    challengeSize: Int,
    onStartChallenge: () -> Unit
) {
    val safeGoal = StudyGoalLogic.normalizeGoal(goal)
    val progress = StudyGoalLogic.progress(activities, safeGoal)
    var settingsExpanded by remember { mutableStateOf(false) }
    var goalMenuOpen by remember { mutableStateOf(false) }
    var reviewMenuOpen by remember { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Mục tiêu hôm nay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (activities >= safeGoal) "$activities / $safeGoal lượt · Hoàn thành"
                        else "$activities / $safeGoal lượt · Còn ${StudyGoalLogic.remaining(activities, safeGoal)} lượt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { settingsExpanded = !settingsExpanded }) {
                    Text(if (settingsExpanded) "Thu gọn" else "Điều chỉnh")
                }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp))
            if (settingsExpanded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mục tiêu", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { goalMenuOpen = true }) { Text("$safeGoal lượt") }
                    DropdownMenu(expanded = goalMenuOpen, onDismissRequest = { goalMenuOpen = false }) {
                        StudyGoalLogic.supportedGoals.forEach { option ->
                            DropdownMenuItem(text = { Text("$option lượt mỗi ngày") }, onClick = {
                                onGoalChange(option)
                                goalMenuOpen = false
                            })
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Số từ mỗi phiên ôn", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { reviewMenuOpen = true }) {
                        Text(if (reviewLimit == 0) "Tất cả" else "$reviewLimit từ")
                    }
                    DropdownMenu(expanded = reviewMenuOpen, onDismissRequest = { reviewMenuOpen = false }) {
                        listOf(10, 20, 50, 0).forEach { option ->
                            DropdownMenuItem(text = { Text(if (option == 0) "Không giới hạn" else "$option từ") }, onClick = {
                                onReviewLimitChange(option)
                                reviewMenuOpen = false
                            })
                        }
                    }
                }
            }
            Spacer(Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Thử thách hôm nay", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (challengeDone) "Đã hoàn thành $challengeSize từ"
                        else "$challengeSize từ trong sổ của bạn",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (challengeDone) {
                    Text("Đã xong", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold)
                } else {
                    OutlinedButton(onClick = onStartChallenge, enabled = challengeSize > 0) { Text("Bắt đầu") }
                }
            }
        }
    }
}


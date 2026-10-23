package com.vocab.ielts.offline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Learning-first home: one clear next action, progress, and direct navigation. */
@Composable
fun LearningHomeScreen(
    words: List<WordEntity>, dueCount: Int, newCount: Int, weakCount: Int,
    todayActivities: Int, dailyGoal: Int, activeDays: Int,
    onReview: () -> Unit, onPractice: () -> Unit, onLibrary: () -> Unit,
    onStats: () -> Unit, onImport: () -> Unit
) {
    val goalProgress = if (dailyGoal <= 0) 0f else (todayActivities.toFloat() / dailyGoal).coerceIn(0f, 1f)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("VOCAB · IELTS", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                Text("Học đúng cách.\nNhớ lâu hơn.", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp)
                Text("Hệ thống học cá nhân, dựa trên bộ từ bạn tự nhập.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary) {
                            Icon(Icons.Default.Bolt, contentDescription = null,
                                modifier = Modifier.padding(12.dp).size(28.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Phiên học tiếp theo", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(when {
                                words.isEmpty() -> "Nhập bộ từ của bạn để bắt đầu"
                                dueCount > 0 -> "$dueCount từ đang đến hạn ôn"
                                newCount > 0 -> "$newCount từ mới sẵn sàng học"
                                else -> "Ôn tập tự do để củng cố kiến thức"
                            }, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f))
                        }
                    }
                    Button(onClick = { if (dueCount > 0) onReview() else onPractice() },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (words.isEmpty()) "Bắt đầu bằng cách nhập từ" else if (dueCount > 0) "Bắt đầu ôn SRS" else if (newCount > 0) "Học từ mới" else "Bắt đầu luyện tập",
                            fontWeight = FontWeight.Bold)
                    }
                    if (words.isEmpty()) {
                        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)) { Text("Nhập từ vựng từ CSV") }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Tổng từ", words.size.toString(), Icons.Default.LibraryBooks, Modifier.weight(1f))
                MetricCard("Đến hạn", dueCount.toString(), Icons.Default.Refresh, Modifier.weight(1f))
                MetricCard("Từ mới", newCount.toString(), Icons.Default.AutoStories, Modifier.weight(1f))
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Mục tiêu hôm nay", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("$todayActivities / $dailyGoal hoạt động", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${(goalProgress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    }
                    LinearProgressIndicator(progress = { goalProgress }, modifier = Modifier.fillMaxWidth().height(9.dp))
                    Text("Chuỗi hoạt động: $activeDays ngày · $weakCount từ cần củng cố",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("Không gian học tập", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) }
        item { ActionCard("Học chủ động", "Gợi nhớ bằng cách tự gõ · nghe và gõ · điền ngữ cảnh · ghép cặp", Icons.Default.PlayArrow, onPractice) }
        item { ActionCard("Sổ từ của tôi", "Tìm kiếm, chỉnh sửa và quản lý bộ từ đã nhập", Icons.Default.LibraryBooks, onLibrary) }
        item { ActionCard("Phân tích tiến độ", "Theo dõi hoạt động và những từ cần củng cố", Icons.Default.QueryStats, onStats) }
        item {
            Text("Dữ liệu lưu trên thiết bị. Ứng dụng không tự thêm từ vựng và không cần tài khoản.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(12.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

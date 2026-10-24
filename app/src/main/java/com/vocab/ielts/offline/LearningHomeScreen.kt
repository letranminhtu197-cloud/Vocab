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

/** A learning-first dashboard with one recommended action and a calm, consistent hierarchy. */
@Composable
fun LearningHomeScreen(
    words: List<WordEntity>, dueCount: Int, newCount: Int, weakCount: Int,
    todayActivities: Int, dailyGoal: Int, activeDays: Int,
    onReview: () -> Unit, onPractice: () -> Unit, onLibrary: () -> Unit,
    onStats: () -> Unit, onImport: () -> Unit
) {
    val goal = dailyGoal.coerceAtLeast(1)
    val progress = (todayActivities.toFloat() / goal).coerceIn(0f, 1f)
    val hasWords = words.isNotEmpty()
    val mainTitle = when {
        !hasWords -> "Bắt đầu với bộ từ của bạn"
        dueCount > 0 -> "Ôn tập đúng thời điểm"
        newCount > 0 -> "Học một nhóm từ mới"
        else -> "Củng cố những từ đã học"
    }
    val mainDescription = when {
        !hasWords -> "Nhập file CSV từ vựng của bạn. Vocab không tự thêm từ."
        dueCount > 0 -> "$dueCount từ đang đến hạn. Ôn hôm nay giúp giữ nhịp ghi nhớ."
        newCount > 0 -> "$newCount từ mới đang chờ bạn làm quen."
        else -> "Bạn đã xử lý hết từ đến hạn. Luyện gợi nhớ tự do nếu muốn học thêm."
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(17.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.AutoStories, contentDescription = null,
                        modifier = Modifier.padding(13.dp).size(27.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("VOCAB · IELTS", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                    Text("Không chỉ nhận ra từ.\nHãy nhớ và dùng được từ.", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
                    Text("Bộ từ của bạn · lưu trữ trên thiết bị", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.primary) {
                            Icon(Icons.Default.Bolt, contentDescription = null,
                                modifier = Modifier.padding(11.dp).size(25.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Gợi ý tiếp theo", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(mainTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Text(mainDescription, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f))
                    Button(
                        onClick = { if (!hasWords) onImport() else if (dueCount > 0) onReview() else onPractice() },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(if (hasWords) Icons.Default.PlayArrow else Icons.Default.LibraryBooks, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(when {
                            !hasWords -> "Nhập bộ từ đầu tiên"
                            dueCount > 0 -> "Bắt đầu ôn $dueCount từ"
                            newCount > 0 -> "Bắt đầu học từ mới"
                            else -> "Bắt đầu luyện gợi nhớ"
                        }, fontWeight = FontWeight.Bold)
                    }
                    if (hasWords && dueCount == 0 && newCount == 0) {
                        OutlinedButton(onClick = onLibrary, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)) { Text("Quản lý bộ từ") }
                    }
                }
            }
        }
        item {
            Text("Tổng quan bộ từ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Tổng từ", words.size.toString(), Icons.Default.LibraryBooks, Modifier.weight(1f))
                MetricCard("Đến hạn", dueCount.toString(), Icons.Default.Refresh, Modifier.weight(1f))
                MetricCard("Từ mới", newCount.toString(), Icons.Default.AutoStories, Modifier.weight(1f))
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Mục tiêu hôm nay", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("$todayActivities / $goal hoạt động", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(onClick = onStats, label = { Text("Chuỗi $activeDays ngày") }, leadingIcon = {
                            Icon(Icons.Default.Bolt, contentDescription = null)
                        })
                        AssistChip(onClick = onStats, label = { Text("$weakCount từ cần củng cố") }, leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        })
                    }
                    if (progress >= 1f) {
                        Text("Đã hoàn thành mục tiêu hôm nay. Bạn có thể dừng tại đây hoặc luyện thêm nếu muốn.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item { Text("Không gian học tập", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) }
        item { ActionCard("Học chủ động", "Tự nhớ và gõ từ · nghe và gõ · điền ngữ cảnh · ghép cặp", Icons.Default.PlayArrow, onPractice) }
        item { ActionCard("Sổ từ của tôi", "Tìm kiếm, lọc, sửa từ và quản lý dữ liệu đã nhập", Icons.Default.LibraryBooks, onLibrary) }
        item { ActionCard("Phân tích tiến độ", "Xem độ chính xác theo dạng bài và lịch sử ôn tập", Icons.Default.QueryStats, onStats) }
        item {
            Text("Dữ liệu được lưu trên thiết bị. Bạn tự kiểm soát bộ từ; Vocab không tự thêm từ và không cần tài khoản.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
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

package com.vocab.ielts.offline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LessonScreen(
    words: List<WordEntity>,
    modifier: Modifier = Modifier,
    sessionSize: Int = 5,
    onReview: (WordEntity, ReviewRating) -> Unit
) {
    // Build a stable mixed deck: overdue/new/weak words rank ahead of mastered words.
    // Freeze the IDs for this session so SRS updates never move the current question.
    val safeSessionSize = sessionSize.coerceIn(1, 50)
    val lessonIds = remember(words.map { it.id }, safeSessionSize) {
        StudyQueueLogic.prioritize(words, System.currentTimeMillis())
            .take(safeSessionSize)
            .map { it.id }
    }
    // Wrong cards are appended once for an end-of-session retry pass. A failed retry
    // is not appended again, preventing an endless loop.
    val retryIds = remember(lessonIds) { mutableStateListOf<Long>() }
    val lessonDeck = (lessonIds + retryIds.toList()).mapNotNull { id -> words.firstOrNull { it.id == id } }
    var index by remember(lessonIds) { mutableIntStateOf(0) }
    var revealed by remember(lessonIds) { mutableStateOf(false) }
    var remembered by remember(lessonIds) { mutableIntStateOf(0) }
    var retry by remember(lessonIds) { mutableIntStateOf(0) }
    val current = lessonDeck.getOrNull(index)
    val completed = lessonDeck.isNotEmpty() && index >= lessonDeck.size

    fun rateCurrent(rating: ReviewRating) {
        val word = current ?: return
        onReview(word, rating)
        if (rating == ReviewRating.AGAIN) {
            retry++
            if (word.id !in retryIds) retryIds.add(word.id)
        } else {
            remembered++
        }
        index++
        revealed = false
    }

    LazyColumn(modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(color = MaterialTheme.colorScheme.tertiary, shape = RoundedCornerShape(18.dp)) {
                        Text("🃏", Modifier.padding(13.dp), fontSize = 29.sp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Bài học theo mục tiêu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text("Nhớ chủ động · ôn đúng lúc · tiến bộ từng ngày", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    Text("🌱", fontSize = 26.sp)
                }
            }
        }
        if (words.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📚 Bắt đầu bằng bộ từ của bạn", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Chưa có từ nào nên chưa thể tạo bài học. Thêm từ trong Sổ từ hoặc nhập file CSV. Ứng dụng không tự nạp từ mẫu.")
                    }
                }
            }
        } else if (completed) {
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆 Hoàn thành phiên học!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Đã nhớ: $remembered • Lượt cần củng cố: $retry")
                        Text("Từ bạn đánh giá “Lại” đã được đưa trở lại cuối phiên một lần để luyện nhớ chủ động. Lịch ôn SRS cũng đã được cập nhật.")
                        Button(
                            onClick = {
                                index = 0
                                revealed = false
                                remembered = 0
                                retry = 0
                                retryIds.clear()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Học lại phiên này") }
                    }
                }
            }
        } else if (current != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Từ ${index + 1} / ${lessonDeck.size}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("Mục tiêu: ${lessonDeck.size} từ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(progress = (index + 1).toFloat() / lessonDeck.size, modifier = Modifier.fillMaxWidth())
            }
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(current.term, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        if (current.ipa.isNotBlank()) Text(current.ipa, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (current.partOfSpeech.isNotBlank()) Text(current.partOfSpeech, color = MaterialTheme.colorScheme.primary)
                        ReviewWindowLabel(current)
                        if (revealed) {
                            HorizontalDivider()
                            Text(current.meaning, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            if (current.topic.isNotBlank()) Text("Chủ đề: ${current.topic}")
                            if (current.collocations.isNotBlank()) Text("Collocations: ${current.collocations}")
                            if (current.synonyms.isNotBlank()) Text("Từ đồng nghĩa: ${current.synonyms}")
                            if (current.wordFamily.isNotBlank()) Text("Họ từ: ${current.wordFamily}")
                            if (current.tags.isNotBlank()) Text("Thẻ: ${current.tags}", style = MaterialTheme.typography.labelSmall)
                            if (current.notes.isNotBlank()) Text("Ghi chú: ${current.notes}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (current.example.isNotBlank()) Text("Ví dụ: ${current.example}")
                            if (current.example.isBlank()) Text("Chưa có ví dụ. Bạn có thể bổ sung trong phần sửa từ hoặc qua CSV.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Bạn nhớ từ này đến mức nào?", fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { rateCurrent(ReviewRating.AGAIN) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Lại")
                                        Text(SrsScheduler.intervalLabel(current.srsLevel, ReviewRating.AGAIN), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { rateCurrent(ReviewRating.HARD) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Khó")
                                        Text(SrsScheduler.intervalLabel(current.srsLevel, ReviewRating.HARD), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { rateCurrent(ReviewRating.GOOD) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Tốt")
                                        Text(SrsScheduler.intervalLabel(current.srsLevel, ReviewRating.GOOD), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Button(
                                    onClick = { rateCurrent(ReviewRating.EASY) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Dễ")
                                        Text(SrsScheduler.intervalLabel(current.srsLevel, ReviewRating.EASY), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        } else {
                            Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) { Text("Lật thẻ · hiện nghĩa và ví dụ") }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun LearningPathScreen(
    words: List<WordEntity>,
    modifier: Modifier = Modifier,
    onStart: (String?) -> Unit,
    onPractice: () -> Unit,
    sessionSize: Int,
    onSessionSizeChange: (Int) -> Unit,
    onOpenLibrary: () -> Unit
) {
    val now = System.currentTimeMillis()
    val overall = LearningPathLogic.overall(words, now)
    val topics = LearningPathLogic.topics(words, now)
    val mastered = words.count { it.srsLevel >= 4 }
    val newWords = words.count { it.reviewCount == 0 }
    val dueWords = words.count { it.nextReviewAtMillis == 0L || it.nextReviewAtMillis <= now }
    val maxTopicWords = topics.maxOfOrNull { it.total }?.coerceAtLeast(1) ?: 1

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("LỘ TRÌNH CỦA BẠN", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.ExtraBold)
                Text("Từng từ một. Tiến bộ mỗi ngày.", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold)
                Text("Lộ trình tự hình thành từ những từ bạn nhập và cách bạn ôn tập — không có tiến độ giả.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary) {
                            Text("✦", modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = MaterialTheme.colorScheme.onPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Bản đồ ghi nhớ", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(if (words.isEmpty()) "Sẵn sàng cho từ đầu tiên của bạn"
                                else "${overall.learned} / ${overall.total} từ đã được ôn",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f))
                        }
                        Text("${overall.percent}%", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    LinearProgressIndicator(
                        progress = { overall.percent / 100f },
                        modifier = Modifier.fillMaxWidth().height(9.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PathMetric("🌱", "Từ mới", newWords, Modifier.weight(1f))
                        PathMetric("⏰", "Đến hạn", dueWords, Modifier.weight(1f))
                        PathMetric("🏆", "Vững", mastered, Modifier.weight(1f))
                    }
                    Button(
                        onClick = { if (words.isEmpty()) onOpenLibrary() else onStart(null) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Text(if (words.isEmpty()) "Thêm từ để bắt đầu" else "Tiếp tục hành trình",
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cài đặt phiên học", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Chọn số từ cho mỗi lượt. Phiên học ưu tiên từ mới, từ đến hạn và từ cần củng cố.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(5, 10, 15, 20).forEach { target ->
                            FilterChip(
                                selected = sessionSize == target,
                                onClick = { onSessionSizeChange(target) },
                                label = { Text("$target từ") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onPractice, modifier = Modifier.weight(1f)) { Text("Luyện quiz") }
                        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.weight(1f)) { Text("Quản lý từ") }
                    }
                }
            }
        }
        if (words.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 42.sp)
                        Text("Chặng đầu tiên đang chờ bạn", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text("Thêm từ thủ công hoặc nhập CSV của riêng bạn. Khi bạn bắt đầu ôn, lộ trình sẽ tự cập nhật.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) { Text("Mở sổ từ") }
                    }
                }
            }
        } else {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Các chặng học", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("${topics.size} chủ đề · sắp xếp theo tên", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(" ${topics.size} ", modifier = Modifier.padding(7.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(topics, key = { it.topic }) { topic ->
                val topicTint = if (topic.weak > 0) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.tertiaryContainer
                val topicOnTint = if (topic.weak > 0) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onTertiaryContainer
                Card(shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = RoundedCornerShape(15.dp), color = topicTint) {
                                Text(if (topic.topic == "Chưa phân loại") "✧" else "◈",
                                    modifier = Modifier.padding(12.dp), color = topicOnTint, fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(topic.topic, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                Text("${topic.total} từ trong chặng", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${topic.percent}%", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(
                            progress = { topic.percent / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = if (topic.weak > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            PathMiniStat("Đã ôn", "${topic.learned}/${topic.total}", Modifier.weight(1f))
                            PathMiniStat("Đến hạn", topic.due.toString(), Modifier.weight(1f))
                            PathMiniStat("Cần củng cố", topic.weak.toString(), Modifier.weight(1f))
                        }
                        Button(onClick = { onStart(topic.topic) }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp)) {
                            Text(if (topic.topic == "Chưa phân loại") "Học từ chưa phân loại" else "Bắt đầu chặng",
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            Text("Tiến độ dựa trên lịch ôn và kết quả thực tế của bạn. Từ chưa được ôn sẽ không được tính là đã ghi nhớ.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PathMetric(icon: String, label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 19.sp)
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PathMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

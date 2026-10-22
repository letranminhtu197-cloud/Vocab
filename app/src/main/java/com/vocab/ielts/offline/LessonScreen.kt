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
    // Keep the randomized deck order stable, but always resolve current entities
    // so edits to a word's meaning/example are reflected without reshuffling the lesson.
    val safeSessionSize = sessionSize.coerceIn(1, 50)
    val lessonIds = remember(words.map { it.id }, safeSessionSize) {
        val newWords = words.filter { it.reviewCount == 0 || it.srsLevel == 0 }
        (newWords.ifEmpty { words }).shuffled().take(safeSessionSize).map { it.id }
    }
    val lessonDeck = lessonIds.mapNotNull { id -> words.firstOrNull { it.id == id } }
    var index by remember(lessonDeck.map { it.id }) { mutableIntStateOf(0) }
    var revealed by remember(lessonDeck.map { it.id }) { mutableStateOf(false) }
    var remembered by remember(lessonDeck.map { it.id }) { mutableIntStateOf(0) }
    var retry by remember(lessonDeck.map { it.id }) { mutableIntStateOf(0) }
    val current = lessonDeck.getOrNull(index)
    val completed = lessonDeck.isNotEmpty() && index >= lessonDeck.size

    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Bài học theo mục tiêu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text("Một phiên ngắn dùng từ do bạn tự nhập, tối đa $safeSessionSize từ. Vocab ưu tiên từ chưa ôn; nếu đã học hết từ mới, phiên sẽ lấy từ trong sổ.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (words.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Bắt đầu bằng bộ từ của bạn", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Chưa có từ nào nên chưa thể tạo bài học. Thêm từ trong Sổ từ hoặc nhập file CSV. Ứng dụng không tự nạp từ mẫu.")
                    }
                }
            }
        } else if (completed) {
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hoàn thành phiên học!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Đã nhớ: $remembered • Cần ôn lại: $retry")
                        Text("Các đánh giá đã được lưu vào tiến độ SRS.")
                        Button(onClick = { index = 0; revealed = false; remembered = 0; retry = 0 }, modifier = Modifier.fillMaxWidth()) { Text("Học lại phiên này") }
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
                            if (current.wordFamily.isNotBlank()) Text("Họ từ: ${current.wordFamily}")
                            if (current.tags.isNotBlank()) Text("Thẻ: ${current.tags}", style = MaterialTheme.typography.labelSmall)
                            if (current.notes.isNotBlank()) Text("Ghi chú: ${current.notes}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (current.example.isNotBlank()) Text("Ví dụ: ${current.example}")
                            if (current.example.isBlank()) Text("Chưa có ví dụ. Bạn có thể bổ sung trong phần sửa từ hoặc qua CSV.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { onReview(current, ReviewRating.AGAIN); retry++; index++; revealed = false },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Chưa nhớ") }
                                Button(
                                    onClick = { onReview(current, ReviewRating.GOOD); remembered++; index++; revealed = false },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Đã nhớ") }
                            }
                        } else {
                            Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) { Text("Hiện nghĩa và ví dụ") }
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
    sessionSize: Int,
    onSessionSizeChange: (Int) -> Unit,
    onOpenLibrary: () -> Unit
) {
    val now = System.currentTimeMillis()
    val overall = LearningPathLogic.overall(words, now)
    val topics = LearningPathLogic.topics(words, now)

    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Lộ trình học", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Theo dõi tiến độ bằng chính bộ từ bạn tự nhập. Mỗi chủ đề có số từ đã học, từ đến hạn và từ cần luyện lại.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tiến độ toàn bộ sổ từ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${overall.learned}/${overall.total} từ đã được ôn ít nhất một lần • ${overall.percent}%", style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(progress = { overall.percent / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("Số từ mỗi phiên", fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(5, 10, 15, 20).forEach { target ->
                            FilterChip(
                                selected = sessionSize == target,
                                onClick = { onSessionSizeChange(target) },
                                label = { Text("$target") }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { onStart(null) }, label = { Text("Bắt đầu học") })
                        AssistChip(onClick = { onStart(null) }, label = { Text("Luyện tập") })
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Đến hạn", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(overall.due.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Từ yếu", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(overall.weak.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        if (words.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Lộ trình bắt đầu từ bộ từ của bạn", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Chưa có từ vựng nên chưa thể tạo tiến độ chủ đề. Thêm từ thủ công hoặc nhập CSV; ứng dụng không tự tải bộ từ.")
                        Button(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) { Text("Mở sổ từ") }
                    }
                }
            }
        } else {
            item { Text("Các chặng theo chủ đề", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(topics, key = { it.topic }) { topic ->
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(topic.topic, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${topic.percent}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(progress = { topic.percent / 100f }, modifier = Modifier.fillMaxWidth())
                        Text("${topic.learned}/${topic.total} từ đã ôn • ${topic.due} đến hạn • ${topic.weak} từ yếu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { onStart(topic.topic) }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (topic.topic == "Chưa phân loại") "Học từ chưa phân loại" else "Bắt đầu chặng này")
                        }
                    }
                }
            }
        }
    }
}

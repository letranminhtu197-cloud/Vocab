package com.vocab.ielts.offline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

    fun rateCurrent(rating: ReviewRating) {
        val word = current ?: return
        onReview(word, rating)
        if (rating == ReviewRating.AGAIN) retry++ else remembered++
        index++
        revealed = false
    }

    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Bài học theo mục tiêu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Text(
                "Học chủ động: tự nhớ nghĩa trước khi lật thẻ, rồi chọn mức độ nhớ để Vocab lên lịch ôn phù hợp.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

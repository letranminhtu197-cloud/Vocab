package com.vocab.ielts.offline

import androidx.compose.foundation.layout.*
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
    onReview: (WordEntity, ReviewRating) -> Unit
) {
    val lessonDeck = remember(words.map { it.id }) {
        val newWords = words.filter { it.reviewCount == 0 || it.srsLevel == 0 }
        (newWords.ifEmpty { words }).shuffled().take(5)
    }
    var index by remember(lessonDeck.map { it.id }) { mutableIntStateOf(0) }
    var revealed by remember(lessonDeck.map { it.id }) { mutableStateOf(false) }
    var remembered by remember(lessonDeck.map { it.id }) { mutableIntStateOf(0) }
    var retry by remember(lessonDeck.map { it.id }) { mutableIntStateOf(0) }
    val current = lessonDeck.getOrNull(index)
    val completed = lessonDeck.isNotEmpty() && index >= lessonDeck.size

    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Bài học 5 từ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text("Một phiên ngắn dùng từ do bạn tự nhập. Vocab ưu tiên từ chưa ôn; nếu đã học hết từ mới, phiên sẽ lấy 5 từ trong sổ.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                    Text("Mục tiêu: 5 từ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        if (revealed) {
                            HorizontalDivider()
                            Text(current.meaning, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            if (current.topic.isNotBlank()) Text("Chủ đề: ${current.topic}")
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

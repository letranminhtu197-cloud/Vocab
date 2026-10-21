package com.vocab.ielts.offline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuizScreen(words: List<WordEntity>, modifier: Modifier = Modifier) {
    var index by remember { mutableIntStateOf(0) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var answered by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    val word = words.getOrNull(if (words.isEmpty()) 0 else index % words.size)
    val options = remember(word?.id, words.map { it.id }) {
        if (word == null) emptyList() else (words.filter { it.id != word.id }.shuffled().take(3) + word).shuffled()
    }

    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Luyện tập nhanh", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text("Chọn nghĩa tiếng Việt đúng. Sau khi trả lời, ứng dụng sẽ giải thích kết quả.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (words.size < 2) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cần ít nhất 2 từ để luyện tập", fontWeight = FontWeight.Bold)
                        Text("Thêm từ của bạn hoặc nhập CSV. Bốn từ trở lên sẽ tạo được câu hỏi với tối đa bốn lựa chọn.")
                    }
                }
            }
        } else {
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Điểm: ${score} / ${total}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(word?.term.orEmpty(), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        word?.ipa?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text("Chọn nghĩa đúng:", fontWeight = FontWeight.SemiBold)
                        options.forEach { option ->
                            val correct = option.id == word?.id
                            val chosen = option.id == selectedId
                            val fill = when {
                                answered && correct -> androidx.compose.ui.graphics.Color(0xFFDDF3E4)
                                answered && chosen -> androidx.compose.ui.graphics.Color(0xFFFFE3E0)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Surface(
                                onClick = {
                                    if (!answered) {
                                        selectedId = option.id
                                        answered = true
                                        total++
                                        if (correct) score++
                                    }
                                },
                                color = fill,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(option.meaning, Modifier.padding(14.dp)) }
                        }
                        if (answered && word != null) {
                            Text(if (selectedId == word.id) "Chính xác!" else "Chưa đúng — đáp án: ${word.meaning}", fontWeight = FontWeight.Bold)
                            if (word.example.isNotBlank()) Text("Ví dụ: ${word.example}")
                        }
                        Button(
                            onClick = { index = (index + 1) % words.size; selectedId = null; answered = false },
                            enabled = answered,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Câu tiếp theo") }
                        TextButton(onClick = { score = 0; total = 0; index = 0; selectedId = null; answered = false }, modifier = Modifier.align(Alignment.End)) {
                            Text("Đặt lại điểm")
                        }
                    }
                }
            }
        }
    }
}

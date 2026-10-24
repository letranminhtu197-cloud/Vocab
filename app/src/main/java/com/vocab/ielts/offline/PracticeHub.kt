package com.vocab.ielts.offline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Offline active-recall practice using only words in the user's library. */
@Composable
fun PracticeHub(
    words: List<WordEntity>,
    modifier: Modifier = Modifier,
    startWithDue: Boolean = true,
    onAnswerRecorded: (WordEntity, Boolean) -> Unit
) {
    var mode by remember { mutableStateOf(PracticeMode.MULTIPLE_CHOICE) }
    var dueOnly by remember { mutableStateOf(startWithDue) }
    var sessionSize by remember { mutableIntStateOf(10) }
    var started by remember { mutableStateOf(false) }
    var order by remember { mutableStateOf<List<Long>>(emptyList()) }
    var position by remember { mutableIntStateOf(0) }
    var answer by remember { mutableStateOf("") }
    var typed by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    var correct by remember { mutableIntStateOf(0) }
    var answered by remember { mutableIntStateOf(0) }

    val now = System.currentTimeMillis()
    val available = words.filterNot { it.excludedFromReview }.filter {
        !dueOnly || it.nextReviewAtMillis == 0L || it.nextReviewAtMillis <= now
    }
    val target = available.firstOrNull { it.id == order.getOrNull(position) }
    val question = remember(target?.id, mode, words) {
        target?.let { PracticeEngine.createQuestion(it, words, mode) }
    }
    val eligible = available.filter { PracticeEngine.createQuestion(it, words, mode) != null }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("TRUNG TÂM LUYỆN TẬP", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        Text("Học chủ động", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        if (!started) {
            Text("Chọn dạng bài và luyện bằng kho từ cá nhân. Kết quả được ghi vào lịch ôn SRS.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(
                PracticeMode.MULTIPLE_CHOICE to ("Chọn nghĩa đúng" to "Nhận diện từ và chọn nghĩa."),
                PracticeMode.LISTEN_AND_TYPE to ("Gõ từ tiếng Anh" to "Nhìn nghĩa và tự gõ từ cần nhớ."),
                PracticeMode.CONTEXTUAL_FILL to ("Điền từ theo ngữ cảnh" to "Điền từ bị ẩn trong câu ví dụ.")
            ).forEach { (value, labels) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (mode == value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    onClick = { mode = value }
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(labels.first, fontWeight = FontWeight.Bold)
                        Text(labels.second, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            FilterChip(selected = dueOnly, onClick = { dueOnly = !dueOnly }, label = { Text("Chỉ từ đến hạn") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 20).forEach { n ->
                    FilterChip(selected = sessionSize == n, onClick = { sessionSize = n }, label = { Text("$n câu") })
                }
            }
            Text("${eligible.size} từ đủ dữ liệu cho dạng bài đã chọn")
            Button(enabled = eligible.isNotEmpty(), onClick = {
                order = eligible.shuffled().take(sessionSize).map { it.id }
                position = 0; correct = 0; answered = 0
                answer = ""; typed = ""; checked = false; started = true
            }, modifier = Modifier.fillMaxWidth()) { Text("Bắt đầu phiên học") }
            if (words.isEmpty()) Text("Sổ từ đang trống. Hãy nhập CSV hoặc thêm từ trước khi học.")
        } else if (position >= order.size || order.isEmpty()) {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Hoàn thành phiên học", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("$correct / $answered câu đúng", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { started = false }, modifier = Modifier.fillMaxWidth()) { Text("Chọn dạng bài khác") }
                }
            }
        } else if (question == null || target == null) {
            Text("Không tạo được câu hỏi từ dữ liệu hiện tại.")
            Button(onClick = { position++; checked = false; answer = ""; typed = "" }) { Text("Bỏ qua") }
        } else {
            Text("Câu ${position + 1}/${order.size}  ·  Đúng: $correct",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { (position + 1).toFloat() / order.size }, modifier = Modifier.fillMaxWidth())
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(question.prompt, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    when (mode) {
                        PracticeMode.MULTIPLE_CHOICE -> LazyColumn(
                            modifier = Modifier.heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(question.options) { option ->
                                val isCorrect = checked && PracticeEngine.grade(question, option).correct
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
                                        checked && answer == option -> MaterialTheme.colorScheme.errorContainer
                                        answer == option -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    onClick = { if (!checked) answer = option }
                                ) { Text(option, Modifier.fillMaxWidth().padding(13.dp)) }
                            }
                        }
                        else -> OutlinedTextField(
                            value = typed, onValueChange = { if (!checked) typed = it },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Nhập đáp án") }, singleLine = true
                        )
                    }
                    if (checked) {
                        val result = PracticeEngine.grade(question, if (mode == PracticeMode.MULTIPLE_CHOICE) answer else typed)
                        Text(result.feedback, fontWeight = FontWeight.Bold,
                            color = if (result.correct) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                        if (question.explanation.isNotBlank()) Text("Ví dụ: ${question.explanation}")
                    }
                    Button(
                        enabled = checked || if (mode == PracticeMode.MULTIPLE_CHOICE) answer.isNotBlank() else typed.isNotBlank(),
                        onClick = {
                            if (!checked) {
                                val result = PracticeEngine.grade(question, if (mode == PracticeMode.MULTIPLE_CHOICE) answer else typed)
                                checked = true; answered++
                                if (result.correct) correct++
                                onAnswerRecorded(target, result.correct)
                            } else {
                                position++; answer = ""; typed = ""; checked = false
                            }
                        }, modifier = Modifier.fillMaxWidth()
                    ) { Text(if (checked) "Câu tiếp theo" else "Kiểm tra đáp án") }
                }
            }
            OutlinedButton(onClick = { started = false }, modifier = Modifier.fillMaxWidth()) { Text("Kết thúc phiên") }
        }
    }
}

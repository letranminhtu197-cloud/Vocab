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

private const val QUIZ_MEANING = "Chọn nghĩa"
private const val QUIZ_RECALL = "Tìm từ"
private const val QUIZ_TYPED = "Gõ từ"
private const val QUIZ_SENTENCE = "Viết câu"
private const val QUIZ_CLOZE = "Điền từ"
private const val QUIZ_WEAK = "Từ yếu"

@Composable
fun QuizScreen(words: List<WordEntity>, modifier: Modifier = Modifier, onAnswerRecorded: (Boolean) -> Unit = {}) {
    var mode by remember { mutableStateOf(QUIZ_MEANING) }
    var index by remember { mutableIntStateOf(0) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var answerText by remember { mutableStateOf("") }
    var answered by remember { mutableStateOf(false) }
    var lastCorrect by remember { mutableStateOf<Boolean?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }

    val weakWords = words.filter { it.lapseCount > 0 }
    val clozeWords = words.filter { QuizLogic.clozeSentence(it.example, it.term) != null }
    val activeWords = when (mode) {
        QUIZ_CLOZE -> clozeWords
        QUIZ_WEAK -> weakWords
        else -> words
    }
    val word = activeWords.getOrNull(if (activeWords.isEmpty()) 0 else index % activeWords.size)
    val options = remember(word?.id, words.map { it.id }) {
        if (word == null) emptyList() else (words.filter { it.id != word.id }.shuffled().take(3) + word).shuffled()
    }

    fun resetForMode(nextMode: String) {
        mode = nextMode
        index = 0
        selectedId = null
        answerText = ""
        answered = false
        lastCorrect = null
    }
    fun submitTypedAnswer() {
        if (!answered && word != null && answerText.isNotBlank()) {
            val correct = QuizLogic.isCorrectAnswer(answerText, word.term)
            lastCorrect = correct
            answered = true
            total++
            if (correct) score++
            onAnswerRecorded(correct)
        }
    }
    fun submitCloze() = submitTypedAnswer()
    fun rateSentence(usedCorrectly: Boolean) {
        if (!answered && word != null) {
            lastCorrect = usedCorrectly
            answered = true
            total++
            if (usedCorrectly) score++
            onAnswerRecorded(usedCorrectly)
        }
    }

    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Luyện tập IELTS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text("Luyện nhớ nghĩa, tự gọi lại từ theo ngữ cảnh hoặc tập trung vào các từ từng quên. Điểm của phiên được giữ khi đổi chế độ.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = mode == QUIZ_MEANING, onClick = { resetForMode(QUIZ_MEANING) }, label = { Text(QUIZ_MEANING) })
                FilterChip(selected = mode == QUIZ_RECALL, onClick = { resetForMode(QUIZ_RECALL) }, label = { Text(QUIZ_RECALL) })
                FilterChip(selected = mode == QUIZ_TYPED, onClick = { resetForMode(QUIZ_TYPED) }, label = { Text(QUIZ_TYPED) })
                FilterChip(selected = mode == QUIZ_CLOZE, onClick = { resetForMode(QUIZ_CLOZE) }, label = { Text(QUIZ_CLOZE) })
                FilterChip(selected = mode == QUIZ_SENTENCE, onClick = { resetForMode(QUIZ_SENTENCE) }, label = { Text(QUIZ_SENTENCE) })
                FilterChip(selected = mode == QUIZ_WEAK, onClick = { resetForMode(QUIZ_WEAK) }, label = { Text(QUIZ_WEAK) })
            }
        }
        if (mode == QUIZ_WEAK && weakWords.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chưa có từ yếu được ghi nhận", fontWeight = FontWeight.Bold)
                        Text("Khi ôn flashcard, chọn “Lại” cho từ chưa nhớ. Vocab sẽ ghi nhận số lần quên để gom các từ đó vào chế độ này.")
                    }
                }
            }
        } else if (mode == QUIZ_CLOZE && clozeWords.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chưa có ví dụ phù hợp", fontWeight = FontWeight.Bold)
                        Text("Thêm câu ví dụ có chứa đúng từ vựng đó vào trường Example trong sổ từ hoặc CSV. Ví dụ: “We need to allocate more resources.”")
                    }
                }
            }
        } else if (words.size < 2 && mode in listOf(QUIZ_MEANING, QUIZ_RECALL)) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cần ít nhất 2 từ để luyện trắc nghiệm", fontWeight = FontWeight.Bold)
                        Text("Thêm từ của bạn hoặc nhập CSV. Vocab không tự nạp bộ từ vựng.")
                    }
                }
            }
        } else if (word != null) {
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Điểm phiên: $score / $total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(
                            when (mode) {
                                QUIZ_CLOZE -> "Điền từ còn thiếu"
                                QUIZ_WEAK -> "Ôn từ từng quên"
                                QUIZ_RECALL -> "Chọn từ khớp với nghĩa"
                                QUIZ_TYPED -> "Tự nhớ và gõ từ tiếng Anh"
                                QUIZ_SENTENCE -> "Tự đặt câu với từ mục tiêu"
                                else -> "Chọn nghĩa đúng"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        if (mode == QUIZ_CLOZE) {
                            Text(QuizLogic.clozeSentence(word.example, word.term).orEmpty(), fontSize = 20.sp, lineHeight = 28.sp)
                            if (word.topic.isNotBlank()) Text("Chủ đề: ${word.topic}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = answerText,
                                onValueChange = { if (!answered) answerText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Từ tiếng Anh còn thiếu") },
                                singleLine = true
                            )
                            Button(onClick = { submitCloze() }, enabled = !answered && answerText.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                                Text("Kiểm tra đáp án")
                            }
                        } else if (mode == QUIZ_TYPED) {
                            Text(word.meaning, fontSize = 23.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                            if (word.topic.isNotBlank()) Text("Chủ đề: ${word.topic}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = answerText,
                                onValueChange = { if (!answered) answerText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Tự gõ từ/cụm từ tiếng Anh") },
                                singleLine = true
                            )
                            Button(onClick = { submitTypedAnswer() }, enabled = !answered && answerText.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                                Text("Kiểm tra đáp án")
                            }
                        } else if (mode == QUIZ_SENTENCE) {
                            Text(word.term, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            OfflineSpeakButton(text = word.term)
                            Text("Hãy tự viết một câu tiếng Anh tự nhiên có sử dụng từ mục tiêu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (word.meaning.isNotBlank()) Text("Nghĩa: ${word.meaning}")
                            OutlinedTextField(
                                value = answerText,
                                onValueChange = { if (!answered) answerText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Câu tiếng Anh của bạn") },
                                minLines = 3,
                                maxLines = 5
                            )
                            if (!answered) {
                                Button(onClick = { if (answerText.isNotBlank()) answered = true }, enabled = answerText.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                                    Text("Xem ví dụ và tự đánh giá")
                                }
                            } else if (lastCorrect == null) {
                                if (word.example.isNotBlank()) Text("Ví dụ tham khảo: ${word.example}")
                                if (word.collocations.isNotBlank()) Text("Collocations: ${word.collocations}")
                                Text("Tự đánh giá câu của bạn:", fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = { rateSentence(false) }, modifier = Modifier.weight(1f)) { Text("Cần luyện") }
                                    Button(onClick = { rateSentence(true) }, modifier = Modifier.weight(1f)) { Text("Dùng đúng") }
                                }
                            }
                        } else {
                            if (mode == QUIZ_RECALL) {
                                Text(word.meaning, fontSize = 23.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                                if (word.topic.isNotBlank()) Text("Chủ đề: ${word.topic}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text(word.term, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                OfflineSpeakButton(text = word.term)
                                word.ipa.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            options.forEach { option ->
                                val correct = option.id == word.id
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
                                            lastCorrect = correct
                                            answered = true
                                            total++
                                            if (correct) score++
                                            onAnswerRecorded(correct)
                                        }
                                    },
                                    color = fill,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(if (mode == QUIZ_RECALL) option.term else option.meaning, Modifier.padding(14.dp)) }
                            }
                        }
                        if (answered) {
                            Text(
                                when (lastCorrect) {
                                    true -> "Chính xác!"
                                    false -> "Chưa đúng — đáp án: ${word.term} — ${word.meaning}"
                                    else -> ""
                                },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (word.example.isNotBlank() && mode !in listOf(QUIZ_CLOZE, QUIZ_SENTENCE)) Text("Ví dụ: ${word.example}")
                            if (word.collocations.isNotBlank()) Text("Collocations: ${word.collocations}")
                            if (word.wordFamily.isNotBlank()) Text("Họ từ: ${word.wordFamily}")
                            if (mode == QUIZ_CLOZE && word.meaning.isNotBlank()) Text("Nghĩa: ${word.meaning}")
                        }
                        Button(
                            onClick = {
                                index = if (activeWords.isEmpty()) 0 else (index + 1) % activeWords.size
                                selectedId = null
                                answerText = ""
                                answered = false
                                lastCorrect = null
                            },
                            enabled = answered,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Câu tiếp theo") }
                        TextButton(
                            onClick = { score = 0; total = 0; index = 0; selectedId = null; answerText = ""; answered = false; lastCorrect = null },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("Đặt lại điểm") }
                    }
                }
            }
        }
    }
}

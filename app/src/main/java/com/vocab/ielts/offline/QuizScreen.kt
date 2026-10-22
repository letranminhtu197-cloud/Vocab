package com.vocab.ielts.offline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
private const val QUIZ_DUE = "Đến hạn"
private const val QUIZ_COLLOCATIONS = "Cụm từ"
private const val QUIZ_FAMILY = "Họ từ"

@Composable
fun QuizScreen(
    words: List<WordEntity>,
    modifier: Modifier = Modifier,
    sessionScore: Int,
    sessionTotal: Int,
    onAnswerRecorded: (WordEntity, Boolean) -> Unit,
    onSessionReset: () -> Unit,
    startWithDue: Boolean = false
) {
    var mode by remember(startWithDue) { mutableStateOf(if (startWithDue) QUIZ_DUE else QUIZ_MEANING) }
    var selectedTopic by remember { mutableStateOf("Tất cả chủ đề") }
    var index by remember { mutableIntStateOf(0) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var answerText by remember { mutableStateOf("") }
    var answered by remember { mutableStateOf(false) }
    var lastCorrect by remember { mutableStateOf<Boolean?>(null) }
    val topics = words.map { it.topic.trim() }.filter { it.isNotEmpty() }.distinct().sortedBy { it.lowercase() }
    val scopedWords = if (selectedTopic == "Tất cả chủ đề") words else words.filter {
        it.topic.trim().equals(selectedTopic, ignoreCase = true)
    }
    val weakWords = scopedWords.filter { it.lapseCount > 0 }
    val dueWords = scopedWords.filter { it.nextReviewAtMillis == 0L || it.nextReviewAtMillis <= System.currentTimeMillis() }
    val clozeWords = scopedWords.filter { QuizLogic.clozeSentence(it.example, it.term) != null }
    val collocationWords = scopedWords.filter { it.collocations.isNotBlank() }
    val familyWords = scopedWords.filter { it.wordFamily.isNotBlank() }
    val activeWords = when (mode) {
        QUIZ_CLOZE -> clozeWords
        QUIZ_WEAK -> weakWords
        QUIZ_DUE -> dueWords.sortedBy { if (it.nextReviewAtMillis == 0L) Long.MIN_VALUE else it.nextReviewAtMillis }
        QUIZ_COLLOCATIONS -> collocationWords
        QUIZ_FAMILY -> familyWords
        else -> scopedWords
    }
    val optionPool = when (mode) {
        QUIZ_COLLOCATIONS -> collocationWords
        QUIZ_FAMILY -> familyWords
        else -> scopedWords
    }
    val word = activeWords.getOrNull(if (activeWords.isEmpty()) 0 else index % activeWords.size)
    val options = remember(word?.id, optionPool.map { it.id }, mode) {
        if (word == null) emptyList() else (optionPool.filter { it.id != word.id }.shuffled().take(3) + word).shuffled()
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
            onAnswerRecorded(word, correct)
        }
    }
    fun submitCloze() = submitTypedAnswer()
    fun rateSentence(usedCorrectly: Boolean) {
        if (mode == QUIZ_SENTENCE && answered && lastCorrect == null && word != null) {
            lastCorrect = usedCorrectly
            onAnswerRecorded(word, usedCorrectly)
        }
    }

    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Luyện tập IELTS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text("Luyện nhớ nghĩa, tự gọi lại từ theo ngữ cảnh hoặc tập trung vào các từ từng quên. Điểm của phiên được giữ khi đổi chế độ.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                FilterChip(selected = mode == QUIZ_MEANING, onClick = { resetForMode(QUIZ_MEANING) }, label = { Text(QUIZ_MEANING) })
                FilterChip(selected = mode == QUIZ_RECALL, onClick = { resetForMode(QUIZ_RECALL) }, label = { Text(QUIZ_RECALL) })
                FilterChip(selected = mode == QUIZ_TYPED, onClick = { resetForMode(QUIZ_TYPED) }, label = { Text(QUIZ_TYPED) })
                FilterChip(selected = mode == QUIZ_CLOZE, onClick = { resetForMode(QUIZ_CLOZE) }, label = { Text(QUIZ_CLOZE) })
                FilterChip(selected = mode == QUIZ_SENTENCE, onClick = { resetForMode(QUIZ_SENTENCE) }, label = { Text(QUIZ_SENTENCE) })
                FilterChip(selected = mode == QUIZ_WEAK, onClick = { resetForMode(QUIZ_WEAK) }, label = { Text(QUIZ_WEAK) })
                FilterChip(selected = mode == QUIZ_DUE, onClick = { resetForMode(QUIZ_DUE) }, label = { Text("$QUIZ_DUE (${dueWords.size})") })
                FilterChip(selected = mode == QUIZ_COLLOCATIONS, onClick = { resetForMode(QUIZ_COLLOCATIONS) }, label = { Text("$QUIZ_COLLOCATIONS (${collocationWords.size})") })
                FilterChip(selected = mode == QUIZ_FAMILY, onClick = { resetForMode(QUIZ_FAMILY) }, label = { Text("$QUIZ_FAMILY (${familyWords.size})") })
            }
        }
        if (topics.isNotEmpty()) {
            item {
                Text("Khu vực IELTS · Chủ đề luyện tập", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    (listOf("Tất cả chủ đề") + topics).forEach { topic ->
                        FilterChip(
                            selected = selectedTopic.equals(topic, ignoreCase = true),
                            onClick = {
                                selectedTopic = topic
                                index = 0
                                selectedId = null
                                answerText = ""
                                answered = false
                                lastCorrect = null
                            },
                            label = { Text(topic) }
                        )
                    }
                }
                Text(
                    if (selectedTopic == "Tất cả chủ đề") "Luyện trên toàn bộ sổ từ" else "Đang luyện chủ đề: $selectedTopic",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (mode == QUIZ_DUE && dueWords.isEmpty() && words.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Bạn đã ôn hết từ đến hạn", fontWeight = FontWeight.Bold)
                        Text("Chế độ này chỉ lấy từ chưa có lịch ôn hoặc đã đến hạn. Từ sắp đến hạn sẽ xuất hiện khi tới thời điểm ôn.")
                    }
                }
            }
        } else if (mode == QUIZ_COLLOCATIONS && collocationWords.size < 2 && words.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chưa đủ dữ liệu collocations", fontWeight = FontWeight.Bold)
                        Text("Cần ít nhất hai từ trong phạm vi đã chọn có trường Collocations. Bạn có thể thêm cụm từ đi kèm trong Sổ từ hoặc CSV.")
                    }
                }
            }
        } else if (mode == QUIZ_FAMILY && familyWords.size < 2 && words.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chưa đủ dữ liệu họ từ", fontWeight = FontWeight.Bold)
                        Text("Cần ít nhất hai từ trong phạm vi đã chọn có trường Word family. Thêm họ từ vào các mục từ vựng của bạn để luyện phân biệt.")
                    }
                }
            }
        } else if (mode == QUIZ_WEAK && weakWords.isEmpty()) {
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
        } else if (words.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tạo bộ từ của riêng bạn", fontWeight = FontWeight.Bold)
                        Text("Thêm từ trong Sổ từ hoặc nhập CSV để bắt đầu luyện tập. Vocab không tự cài sẵn danh sách từ.")
                    }
                }
            }
        } else if (scopedWords.size < 2 && mode in listOf(QUIZ_MEANING, QUIZ_RECALL)) {
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
                        Text("Điểm phiên: $sessionScore / $sessionTotal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                                if (mode in listOf(QUIZ_COLLOCATIONS, QUIZ_FAMILY) && word.meaning.isNotBlank()) Text("Nghĩa: ${word.meaning}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                            onAnswerRecorded(word, correct)
                                        }
                                    },
                                    color = fill,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        when (mode) {
                                            QUIZ_RECALL -> option.term
                                            QUIZ_COLLOCATIONS -> option.collocations
                                            QUIZ_FAMILY -> option.wordFamily
                                            else -> option.meaning
                                        },
                                        Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                        if (answered && (mode != QUIZ_SENTENCE || lastCorrect != null)) {
                            Text(
                                when {
                                    mode == QUIZ_SENTENCE && lastCorrect == true -> "Đã ghi nhận: bạn tự đánh giá câu dùng từ đúng."
                                    mode == QUIZ_SENTENCE && lastCorrect == false -> "Đã ghi nhận: câu này cần luyện thêm."
                                    lastCorrect == true -> "Chính xác!"
                                    lastCorrect == false -> "Chưa đúng — đáp án: ${word.term} — ${word.meaning}"
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
                            enabled = answered && (mode != QUIZ_SENTENCE || lastCorrect != null),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Câu tiếp theo") }
                        TextButton(
                            onClick = { onSessionReset(); index = 0; selectedId = null; answerText = ""; answered = false; lastCorrect = null },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("Đặt lại điểm") }
                    }
                }
            }
        }
    }
}

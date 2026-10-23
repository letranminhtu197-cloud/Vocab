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
private const val QUIZ_LISTENING = "Nghe & gõ"
private const val QUIZ_SPEAKING = "Nói từ"
private const val QUIZ_SENTENCE = "Viết câu"
private const val QUIZ_CLOZE = "Điền từ"
private const val QUIZ_WEAK = "Từ yếu"
private const val QUIZ_DUE = "Đến hạn"
private const val QUIZ_COLLOCATIONS = "Cụm từ"
private const val QUIZ_FAMILY = "Họ từ"
private const val QUIZ_SYNONYMS = "Từ đồng nghĩa"
@Composable
private fun QuizQuickStat(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 20.sp)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@Composable
fun QuizScreen(
    words: List<WordEntity>,
    modifier: Modifier = Modifier,
    sessionScore: Int,
    sessionTotal: Int,
    onAnswerRecorded: (WordEntity, Boolean, Boolean) -> Unit,
    onSessionReset: () -> Unit,
    startWithDue: Boolean = false
) {
    var isSrsReview by remember(startWithDue) { mutableStateOf(startWithDue) }
    var showMatchGame by remember { mutableStateOf(false) }
    var mode by remember(startWithDue) { mutableStateOf(if (startWithDue) QUIZ_DUE else QUIZ_MEANING) }
    var selectedTopic by remember { mutableStateOf("Tất cả chủ đề") }
    var index by remember { mutableIntStateOf(0) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var answerText by remember { mutableStateOf("") }
    var answered by remember { mutableStateOf(false) }
    var lastCorrect by remember { mutableStateOf<Boolean?>(null) }
    var showSummary by remember { mutableStateOf(false) }
    var retryWrongOnly by remember { mutableStateOf(false) }
    var sessionAttempts by remember { mutableIntStateOf(0) }
    var sessionCorrect by remember { mutableIntStateOf(0) }
    val wrongWordIds = remember { mutableStateListOf<Long>() }
    // Freeze the replay deck for the duration of a retry session. The unresolved set can shrink
    // after a correct answer, but changing the rendered deck mid-question would skip items.
    val retryDeckIds = remember { mutableStateListOf<Long>() }
    val modeAttempts = remember { mutableStateMapOf<String, Int>() }
    val modeCorrect = remember { mutableStateMapOf<String, Int>() }
    val topics = words.map { it.topic.trim() }.filter { it.isNotEmpty() }.distinct().sortedBy { it.lowercase() }
    val topicScopedWords = if (selectedTopic == "Tất cả chủ đề") words else words.filter {
        it.topic.trim().equals(selectedTopic, ignoreCase = true)
    }
    val nowForQueue = System.currentTimeMillis()
    val scopedWords = if (isSrsReview) topicScopedWords.filter {
        !it.excludedFromReview && (it.nextReviewAtMillis == 0L || it.nextReviewAtMillis <= nowForQueue)
    } else topicScopedWords
    val weakWords = scopedWords.filter { it.lapseCount > 0 }
    val dueWords = scopedWords.filter { it.nextReviewAtMillis == 0L || it.nextReviewAtMillis <= System.currentTimeMillis() }
    val clozeWords = scopedWords.filter { ActiveLearningLogic.validatedCloze(it.example, it.term) != null }
    val collocationWords = scopedWords.filter { it.collocations.isNotBlank() }
    val familyWords = scopedWords.filter { it.wordFamily.isNotBlank() }
    val synonymWords = scopedWords.filter { it.synonyms.isNotBlank() }
    val modeWords = when (mode) {
        QUIZ_CLOZE -> clozeWords
        QUIZ_WEAK -> weakWords.sortedWith(compareByDescending<WordEntity> { it.lapseCount }.thenBy { it.nextReviewAtMillis })
        QUIZ_DUE -> dueWords.sortedBy { if (it.nextReviewAtMillis == 0L) Long.MIN_VALUE else it.nextReviewAtMillis }
        QUIZ_COLLOCATIONS -> collocationWords
        QUIZ_FAMILY -> familyWords
        QUIZ_SYNONYMS -> synonymWords
        else -> scopedWords
    }
    val activeWords = if (retryWrongOnly) QuizLogic.retryWords(scopedWords, retryDeckIds.toList()) else modeWords
    val optionPool = when (mode) {
        QUIZ_COLLOCATIONS -> collocationWords
        QUIZ_FAMILY -> familyWords
        else -> scopedWords
    }
    val word = activeWords.getOrNull(if (activeWords.isEmpty()) 0 else index % activeWords.size)
    val options = remember(word?.id, optionPool.map { it.id }, optionPool.map { QuizLogic.choiceLabel(it, mode) }, mode) {
        if (word == null) emptyList() else QuizLogic.multipleChoiceOptions(word, optionPool, mode)
    }

    fun resetQuestionState() {
        index = 0
        selectedId = null
        answerText = ""
        answered = false
        lastCorrect = null
    }
    fun resetForMode(nextMode: String) {
        mode = nextMode
        retryWrongOnly = false
        retryDeckIds.clear()
        showSummary = false
        resetQuestionState()
    }
    fun recordAnswer(wordToRecord: WordEntity, correct: Boolean) {
        sessionAttempts += 1
        modeAttempts[mode] = (modeAttempts[mode] ?: 0) + 1
        if (correct) {
            sessionCorrect += 1
            modeCorrect[mode] = (modeCorrect[mode] ?: 0) + 1
            if (retryWrongOnly) wrongWordIds.remove(wordToRecord.id)
        } else if (wordToRecord.id !in wrongWordIds) {
            wrongWordIds.add(wordToRecord.id)
        }
        onAnswerRecorded(wordToRecord, correct, isSrsReview)
    }
    fun submitTypedAnswer() {
        if (!answered && word != null && answerText.isNotBlank()) {
            val correct = QuizLogic.isCorrectAnswer(answerText, word.term)
            lastCorrect = correct
            answered = true
            recordAnswer(word, correct)
        }
    }
    fun submitCloze() = submitTypedAnswer()
    fun rateSentence(usedCorrectly: Boolean) {
        if (mode == QUIZ_SENTENCE && answered && lastCorrect == null && word != null) {
            lastCorrect = usedCorrectly
            recordAnswer(word, usedCorrectly)
        }
    }

    LazyColumn(modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(18.dp)) {
                        Text("🧩", Modifier.padding(13.dp), fontSize = 29.sp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Luyện tập IELTS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Chọn cách luyện phù hợp với bạn", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Text("⚡", fontSize = 26.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuizQuickStat("✅", "Đúng", "$sessionScore", Modifier.weight(1f))
                QuizQuickStat("🎯", "Mục tiêu", "$sessionTotal", Modifier.weight(1f))
                QuizQuickStat("🧠", "Cần ôn", "${wrongWordIds.size}", Modifier.weight(1f))
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chế độ học", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isSrsReview,
                            onClick = {
                                if (!isSrsReview) {
                                    isSrsReview = true
                                    mode = QUIZ_DUE
                                    selectedTopic = "Tất cả chủ đề"
                                    retryWrongOnly = false
                                    retryDeckIds.clear()
                                    showSummary = false
                                    resetQuestionState()
                                }
                            },
                            label = { Text("Ôn SRS · chỉ từ đến hạn") }
                        )
                        FilterChip(
                            selected = !isSrsReview,
                            onClick = {
                                if (isSrsReview) {
                                    isSrsReview = false
                                    mode = QUIZ_MEANING
                                    retryWrongOnly = false
                                    retryDeckIds.clear()
                                    showSummary = false
                                    resetQuestionState()
                                }
                            },
                            label = { Text("Luyện tập tự do") }
                        )
                    }
                    Text(
                        if (isSrsReview) "Câu trả lời đã nộp sẽ cập nhật lịch SRS đúng một lần. Câu chưa trả lời không bị tính sai."
                        else "Luyện tập tự do chỉ ghi thống kê và phản hồi; không đổi lịch ôn, cấp SRS hoặc chuỗi ôn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (showSummary) {
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Tổng kết phiên học", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Đã trả lời: $sessionAttempts câu")
                        Text("Đúng: $sessionCorrect câu")
                        Text("Chưa đúng: ${(sessionAttempts - sessionCorrect).coerceAtLeast(0)} câu")
                        val accuracy = if (sessionAttempts == 0) 0 else (sessionCorrect * 100 / sessionAttempts)
                        Text("Độ chính xác: $accuracy%", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("Số từ cần ôn lại: ${wrongWordIds.size}")
                        if (modeAttempts.isNotEmpty()) {
                            Text("Kết quả theo chế độ", fontWeight = FontWeight.SemiBold)
                            modeAttempts.forEach { (modeName, attempts) ->
                                val correctInMode = modeCorrect[modeName] ?: 0
                                val modeAccuracy = if (attempts == 0) 0 else correctInMode * 100 / attempts
                                Text("$modeName: $correctInMode/$attempts đúng ($modeAccuracy%)")
                            }
                        }
                        if (wrongWordIds.isNotEmpty()) {
                            Text("Từ cần ôn", fontWeight = FontWeight.SemiBold)
                            words.filter { it.id in wrongWordIds }.forEach { wrong ->
                                Text("• ${wrong.term} — ${wrong.meaning}")
                            }
                            Button(
                                onClick = {
                                    // Replay every wrong word in a stable, answerable mode. The due
                                    // queue changes as SRS timestamps are updated after each answer.
                                    selectedTopic = "Tất cả chủ đề"
                                    mode = QUIZ_TYPED
                                    retryDeckIds.clear()
                                    retryDeckIds.addAll(wrongWordIds)
                                    retryWrongOnly = true
                                    showSummary = false
                                    resetQuestionState()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Luyện lại ${wrongWordIds.size} từ sai") }
                        }
                        OutlinedButton(
                            onClick = {
                                sessionAttempts = 0
                                sessionCorrect = 0
                                wrongWordIds.clear()
                                retryDeckIds.clear()
                                modeAttempts.clear()
                                modeCorrect.clear()
                                retryWrongOnly = false
                                retryDeckIds.clear()
                                showSummary = false
                                resetQuestionState()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Bắt đầu phiên mới") }
                    }
                }
            }
        } else if (sessionAttempts > 0) {
            item {
                TextButton(onClick = { showSummary = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Xem tổng kết phiên")
                }
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                FilterChip(selected = mode == QUIZ_MEANING, onClick = { resetForMode(QUIZ_MEANING) }, label = { Text(QUIZ_MEANING) })
                FilterChip(selected = false, onClick = { showMatchGame = true }, label = { Text("🎮 Ghép cặp") })
                FilterChip(selected = mode == QUIZ_RECALL, onClick = { resetForMode(QUIZ_RECALL) }, label = { Text(QUIZ_RECALL) })
                FilterChip(selected = mode == QUIZ_TYPED, onClick = { resetForMode(QUIZ_TYPED) }, label = { Text(QUIZ_TYPED) })
                FilterChip(selected = mode == QUIZ_LISTENING, onClick = { resetForMode(QUIZ_LISTENING) }, label = { Text(QUIZ_LISTENING) })
                FilterChip(selected = mode == QUIZ_SPEAKING, onClick = { resetForMode(QUIZ_SPEAKING) }, label = { Text(QUIZ_SPEAKING) })
                FilterChip(selected = mode == QUIZ_CLOZE, onClick = { resetForMode(QUIZ_CLOZE) }, label = { Text(QUIZ_CLOZE) })
                FilterChip(selected = mode == QUIZ_SENTENCE, onClick = { resetForMode(QUIZ_SENTENCE) }, label = { Text(QUIZ_SENTENCE) })
                FilterChip(selected = mode == QUIZ_WEAK, onClick = { resetForMode(QUIZ_WEAK) }, label = { Text(QUIZ_WEAK) })
                FilterChip(selected = mode == QUIZ_DUE, onClick = { resetForMode(QUIZ_DUE) }, label = { Text("$QUIZ_DUE (${dueWords.size})") })
                FilterChip(selected = mode == QUIZ_COLLOCATIONS, onClick = { resetForMode(QUIZ_COLLOCATIONS) }, label = { Text("$QUIZ_COLLOCATIONS (${collocationWords.size})") })
                FilterChip(selected = mode == QUIZ_FAMILY, onClick = { resetForMode(QUIZ_FAMILY) }, label = { Text("$QUIZ_FAMILY (${familyWords.size})") })
                FilterChip(selected = mode == QUIZ_SYNONYMS, onClick = { resetForMode(QUIZ_SYNONYMS) }, label = { Text("$QUIZ_SYNONYMS (${synonymWords.size})") })
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
                                retryWrongOnly = false
                                retryDeckIds.clear()
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
        } else if (mode == QUIZ_SYNONYMS && synonymWords.size < 2 && words.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chưa đủ dữ liệu từ đồng nghĩa", fontWeight = FontWeight.Bold)
                        Text("Cần ít nhất hai mục từ có trường Synonyms khác nhau. Thêm từ đồng nghĩa trong phần sửa từ hoặc nhập qua CSV.")
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
        } else if (word != null && !showSummary) {
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Điểm phiên: $sessionScore / $sessionTotal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Câu ${index + 1} trong ${activeWords.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { (index + 1).toFloat() / activeWords.size.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            when (mode) {
                                QUIZ_CLOZE -> "Điền từ còn thiếu"
                                QUIZ_WEAK -> "Ôn từ từng quên"
                                QUIZ_RECALL -> "Chọn từ khớp với nghĩa"
                                QUIZ_TYPED -> "Tự nhớ và gõ từ tiếng Anh"
                                QUIZ_LISTENING -> "Nghe phát âm ngoại tuyến rồi gõ từ"
                                QUIZ_SPEAKING -> "Nói từ tiếng Anh bằng nhận diện ngoại tuyến"
                                QUIZ_SENTENCE -> "Tự đặt câu với từ mục tiêu"
                                QUIZ_SYNONYMS -> "Chọn nhóm từ đồng nghĩa phù hợp"
                                else -> "Chọn nghĩa đúng"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        if (mode == QUIZ_CLOZE) {
                            Text(ActiveLearningLogic.validatedCloze(word.example, word.term).orEmpty(), fontSize = 20.sp, lineHeight = 28.sp)
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
                        } else if (mode == QUIZ_LISTENING) {
                            Text(
                                "Nghe từ tiếng Anh rồi nhập chính tả bạn nghe được. Đáp án không hiển thị trước khi kiểm tra.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OfflineSpeakButton(text = word.term, label = "Phát lại từ (offline)")
                            if (word.topic.isNotBlank()) {
                                Text("Chủ đề: ${word.topic}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedTextField(
                                value = answerText,
                                onValueChange = { if (!answered) answerText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Nhập từ bạn nghe được") },
                                singleLine = true
                            )
                            Button(
                                onClick = { submitTypedAnswer() },
                                enabled = !answered && answerText.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Kiểm tra chính tả") }
                        } else if (mode == QUIZ_SPEAKING) {
                            Text(word.term, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            if (word.ipa.isNotBlank()) Text(word.ipa, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (word.meaning.isNotBlank()) Text("Nghĩa: ${word.meaning}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OfflineSpeakButton(text = word.term, label = "Nghe phát âm mẫu (offline)")
                            Text(
                                "Đọc từ thành tiếng. Vocab chỉ kiểm tra từ mà bộ nhận diện nghe được, không chấm giọng hay trọng âm.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OfflineSpeechInput(
                                enabled = !answered,
                                onRecognized = { recognized ->
                                    if (!answered) {
                                        answerText = recognized
                                        val correct = QuizLogic.isCorrectAnswer(recognized, word.term)
                                        lastCorrect = correct
                                        answered = true
                                        recordAnswer(word, correct)
                                    }
                                }
                            )
                            if (answerText.isNotBlank()) Text("Bản nhận diện: $answerText", style = MaterialTheme.typography.bodyMedium)
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
                                if (mode in listOf(QUIZ_COLLOCATIONS, QUIZ_FAMILY, QUIZ_SYNONYMS) && word.meaning.isNotBlank()) Text("Nghĩa: ${word.meaning}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (options.size < 2) {
                                Text(
                                    "Chưa đủ đáp án khác nhau để tạo câu trắc nghiệm. Hãy bổ sung nghĩa, cụm từ hoặc họ từ khác nhau cho các mục trong phạm vi này.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = {
                                        if (index + 1 >= activeWords.size) {
                                            showSummary = true
                                        } else {
                                            index += 1
                                        }
                                        selectedId = null
                                        answerText = ""
                                        answered = false
                                        lastCorrect = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (index + 1 >= activeWords.size) "Bỏ qua và kết thúc phiên" else "Bỏ qua câu không thể trả lời")
                                }
                            }
                            options.forEach { option ->
                                val correct = option.id == word.id
                                val chosen = option.id == selectedId
                                val fill = when {
                                    answered && correct -> MaterialTheme.colorScheme.primaryContainer
                                    answered && chosen -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                Surface(
                                    onClick = {
                                        if (!answered && options.size >= 2) {
                                            selectedId = option.id
                                            lastCorrect = correct
                                            answered = true
                                            recordAnswer(word, correct)
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
                                            QUIZ_SYNONYMS -> option.synonyms
                                            else -> option.meaning
                                        },
                                        Modifier.padding(14.dp),
                                        color = when {
                                            answered && correct -> MaterialTheme.colorScheme.onPrimaryContainer
                                            answered && chosen -> MaterialTheme.colorScheme.onErrorContainer
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
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
                            HorizontalDivider()
                            Text("Ghi nhớ từ vựng", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(word.term, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (word.ipa.isNotBlank()) Text("Phiên âm: ${word.ipa}")
                            if (word.partOfSpeech.isNotBlank()) Text("Từ loại: ${word.partOfSpeech}")
                            if (word.meaning.isNotBlank()) Text("Nghĩa: ${word.meaning}")
                            if (word.topic.isNotBlank()) Text("Chủ đề IELTS: ${word.topic}")
                            if (word.example.isNotBlank()) Text("Ví dụ: ${word.example}")
                            if (word.collocations.isNotBlank()) Text("Collocations: ${word.collocations}")
                            if (word.synonyms.isNotBlank()) Text("Từ đồng nghĩa: ${word.synonyms}")
                            if (word.wordFamily.isNotBlank()) Text("Họ từ: ${word.wordFamily}")
                            if (word.notes.isNotBlank()) Text("Ghi chú: ${word.notes}")
                            if (word.tags.isNotBlank()) Text("Thẻ: ${word.tags}")
                        }
                        Button(
                            onClick = {
                                if (index + 1 >= activeWords.size) {
                                    showSummary = true
                                } else {
                                    index += 1
                                }
                                selectedId = null
                                answerText = ""
                                answered = false
                                lastCorrect = null
                            },
                            enabled = answered && (mode != QUIZ_SENTENCE || lastCorrect != null),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (index + 1 >= activeWords.size) "Hoàn thành phiên" else "Câu tiếp theo") }
                        TextButton(
                            onClick = {
                                onSessionReset()
                                sessionAttempts = 0
                                sessionCorrect = 0
                                wrongWordIds.clear()
                                modeAttempts.clear()
                                modeCorrect.clear()
                                retryWrongOnly = false
                                showSummary = false
                                resetQuestionState()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("Đặt lại điểm") }
                    }
                }
            }
        }
    }
    if (showMatchGame) MatchGameDialog(words = words, onDismiss = { showMatchGame = false })
}

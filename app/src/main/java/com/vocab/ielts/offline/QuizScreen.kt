package com.vocab.ielts.offline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

private const val MODE_NEW = "Flashcard · từ mới"
private const val MODE_MC = "Trắc nghiệm"
private const val MODE_LISTEN = "Nghe và gõ"
private const val MODE_CLOZE = "Điền vào ngữ cảnh"

private fun defaultPracticeMode(words: List<WordEntity>): String {
    val pool = words.filterNot { it.excludedFromReview }
    if (pool.any { it.isNew }) return MODE_NEW
    val hasMultipleChoice = pool.any { target ->
        val options = QuizLogic.multipleChoiceOptions(target, pool, "Chọn nghĩa")
        ActiveLearningLogic.hasFourValidChoices(options.map { it.meaning }, target.meaning)
    }
    if (hasMultipleChoice) return MODE_MC
    if (pool.any { ActiveLearningLogic.validatedCloze(it.example, it.term) != null }) return MODE_CLOZE
    if (pool.any { it.term.isNotBlank() }) return MODE_LISTEN
    return MODE_NEW
}

@Composable
fun QuizScreen(
    words: List<WordEntity>,
    modifier: Modifier = Modifier,
    sessionScore: Int,
    sessionTotal: Int,
    onAnswerRecorded: (WordEntity, Boolean, Boolean, String, String, ActiveExerciseType) -> Unit,
    onAudioReplay: () -> Unit = {},
    onSessionStarted: (String, Long, Boolean) -> Unit = { _, _, _ -> },
    onSessionCompleted: (String, Long, Long, Int, Int, Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onNewWordIntroduced: (WordEntity) -> Unit,
    onSessionReset: () -> Unit,
    startWithDue: Boolean = false
) {
    var isSrsReview by rememberSaveable(startWithDue) { mutableStateOf(startWithDue) }
    var mode by rememberSaveable(startWithDue, words.map { it.id }) { mutableStateOf(if (startWithDue) MODE_MC else defaultPracticeMode(words)) }
    var index by rememberSaveable(startWithDue) { mutableIntStateOf(0) }
    var deckGeneration by rememberSaveable(startWithDue) { mutableIntStateOf(0) }
    var answerText by rememberSaveable(startWithDue) { mutableStateOf("") }
    var selectedOptionId by rememberSaveable(startWithDue) { mutableStateOf<Long?>(null) }
    var answered by rememberSaveable(startWithDue) { mutableStateOf(false) }
    var revealed by rememberSaveable(startWithDue) { mutableStateOf(false) }
    var correct by rememberSaveable(startWithDue) { mutableStateOf<Boolean?>(null) }
    var showMatch by remember { mutableStateOf(false) }
    var attempts by rememberSaveable(startWithDue) { mutableIntStateOf(0) }
    var correctCount by rememberSaveable(startWithDue) { mutableIntStateOf(0) }
    var lastType by rememberSaveable(startWithDue) { mutableStateOf<ActiveExerciseType?>(null) }
    var overrideType by rememberSaveable(startWithDue) { mutableStateOf<ActiveExerciseType?>(null) }
    var audioReady by rememberSaveable(startWithDue) { mutableStateOf(false) }
    var audioStatus by rememberSaveable(startWithDue) { mutableStateOf("Đang kiểm tra giọng đọc ngoại tuyến…") }
    var sessionId by rememberSaveable(startWithDue) { mutableStateOf(UUID.randomUUID().toString()) }
    var sessionStartedAtMillis by rememberSaveable(startWithDue) { mutableStateOf(System.currentTimeMillis()) }
    var sessionCompleted by rememberSaveable(startWithDue) { mutableStateOf(false) }
    val exerciseCounts = remember(startWithDue) { mutableStateMapOf<ActiveExerciseType, Int>() }
    val exerciseErrors = remember(startWithDue) { mutableStateMapOf<ActiveExerciseType, Int>() }
    val exerciseLastUsed = remember(startWithDue) { mutableStateMapOf<ActiveExerciseType, Long>() }

    // Freeze the SRS deck for this session: updating a due date must not skip the next card.
    val srsDeckIds = remember(isSrsReview, words.map { it.id }, deckGeneration) {
        val now = System.currentTimeMillis()
        words.filter { !it.isNew && !it.excludedFromReview && (it.nextReviewAtMillis == 0L || it.nextReviewAtMillis <= now) }
            .map { it.id }.toSet()
    }
    val srsDeck = words.filter { it.id in srsDeckIds && !it.isNew && !it.excludedFromReview }
    val newWordIds = remember(words.map { it.id }, deckGeneration) {
        words.filter { it.isNew && !it.excludedFromReview }.map { it.id }.toSet()
    }
    val newWords = words.filter { it.id in newWordIds && !it.excludedFromReview }
    val choicePool = words.filter { !it.excludedFromReview }
    val practiceWords = if (isSrsReview) srsDeck else when (mode) {
        MODE_NEW -> newWords
        MODE_LISTEN -> choicePool.filter { it.term.isNotBlank() }
        MODE_CLOZE -> choicePool.filter { ActiveLearningLogic.validatedCloze(it.example, it.term) != null }
        MODE_MC -> choicePool.filter { candidate ->
            val candidateOptions = QuizLogic.multipleChoiceOptions(candidate, choicePool, "Chọn nghĩa")
            ActiveLearningLogic.hasFourValidChoices(candidateOptions.map { it.meaning }, candidate.meaning)
        }
        else -> choicePool
    }
    val targetDeck = if (isSrsReview) srsDeck else practiceWords
    val word = targetDeck.getOrNull(index)
    val options = remember(word?.id, choicePool.map { it.id to it.meaning }, mode) {
        if (word == null) emptyList() else QuizLogic.multipleChoiceOptions(word, choicePool, "Chọn nghĩa")
    }
    val clozeText = word?.let { ActiveLearningLogic.validatedCloze(it.example, it.term) }
    val mcAvailable = word != null && ActiveLearningLogic.hasFourValidChoices(
        options.map { it.meaning }, word.meaning
    )
    val clozeAvailable = word != null && clozeText != null
    val listenAvailable = word?.term?.isNotBlank() == true

    fun availableTypes(target: WordEntity?): List<ActiveExerciseType> {
        if (target == null) return emptyList()
        val pool = words.filterNot { it.excludedFromReview }
        val validMc = QuizLogic.multipleChoiceOptions(target, pool, "Chọn nghĩa")
            .let { list -> ActiveLearningLogic.hasFourValidChoices(list.map { it.meaning }, target.meaning) }
        return buildList {
            if (validMc) add(ActiveExerciseType.MULTIPLE_CHOICE)
            if (target.term.isNotBlank()) add(ActiveExerciseType.LISTEN_AND_TYPE)
            if (ActiveLearningLogic.validatedCloze(target.example, target.term) != null) add(ActiveExerciseType.CLOZE)
        }
    }

    fun pickSrsExercise(target: WordEntity?): ActiveExerciseType? {
        val available = availableTypes(target)
        val evidence = available.map { type ->
            ExerciseEvidence(
                type = type,
                valid = true,
                attempts = exerciseCounts[type] ?: 0,
                errors = exerciseErrors[type] ?: 0,
                lastUsedAtMillis = exerciseLastUsed[type] ?: 0L
            )
        }
        return ActiveLearningLogic.chooseNext(evidence, System.currentTimeMillis(), exerciseCounts.toMap())
            ?: available.firstOrNull()
    }

    LaunchedEffect(sessionId) {
        onSessionStarted(sessionId, sessionStartedAtMillis, isSrsReview)
    }

    LaunchedEffect(sessionId, index, targetDeck.size, mode) {
        if (targetDeck.isNotEmpty() && index >= targetDeck.size && !sessionCompleted) {
            sessionCompleted = true
            val completedItems = if (!isSrsReview && mode == MODE_NEW) index else attempts
            val endedAt = System.currentTimeMillis()
            onSessionCompleted(sessionId, sessionStartedAtMillis, endedAt, completedItems, completedItems, isSrsReview)
        }
    }

    fun resetAnswerState() {
        answerText = ""
        selectedOptionId = null
        answered = false
        revealed = false
        correct = null
        lastType = null
        overrideType = null
        audioReady = false
        audioStatus = "Đang kiểm tra giọng đọc ngoại tuyến…"
    }

    fun changeMode(nextMode: String, srs: Boolean = false) {
        isSrsReview = srs
        mode = nextMode
        deckGeneration++
        index = 0
        attempts = 0
        correctCount = 0
        resetAnswerState()
        sessionId = UUID.randomUUID().toString()
        sessionStartedAtMillis = System.currentTimeMillis()
        sessionCompleted = false
        onSessionReset()
    }

    fun submitAnswer(target: WordEntity, isCorrect: Boolean, type: ActiveExerciseType) {
        if (answered) return
        answered = true
        correct = isCorrect
        attempts++
        if (isCorrect) correctCount++
        exerciseCounts[type] = (exerciseCounts[type] ?: 0) + 1
        if (!isCorrect) exerciseErrors[type] = (exerciseErrors[type] ?: 0) + 1
        exerciseLastUsed[type] = System.currentTimeMillis()
        lastType = type
        onAnswerRecorded(target, isCorrect, isSrsReview, UUID.randomUUID().toString(), sessionId, type)
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🧠", fontSize = 32.sp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("IELTS Vocabulary Lab", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text(if (isSrsReview) "Ôn chủ động · chỉ từ đến hạn" else "Học từ mới và luyện tập tự do")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Đúng", style = MaterialTheme.typography.labelMedium)
                    Text("$sessionScore", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Đã trả lời", style = MaterialTheme.typography.labelMedium)
                    Text("$sessionTotal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Phiên này", style = MaterialTheme.typography.labelMedium)
                    Text("$attempts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Chế độ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isSrsReview,
                        onClick = { if (!isSrsReview) changeMode(MODE_MC, true) },
                        label = { Text("Ôn SRS") }
                    )
                    FilterChip(
                        selected = !isSrsReview,
                        onClick = { if (isSrsReview) changeMode(defaultPracticeMode(words), false) },
                        label = { Text("Luyện tự do") }
                    )
                }
                Text(
                    if (isSrsReview) "Chỉ từ đã được giới thiệu và đến hạn mới vào hàng đợi SRS. Mỗi câu được ghi nhận một lần."
                    else "Flashcard chỉ dùng để làm quen từ mới; luyện tập tự do không thay đổi lịch SRS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!isSrsReview) {
            Text("Chọn dạng bài", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == MODE_NEW, onClick = { changeMode(MODE_NEW) }, label = { Text("Từ mới") })
                FilterChip(selected = mode == MODE_MC, onClick = { changeMode(MODE_MC) }, label = { Text("Trắc nghiệm") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == MODE_LISTEN, onClick = { changeMode(MODE_LISTEN) }, label = { Text("Nghe & gõ") })
                FilterChip(selected = mode == MODE_CLOZE, onClick = { changeMode(MODE_CLOZE) }, label = { Text("Điền ngữ cảnh") })
                FilterChip(selected = false, onClick = { showMatch = true }, label = { Text("Ghép cặp") })
            }
        } else {
            Text("Dạng bài được chọn thích ứng theo dữ liệu có sẵn", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        if (targetDeck.isEmpty()) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when {
                            words.isEmpty() -> "Bắt đầu bằng bộ từ của bạn"
                            isSrsReview -> "Không có từ đến hạn"
                            mode == MODE_NEW -> "Không còn từ mới"
                            mode == MODE_CLOZE -> "Chưa có ví dụ phù hợp"
                            else -> "Chưa có từ để luyện"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            words.isEmpty() -> "Nhập từ vựng qua Sổ từ hoặc file CSV/JSON/TXT. Ứng dụng không tự thêm từ."
                            isSrsReview -> "Bạn đã ôn hết từ đến hạn. Từ sắp đến hạn sẽ không xuất hiện sớm."
                            mode == MODE_NEW -> "Các mục đã có lịch SRS không được coi là từ mới."
                            mode == MODE_CLOZE -> "Hãy nhập câu ví dụ có chứa từ mục tiêu đúng một lần."
                            else -> "Hãy nhập dữ liệu từ vựng trước khi luyện."
                        }
                    )
                }
            }
        } else if (word == null) {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (isSrsReview) "Đã hoàn thành hàng đợi ôn SRS" else "Đã hoàn thành lượt học",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isSrsReview) "Bạn đã đi qua toàn bộ các từ đến hạn được chốt khi bắt đầu phiên. Từ đã ôn sẽ không bị lặp lại trong phiên này."
                        else "Bạn đã đi hết các từ trong dạng bài hiện tại."
                    )
                    Button(
                        onClick = { changeMode(mode, isSrsReview) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isSrsReview) "Tạo hàng đợi SRS mới" else "Làm lại dạng bài") }
                    if (isSrsReview) {
                        OutlinedButton(
                            onClick = { changeMode(MODE_NEW, false) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Chuyển sang học từ mới") }
                    }
                }
            }
        } else if (word != null) {
            val activeType = if (!isSrsReview && mode == MODE_NEW) null else overrideType ?: if (isSrsReview) {
                if (answered) lastType ?: pickSrsExercise(word) ?: ActiveExerciseType.LISTEN_AND_TYPE
                else pickSrsExercise(word) ?: ActiveExerciseType.LISTEN_AND_TYPE
            } else when (mode) {
                MODE_LISTEN -> ActiveExerciseType.LISTEN_AND_TYPE
                MODE_CLOZE -> ActiveExerciseType.CLOZE
                MODE_MC -> ActiveExerciseType.MULTIPLE_CHOICE
                else -> null
            }

            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                            Text("Câu ${index + 1}/${targetDeck.size}", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.weight(1f))
                        if (isSrsReview) Text("SRS", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Đã trả lời: $attempts · Còn lại: ${(targetDeck.size - index).coerceAtLeast(0)} từ trong lượt này",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { (index + 1).toFloat() / targetDeck.size.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isSrsReview && mode == MODE_NEW) {
                        Text("Làm quen từ mới", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(word.term, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        if (word.ipa.isNotBlank()) Text(word.ipa, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (word.partOfSpeech.isNotBlank()) Text(word.partOfSpeech, color = MaterialTheme.colorScheme.primary)
                        if (revealed) {
                            HorizontalDivider()
                            Text(word.meaning, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (word.example.isNotBlank()) Text("Ví dụ: ${word.example}", style = MaterialTheme.typography.bodyLarge)
                            if (word.topic.isNotBlank()) Text("Chủ đề: ${word.topic}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (word.collocations.isNotBlank()) Text("Cụm từ: ${word.collocations}")
                            if (word.synonyms.isNotBlank()) Text("Từ đồng nghĩa: ${word.synonyms}")
                            if (word.wordFamily.isNotBlank()) Text("Họ từ: ${word.wordFamily}")
                            if (word.tags.isNotBlank()) Text("Thẻ: ${word.tags}")
                            if (word.notes.isNotBlank()) Text("Ghi chú: ${word.notes}")
                            Button(onClick = { onNewWordIntroduced(word); index++; resetAnswerState() }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (index + 1 >= targetDeck.size) "Hoàn tất làm quen" else "Từ tiếp theo")
                            }
                        } else {
                            Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) { Text("Hiện nghĩa và ví dụ") }
                        }
                    } else when (activeType) {
                        ActiveExerciseType.MULTIPLE_CHOICE -> {
                            Text("Chọn nghĩa chính xác", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(word.term, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                            if (word.ipa.isNotBlank()) Text(word.ipa, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!mcAvailable) {
                                Text("Không tạo câu hỏi trắc nghiệm: cần 4 nghĩa khác nhau trong dữ liệu đã nhập.", color = MaterialTheme.colorScheme.error)
                            } else {
                                options.forEach { option ->
                                    val isTarget = option.id == word.id
                                    val wasChosenWrong = answered && selectedOptionId == option.id && correct == false
                                    OutlinedButton(
                                        onClick = {
                                            selectedOptionId = option.id
                                            submitAnswer(word, isTarget, ActiveExerciseType.MULTIPLE_CHOICE)
                                        },
                                        enabled = !answered,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = when {
                                                answered && isTarget -> MaterialTheme.colorScheme.primaryContainer
                                                wasChosenWrong -> MaterialTheme.colorScheme.errorContainer
                                                else -> MaterialTheme.colorScheme.surface
                                            }
                                        )
                                    ) {
                                        Text(option.meaning, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                        ActiveExerciseType.LISTEN_AND_TYPE -> {
                            Text("Nghe và gõ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Nghe cách phát âm, sau đó nhập từ tiếng Anh bạn nghe được.")
                            OfflineSpeakButton(
                                text = word.term,
                                label = "Phát âm từ",
                                onAvailabilityChanged = { ready, status ->
                                    audioReady = ready
                                    audioStatus = status
                                },
                                onAudioReplay = onAudioReplay
                            )
                            if (!audioReady) {
                                Text(audioStatus, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                val alternative = availableTypes(word).firstOrNull { it != ActiveExerciseType.LISTEN_AND_TYPE }
                                OutlinedButton(
                                    onClick = {
                                        if (alternative != null) {
                                            overrideType = alternative
                                            answered = false
                                            answerText = ""
                                            correct = null
                                        } else {
                                            index++
                                            resetAnswerState()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(if (alternative != null) "Chuyển sang dạng bài khả dụng" else "Bỏ qua từ này, không ghi kết quả") }
                            }
                            OutlinedTextField(
                                value = answerText,
                                onValueChange = { if (!answered) answerText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Nhập từ nghe được") },
                                singleLine = true
                            )
                            Button(
                                onClick = { submitAnswer(word, QuizLogic.isCorrectAnswer(answerText, word.term), ActiveExerciseType.LISTEN_AND_TYPE) },
                                enabled = !answered && audioReady && answerText.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Kiểm tra") }
                        }
                        ActiveExerciseType.CLOZE -> {
                            Text("Điền vào ngữ cảnh", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(clozeText.orEmpty(), style = MaterialTheme.typography.titleLarge, lineHeight = 30.sp)
                            OutlinedTextField(
                                value = answerText,
                                onValueChange = { if (!answered) answerText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Từ/cụm từ còn thiếu") },
                                singleLine = true
                            )
                            Button(
                                onClick = { submitAnswer(word, QuizLogic.isCorrectAnswer(answerText, word.term), ActiveExerciseType.CLOZE) },
                                enabled = !answered && answerText.isNotBlank() && clozeText != null,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Kiểm tra") }
                        }
                        null -> Unit
                    }

                    if (answered) {
                        Surface(
                            color = if (correct == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (correct == true) "Chính xác!" else "Chưa chính xác", fontWeight = FontWeight.Bold)
                                Text("${word.term} — ${word.meaning}")
                                if (word.ipa.isNotBlank()) Text("IPA: ${word.ipa}")
                                if (word.partOfSpeech.isNotBlank()) Text("Từ loại: ${word.partOfSpeech}")
                                if (word.example.isNotBlank()) Text("Ví dụ: ${word.example}")
                                if (word.collocations.isNotBlank()) Text("Cụm từ: ${word.collocations}")
                                if (word.synonyms.isNotBlank()) Text("Từ đồng nghĩa: ${word.synonyms}")
                                if (word.wordFamily.isNotBlank()) Text("Họ từ: ${word.wordFamily}")
                                if (word.notes.isNotBlank()) Text("Ghi chú: ${word.notes}")
                            }
                        }
                        Button(
                            onClick = {
                                index++
                                resetAnswerState()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (index + 1 >= targetDeck.size) "Tiếp tục / bắt đầu lại" else "Câu tiếp theo") }
                    }
                }
            }
        }

        if (attempts > 0) {
            Text("Kết quả phiên này: $correctCount/$attempts đúng", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }

    if (showMatch) MatchGameDialog(words = words.filterNot { it.excludedFromReview }, onDismiss = { showMatch = false })
}

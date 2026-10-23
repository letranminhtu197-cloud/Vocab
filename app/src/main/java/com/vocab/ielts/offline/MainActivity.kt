package com.vocab.ielts.offline

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = VocabDatabase.getInstance(applicationContext)
        setContent { VocabApp(database.wordDao(), ReviewStore(database)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabApp(dao: WordDao, reviewStore: ReviewStore) {
    var tab by remember { mutableIntStateOf(0) }
    var studyMode by remember { mutableIntStateOf(0) }
    var quizStartWithDue by remember { mutableStateOf(true) }
    var learningPathTopic by remember { mutableStateOf<String?>(null) }
    var showPathLesson by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var libraryFilter by remember { mutableStateOf("Tất cả") }
    var sortMode by remember { mutableStateOf(LibrarySort.AZ) }
    var selectedTopic by remember { mutableStateOf("Mọi chủ đề") }
    var currentIndex by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    val ratingInProgress = remember { mutableStateOf(false) }
    var editorWord by remember { mutableStateOf<WordEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var wordToDelete by remember { mutableStateOf<WordEntity?>(null) }
    var detailWord by remember { mutableStateOf<WordEntity?>(null) }
    var practiceFocusId by remember { mutableStateOf<Long?>(null) }
    var showSamplesConfirm by remember { mutableStateOf(false) }
    var showStarterConfirm by remember { mutableStateOf(false) }
    var showDataTools by remember { mutableStateOf(false) }
    var dueOnly by remember { mutableStateOf(false) }
    var csvMessage by remember { mutableStateOf<String?>(null) }
    var pendingCsvWords by remember { mutableStateOf<List<WordEntity>?>(null) }
    var pendingCourseName by remember { mutableStateOf<String?>(null) }
    var pendingCourseLessons by remember { mutableIntStateOf(0) }
    var pendingCsvDuplicates by remember { mutableIntStateOf(0) }
    var pendingCsvInvalidRows by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val settingsPrefs = remember(context) { context.getSharedPreferences("vocab_settings", 0) }
    var darkTheme by remember(settingsPrefs) { mutableStateOf(settingsPrefs.getBoolean("dark_theme", false)) }
    var lessonTarget by remember(settingsPrefs) {
        mutableIntStateOf(settingsPrefs.getInt("lesson_target", 5).let { if (it in listOf(5, 10, 15, 20)) it else 5 })
    }
    var dailyGoal by remember(settingsPrefs) {
        mutableIntStateOf(StudyGoalLogic.normalizeGoal(settingsPrefs.getInt("daily_goal", 10)))
    }
    var reviewLimit by remember(settingsPrefs) {
        mutableIntStateOf(settingsPrefs.getInt("review_limit", 20).let { if (it in listOf(0, 10, 20, 50)) it else 20 })
    }
    var challengeIds by remember { mutableStateOf<List<Long>?>(null) }
    var challengeDone by remember(settingsPrefs) {
        mutableStateOf(
            settingsPrefs.getBoolean(DailyChallengeLogic.isCompletedKey(java.time.LocalDate.now()), false)
        )
    }
    val statsPrefs = remember(context) { context.getSharedPreferences("vocab_learning_stats", 0) }
    var quizCorrect by remember(statsPrefs) { mutableIntStateOf(statsPrefs.getInt("quiz_correct", 0)) }
    var quizTotal by remember(statsPrefs) { mutableIntStateOf(statsPrefs.getInt("quiz_total", 0)) }
    var quizSessionCorrect by remember(statsPrefs) { mutableIntStateOf(statsPrefs.getInt("quiz_session_correct", 0)) }
    var quizSessionTotal by remember(statsPrefs) { mutableIntStateOf(statsPrefs.getInt("quiz_session_total", 0)) }
    val scope = rememberCoroutineScope()
    val words by dao.observeAll().collectAsState(initial = emptyList())
    val topics = words.map { it.topic.trim() }.filter { it.isNotEmpty() }.distinct().sortedBy { it.lowercase() }
    val filtered = words.filter { word ->
        val matchesQuery = query.isBlank() || listOf(
            word.term, word.meaning, word.ipa, word.partOfSpeech, word.topic, word.example,
            word.collocations, word.synonyms, word.wordFamily, word.notes, word.tags
        ).any { it.contains(query.trim(), ignoreCase = true) }
        val matchesMode = when (libraryFilter) {
            "Đến hạn" -> WordStatus.isDue(word, System.currentTimeMillis())
            "Từ yếu" -> WordStatus.isWeak(word)
            "Có thẻ" -> word.tags.isNotBlank()
            else -> true
        }
        matchesQuery && matchesMode && (selectedTopic == "Mọi chủ đề" || word.topic.trim().equals(selectedTopic, ignoreCase = true))
    }
    val visibleWords = LibrarySorting.sort(filtered, sortMode)
    val reviewDeck = if (dueOnly) {
        words.filter { !it.paused && WordStatus.isDue(it, System.currentTimeMillis()) }
            .sortedBy { if (it.nextReviewAtMillis <= 0L) Long.MIN_VALUE else it.nextReviewAtMillis }
            .let { due -> if (reviewLimit > 0) due.take(reviewLimit) else due }
    } else words.filter { !it.paused }
    val challengeWords = remember(challengeIds, words) {
        challengeIds?.let { ids -> ids.mapNotNull { id -> words.firstOrNull { it.id == id } } } ?: emptyList()
    }
    val recordReview: (WordEntity, ReviewRating) -> Unit = { word, rating ->
        StudyActivityTracker.recordEvent(statsPrefs)
        scope.launch { reviewStore.rate(word.id, rating, System.currentTimeMillis()) }
    }
    val recordQuizAnswer: (WordEntity, Boolean) -> Unit = { word, correct ->
        StudyActivityTracker.recordEvent(statsPrefs)
        quizTotal += 1
        quizSessionTotal += 1
        if (correct) {
            quizCorrect += 1
            quizSessionCorrect += 1
        }
        statsPrefs.edit()
            .putInt("quiz_total", quizTotal)
            .putInt("quiz_correct", quizCorrect)
            .putInt("quiz_session_total", quizSessionTotal)
            .putInt("quiz_session_correct", quizSessionCorrect)
            .apply()
        scope.launch { reviewStore.rateByCorrectness(word.id, correct, Instant.now().toEpochMilli()) }
    }
    val goldenNow = System.currentTimeMillis()
    val goldenReminderAt = remember(words) { GoldenTimeLogic.nextReminderAt(words, goldenNow) }
    val goldenSoonest = remember(words) { GoldenTimeLogic.soonestWord(words, goldenNow) }
    val goldenDueNow = WordStatus.dueCount(words, goldenNow)
    val goldenLabel = when {
        words.isEmpty() -> "Thêm từ để Vocab tính thời điểm ôn cho bạn."
        goldenDueNow > 0 -> "Ôn các từ đến hạn ngay khi bạn sẵn sàng."
        goldenReminderAt != null -> "Từ \"${goldenSoonest?.term}\" nên ôn ${GoldenTimeLogic.label(goldenReminderAt, goldenNow)}."
        else -> "Chưa có lịch ôn nào sắp tới."
    }
    LaunchedEffect(words) {
        if (StudyReminder.isEnabled(context)) {
            val now = System.currentTimeMillis()
            val next = GoldenTimeLogic.nextReminderAt(words, now)
            if (next != null) StudyReminder.scheduleGoldenTime(context, next) else StudyReminder.schedule(context)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val importedTextResult = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { CsvCodec.readUtf8(it) }
                            ?: error("Không mở được file CSV.")
                    }
                }
                val importedText = importedTextResult.getOrNull()
                if (importedText == null) {
                    csvMessage = importedTextResult.exceptionOrNull()?.message
                        ?: "Không đọc được file. Hãy chọn một file CSV UTF-8 hợp lệ."
                } else {
                    val trimmedInput = importedText.trimStart()
                    val isJson = trimmedInput.startsWith("{") || trimmedInput.startsWith("[")
                    val parsedWords: List<WordEntity>
                    val parsedInvalid: Int
                    val parsedError: String?
                    var courseName: String? = null
                    var courseLessons = 0
                    if (isJson) {
                        val course = CoursePackCodec.parse(importedText)
                        parsedWords = course.words
                        parsedInvalid = course.invalidRows
                        parsedError = course.error
                        courseName = course.name
                        courseLessons = course.lessonCount
                    } else {
                        val parsed = ImportCodec.parse(importedText)
                        parsedWords = parsed.words
                        parsedInvalid = parsed.invalidRows
                        parsedError = parsed.error
                    }
                    if (parsedError != null) {
                        csvMessage = parsedError
                    } else {
                        val existingTerms = dao.observeAll().first().map { it.term.trim().lowercase() }.toMutableSet()
                        var duplicateCount = 0
                        val uniqueWords = parsedWords.filter { word ->
                            if (existingTerms.add(word.term.trim().lowercase())) true
                            else { duplicateCount++; false }
                        }
                        if (uniqueWords.isEmpty()) {
                            csvMessage = "Không có từ mới để nhập. Bỏ qua $duplicateCount từ trùng và $parsedInvalid dòng thiếu từ/nghĩa."
                        } else {
                            pendingCsvWords = uniqueWords
                            pendingCsvDuplicates = duplicateCount
                            pendingCsvInvalidRows = parsedInvalid
                            pendingCourseName = courseName
                            pendingCourseLessons = courseLessons
                        }
                    }
                }
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val allWords = dao.observeAll().first()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                            it.write("\uFEFF")
                            it.write(CsvCodec.export(allWords))
                        } ?: error("Không mở được file đích.")
                    }
                    allWords.size
                }
                csvMessage = result.fold(
                    onSuccess = { "Đã xuất ${it} từ ra file CSV." },
                    onFailure = { "Xuất CSV thất bại: ${it.message ?: "lỗi ghi file"}" }
                )
            }
        }
    }

    VocabTheme(darkTheme = darkTheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                when {
                                    tab == 0 && challengeWords.isNotEmpty() -> "Thử thách hôm nay"
                                    tab == 0 -> "Hôm nay"
                                    tab == 1 -> "Sổ từ"
                                    tab == 2 || tab == 3 -> "Học"
                                    else -> "Tiến độ"
                                },
                                fontWeight = FontWeight.Bold
                            )
                            Text("Vocab · IELTS · Offline", fontSize = 12.sp)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            darkTheme = !darkTheme
                            settingsPrefs.edit().putBoolean("dark_theme", darkTheme).apply()
                        }) {
                            Icon(
                                imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (darkTheme) "Chuyển sang giao diện sáng" else "Chuyển sang giao diện tối"
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                if (tab == 1) {
                    FloatingActionButton(onClick = { editorWord = null; showEditor = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm từ")
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    val destinations = listOf(
                        Triple(0, "Hôm nay", Icons.Default.Home),
                        Triple(3, "Học", Icons.Default.School),
                        Triple(1, "Sổ từ", Icons.Default.MenuBook),
                        Triple(4, "Tiến độ", Icons.Default.QueryStats)
                    )
                    destinations.forEach { (destination, label, icon) ->
                        NavigationBarItem(
                            selected = tab == destination || (destination == 3 && tab == 2),
                            onClick = {
                                tab = destination
                                if (destination == 3) {
                                    studyMode = 0
                                    quizStartWithDue = false
                                    practiceFocusId = null
                                }
                            },
                            icon = {
                                Icon(icon, contentDescription = label)
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            when (tab) {
                0 -> if (challengeWords.isNotEmpty()) {
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(onClick = { challengeIds = null }) { Text("← Đóng thử thách") }
                            Text(
                                "Thử thách hôm nay",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        LessonScreen(
                            words = challengeWords,
                            modifier = Modifier.fillMaxSize().weight(1f),
                            sessionSize = challengeWords.size,
                            onReview = recordReview,
                            onCompleted = {
                                challengeDone = true
                                settingsPrefs.edit()
                                    .putBoolean(DailyChallengeLogic.isCompletedKey(java.time.LocalDate.now()), true)
                                    .apply()
                            }
                        )
                    }
                } else LazyColumn(
                    Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        DashboardOverview(
                            totalWords = words.size,
                            dueWords = WordStatus.dueCount(words, System.currentTimeMillis()),
                            learnedWords = words.count { it.reviewCount > 0 },
                            activeStreak = StudyActivityTracker.activeDayCount(statsPrefs),
                            todayActivities = StudyActivityTracker.countForDate(statsPrefs, java.time.LocalDate.now()),
                            dailyGoal = dailyGoal,
                            onGoalChange = { goal ->
                                dailyGoal = goal
                                settingsPrefs.edit().putInt("daily_goal", goal).apply()
                            },
                            reviewLimit = reviewLimit,
                            onReviewLimitChange = { limit ->
                                reviewLimit = limit
                                settingsPrefs.edit().putInt("review_limit", limit).apply()
                            },
                            challengeDone = challengeDone,
                            challengeSize = DailyChallengeLogic.SIZE,
                            goldenLabel = goldenLabel,
                            goldenDueNow = goldenDueNow,
                            onStartChallenge = {
                                challengeIds = DailyChallengeLogic.selectWords(
                                    words,
                                    java.time.LocalDate.now(),
                                    System.currentTimeMillis()
                                ).map { it.id }
                            },
                            onStartReview = {
                                quizStartWithDue = true
                                studyMode = 2
                                tab = if (words.isEmpty()) 1 else 3
                            },
                            onOpenPractice = {
                                quizStartWithDue = false
                                studyMode = 2
                                tab = 3
                            },
                            onOpenLibrary = { tab = 1 }
                        )
                    }
                }
                1 -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Text("Sổ tay từ vựng", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Thêm, chỉnh sửa hoặc xóa từ. Dữ liệu vẫn còn sau khi đóng app.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tìm từ, nghĩa, ví dụ, cụm từ, ghi chú, thẻ…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )
                    var topicMenuOpen by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Tất cả", "Đến hạn", "Từ yếu", "Có thẻ").forEach { option ->
                            FilterChip(
                                selected = libraryFilter == option,
                                onClick = { libraryFilter = option },
                                label = { Text(option) }
                            )
                        }
                    }
                    if (topics.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Lọc chủ đề", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { topicMenuOpen = true }) { Text(selectedTopic) }
                            DropdownMenu(expanded = topicMenuOpen, onDismissRequest = { topicMenuOpen = false }) {
                                (listOf("Mọi chủ đề") + topics).forEach { topic ->
                                    DropdownMenuItem(
                                        text = { Text(topic) },
                                        onClick = { selectedTopic = topic; topicMenuOpen = false }
                                    )
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text(
                            "${filtered.size}/${words.size} từ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        var sortMenuOpen by remember { mutableStateOf(false) }
                        TextButton(onClick = { sortMenuOpen = true }) { Text("Sắp xếp: ${sortMode.label}") }
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            LibrarySort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = { sortMode = option; sortMenuOpen = false }
                                )
                            }
                        }
                        TextButton(onClick = { showDataTools = !showDataTools }) {
                            Text(if (showDataTools) "Ẩn dữ liệu" else "Dữ liệu")
                        }
                    }
                    if (showDataTools) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/json", "application/octet-stream")) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Nhập tệp") }
                            OutlinedButton(
                                onClick = { exportLauncher.launch("vocab-words.csv") },
                                modifier = Modifier.weight(1f)
                            ) { Text("Xuất CSV") }
                        }
                        Text(
                            "CSV/TSV/TXT: cột term, meaning hoặc mỗi dòng “từ - nghĩa [- câu ví dụ]”. " +
                                "JSON: gói khóa học nhiều bài (xem docs/course-pack-example.json).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BackupControls(dao = dao, onMessage = { csvMessage = it })
                    }
                    if (words.isEmpty()) {
                        Spacer(Modifier.height(18.dp))
                        Text("Chưa có từ nào. Nhấn nút + để nhập từ đầu tiên.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { showStarterConfirm = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text("Thêm khóa khởi đầu ${StarterCourse.words.size} từ")
                        }
                        OutlinedButton(onClick = { showSamplesConfirm = true }, modifier = Modifier.padding(top = 8.dp)) { Text("Thêm 3 từ mẫu để thử") }
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f).padding(top = 10.dp)
                    ) {
                        if (filtered.isEmpty() && words.isNotEmpty()) {
                            item {
                                Text(
                                    "Không tìm thấy từ phù hợp. Hãy thử từ khóa khác hoặc xóa nội dung tìm kiếm.",
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(visibleWords, key = { it.id }) { word ->
                            Card(
                                onClick = { detailWord = word },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(word.term, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    if (word.ipa.isNotBlank() || word.partOfSpeech.isNotBlank()) Text(listOf(word.ipa, word.partOfSpeech).filter { it.isNotBlank() }.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                                    Text(word.meaning, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            word.topic.ifBlank { "Chưa phân chủ đề" },
                                            Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            when {
                                                word.paused -> "Tạm dừng"
                                                word.reviewCount == 0 -> "Mới"
                                                WordStatus.isDue(word, System.currentTimeMillis()) -> "Đến hạn"
                                                else -> "Cấp ${word.srsLevel}/5"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (WordStatus.isDue(word, System.currentTimeMillis()) && !word.paused)
                                                MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> Column(Modifier.fillMaxSize().padding(padding)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        listOf("Flashcard", "Lộ trình", "Bài tập", "Ghép cặp").forEachIndexed { index, label ->
                            FilterChip(
                                selected = studyMode == index,
                                onClick = {
                                    studyMode = index
                                    if (index == 1) showPathLesson = false
                                    if (index == 2) {
                                        quizStartWithDue = false
                                        practiceFocusId = null
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    Box(Modifier.fillMaxSize().weight(1f)) {
                        when (studyMode) {
                            0 -> FlashcardReviewScreen(
                                allWords = words,
                                deck = reviewDeck,
                                dueOnly = dueOnly,
                                currentIndex = currentIndex,
                                revealed = revealed,
                                ratingInProgress = ratingInProgress.value,
                                onDueOnlyChange = { value -> dueOnly = value; currentIndex = 0; revealed = false },
                                onIndexChange = { currentIndex = it },
                                onRevealChange = { revealed = it },
                                onRate = { word, rating ->
                                    if (!ratingInProgress.value) {
                                        ratingInProgress.value = true
                                        StudyActivityTracker.recordEvent(statsPrefs)
                                        val nowMillis = System.currentTimeMillis()
                                        scope.launch {
                                            try {
                                                reviewStore.rate(word.id, rating, nowMillis)
                                                currentIndex = FlashcardLogic.nextIndexAfterRating(currentIndex, reviewDeck.size, dueOnly)
                                                revealed = false
                                            } finally {
                                                ratingInProgress.value = false
                                            }
                                        }
                                    }
                                }
                            )
                            1 -> if (showPathLesson) {
                                Column(Modifier.fillMaxSize()) {
                                    TextButton(onClick = { showPathLesson = false }) { Text("← Quay lại lộ trình") }
                                    val pathWords = when (learningPathTopic) {
                                        null -> words
                                        "Chưa phân loại" -> words.filter { it.topic.isBlank() }
                                        else -> words.filter { it.topic.trim().equals(learningPathTopic, ignoreCase = true) }
                                    }
                                    LessonScreen(
                                        words = pathWords,
                                        modifier = Modifier.fillMaxSize().weight(1f),
                                        sessionSize = lessonTarget,
                                        onReview = recordReview
                                    )
                                }
                            } else {
                                LearningPathScreen(
                                    words = words,
                                    modifier = Modifier.fillMaxSize(),
                                    sessionSize = lessonTarget,
                                    onSessionSizeChange = { target ->
                                        lessonTarget = target
                                        settingsPrefs.edit().putInt("lesson_target", target).apply()
                                    },
                                    onStart = { topic -> learningPathTopic = topic; showPathLesson = true },
                                    onPractice = { showPathLesson = false; quizStartWithDue = false; studyMode = 2 },
                                    onOpenLibrary = { tab = 1 }
                                )
                            }
                            2 -> QuizScreen(
                                words = words,
                                modifier = Modifier.fillMaxSize(),
                                sessionScore = quizSessionCorrect,
                                sessionTotal = quizSessionTotal,
                                onAnswerRecorded = recordQuizAnswer,
                                onSessionReset = {
                                    quizSessionCorrect = 0
                                    quizSessionTotal = 0
                                    statsPrefs.edit()
                                        .putInt("quiz_session_total", 0)
                                        .putInt("quiz_session_correct", 0)
                                        .apply()
                                },
                                startWithDue = quizStartWithDue,
                                focusWordId = practiceFocusId,
                                dueLimit = reviewLimit,
                                onClearFocus = { practiceFocusId = null }
                            )
                            else -> MatchingGameScreen(
                                words = words,
                                modifier = Modifier.fillMaxSize(),
                                onAnswerRecorded = recordQuizAnswer
                            )
                        }
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Tiến độ học", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Nhìn lại việc học, ghi nhớ và các chủ đề đang tiến bộ.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ProgressMetric("Từ trong sổ", words.size.toString(), Modifier.weight(1f))
                                    ProgressMetric("Đến hạn", WordStatus.dueCount(words, System.currentTimeMillis()).toString(), Modifier.weight(1f))
                                }
                                HorizontalDivider()
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ProgressMetric("Lượt ôn", words.sumOf { it.reviewCount }.toString(), Modifier.weight(1f))
                                    ProgressMetric("Quiz đúng", if (quizTotal == 0) "—" else "${quizCorrect * 100 / quizTotal}%", Modifier.weight(1f))
                                }
                                HorizontalDivider()
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ProgressMetric("Chuỗi học", "${StudyActivityTracker.activeDayCount(statsPrefs)} ngày", Modifier.weight(1f))
                                    ProgressMetric("Cấp 5", words.count { it.srsLevel == 5 }.toString(), Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    item {
                        AchievementSection(
                            totalWords = words.size,
                            totalPracticeEvents = words.sumOf { it.reviewCount },
                            activeStreak = StudyActivityTracker.activeDayCount(statsPrefs),
                            todayActivities = StudyActivityTracker.countForDate(statsPrefs, java.time.LocalDate.now()),
                            dailyGoal = dailyGoal
                        )
                    }
                    item {
                        val week = StudyActivityTracker.lastSevenDays(statsPrefs)
                        val activeDays = week.count { it.activities > 0 }
                        val eventTotal = week.sumOf { it.activities }
                        val maximum = week.maxOfOrNull { it.activities }?.coerceAtLeast(1) ?: 1
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Hoạt động 7 ngày gần nhất", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("$activeDays/7 ngày có học • $eventTotal lượt luyện/ôn", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                week.forEach { day ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(day.date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")), modifier = Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
                                        LinearProgressIndicator(
                                            progress = { day.activities.toFloat() / maximum },
                                            modifier = Modifier.weight(1f).height(8.dp)
                                        )
                                        Text(day.activities.toString(), modifier = Modifier.width(24.dp), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        val topicProgress = LearningPathLogic.topics(words, System.currentTimeMillis()).take(5)
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Tiến độ theo chủ đề", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                if (topicProgress.isEmpty()) {
                                    Text("Thêm từ và gán chủ đề IELTS để theo dõi tiến độ từng chủ đề.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    topicProgress.forEach { topic ->
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(topic.topic, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                                Text("${topic.percent}%")
                                            }
                                            LinearProgressIndicator(progress = { topic.percent / 100f }, modifier = Modifier.fillMaxWidth())
                                            Text("${topic.learned}/${topic.total} đã ôn • ${topic.due} đến hạn • ${topic.weak} từ yếu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        val priorityWords = words.filter { it.lapseCount > 0 }.sortedWith(compareByDescending<WordEntity> { it.lapseCount }.thenBy { it.term.lowercase() }).take(5)
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Từ cần ưu tiên", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                if (priorityWords.isEmpty()) {
                                    Text("Chưa có từ nào được ghi nhận là hay quên. Khi chọn “Lại” hoặc trả lời sai trong quiz, các từ cần luyện lại sẽ xuất hiện ở đây.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    priorityWords.forEach { word ->
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(word.term, fontWeight = FontWeight.SemiBold)
                                                Text(word.meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text("${word.lapseCount} lần quên", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                    item { LevelDistributionChart(words = words) }
                    item { StudyReminderControl(onMessage = { csvMessage = it }) }
                    item { StatCard("Chế độ hoạt động", "Ngoại tuyến") }
                }
            }
        }
    }

    pendingCsvWords?.let { previewWords ->
        val courseName = pendingCourseName
        AlertDialog(
            onDismissRequest = { pendingCsvWords = null; pendingCourseName = null },
            title = { Text(if (courseName != null) "Xác nhận nhập khóa học" else "Xác nhận nhập CSV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (courseName != null) {
                        Text("Khóa: $courseName (${pendingCourseLessons} bài)")
                    }
                    Text("Sẽ thêm ${previewWords.size} từ mới vào sổ. Không ghi đè từ hiện có.")
                    Text("Bỏ qua ${pendingCsvDuplicates} từ trùng và ${pendingCsvInvalidRows} dòng thiếu từ/nghĩa.")
                    Text("Xem trước", fontWeight = FontWeight.SemiBold)
                    previewWords.take(5).forEach { item ->
                        Text("• ${item.term} — ${item.meaning}")
                    }
                    if (previewWords.size > 5) Text("… và ${previewWords.size - 5} từ khác")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val wordsToInsert = previewWords
                    val duplicates = pendingCsvDuplicates
                    val invalidRows = pendingCsvInvalidRows
                    val importedCourse = courseName
                    pendingCsvWords = null
                    pendingCourseName = null
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) { dao.insertAll(wordsToInsert) }
                        }
                        csvMessage = if (result.isFailure) {
                            "Nhập file thất bại: ${result.exceptionOrNull()?.message ?: "lỗi dữ liệu"}"
                        } else if (importedCourse != null) {
                            "Đã nhập khóa \"$importedCourse\": ${wordsToInsert.size} từ. Bỏ qua ${duplicates} từ trùng và ${invalidRows} dòng lỗi."
                        } else {
                            "Đã nhập ${wordsToInsert.size} từ. Bỏ qua ${duplicates} từ trùng và ${invalidRows} dòng thiếu từ/nghĩa."
                        }
                    }
                }) { Text("Nhập ${previewWords.size} từ") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCsvWords = null; pendingCourseName = null }) { Text("Hủy") }
            }
        )
    }
    if (csvMessage != null) {
        AlertDialog(
            onDismissRequest = { csvMessage = null },
            title = { Text("CSV") },
            text = { Text(csvMessage.orEmpty()) },
            confirmButton = { TextButton(onClick = { csvMessage = null }) { Text("Đóng") } }
        )
    }
    if (showEditor) {
        WordEditorDialog(
            initial = editorWord,
            onDismiss = { showEditor = false; editorWord = null },
            onSave = { word ->
                scope.launch {
                    val normalizedTerm = word.term.trim()
                    val editingId = editorWord?.id
                    val duplicate = dao.observeAll().first().any {
                        it.id != editingId && it.term.trim().equals(normalizedTerm, ignoreCase = true)
                    }
                    if (duplicate) {
                        csvMessage = "Từ/cụm từ \"$normalizedTerm\" đã có trong sổ. Hãy kiểm tra từ hiện có thay vì tạo bản trùng."
                    } else {
                        val normalizedWord = word.copy(term = normalizedTerm, meaning = word.meaning.trim())
                        if (editorWord == null) dao.insert(normalizedWord) else dao.update(normalizedWord)
                        showEditor = false
                        editorWord = null
                    }
                }
            }
        )
    }
    if (wordToDelete != null) {
        val deleting = wordToDelete!!
        AlertDialog(
            onDismissRequest = { wordToDelete = null },
            title = { Text("Xóa từ này?") },
            text = { Text("Bạn có chắc muốn xóa \"${deleting.term}\" khỏi sổ từ vựng? Thao tác này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.delete(deleting) }
                    wordToDelete = null
                }) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { wordToDelete = null }) { Text("Hủy") } }
        )
    }
    if (showSamplesConfirm) {
        AlertDialog(
            onDismissRequest = { showSamplesConfirm = false },
            title = { Text("Thêm từ mẫu?") },
            text = { Text("Thêm 3 từ ví dụ vào sổ để bạn kiểm tra flashcard và lịch ôn. Ứng dụng không tự nạp bộ từ mẫu khi cài đặt.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val existing = words.map { it.term.lowercase() }.toSet()
                        WordSamples.words.filterNot { it.term.lowercase() in existing }.forEach { dao.insert(it) }
                    }
                    showSamplesConfirm = false
                }) { Text("Thêm từ mẫu") }
            },
            dismissButton = { TextButton(onClick = { showSamplesConfirm = false }) { Text("Hủy") } }
        )
    }
    if (showStarterConfirm) {
        AlertDialog(
            onDismissRequest = { showStarterConfirm = false },
            title = { Text(StarterCourse.NAME) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Thêm ${StarterCourse.words.size} từ IELTS khởi đầu (6 chủ đề) do Vocab tự soạn — " +
                            "không phải dữ liệu trích từ ứng dụng tham chiếu."
                    )
                    Text("Sau khi thêm, bạn có thể sửa hoặc xóa từng từ; tiến độ SRS tính như từ tự nhập.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showStarterConfirm = false
                    scope.launch {
                        val existing = words.map { it.term.trim().lowercase() }.toMutableSet()
                        val toInsert = StarterCourse.words.filter { existing.add(it.term.trim().lowercase()) }
                        if (toInsert.isEmpty()) {
                            csvMessage = "Khóa khởi đầu đã có đủ trong sổ từ."
                        } else {
                            dao.insertAll(toInsert)
                            csvMessage = "Đã thêm ${toInsert.size} từ từ ${StarterCourse.NAME}."
                        }
                    }
                }) { Text("Thêm ${StarterCourse.words.size} từ") }
            },
            dismissButton = { TextButton(onClick = { showStarterConfirm = false }) { Text("Hủy") } }
        )
    }
    detailWord?.let { detail ->
        WordDetailSheet(
            word = detail,
            onDismiss = { detailWord = null },
            onEdit = {
                detailWord = null
                editorWord = detail
                showEditor = true
            },
            onDelete = {
                detailWord = null
                wordToDelete = detail
            },
            onPractice = {
                detailWord = null
                practiceFocusId = detail.id
                quizStartWithDue = false
                studyMode = 2
                tab = 3
            },
            onTogglePause = {
                scope.launch { dao.update(detail.copy(paused = !detail.paused)) }
                detailWord = null
            }
        )
    }
}

@Composable
private fun WordEditorDialog(
    initial: WordEntity?,
    onDismiss: () -> Unit,
    onSave: (WordEntity) -> Unit
) {
    var term by remember(initial?.id) { mutableStateOf(initial?.term.orEmpty()) }
    var ipa by remember(initial?.id) { mutableStateOf(initial?.ipa.orEmpty()) }
    var pos by remember(initial?.id) { mutableStateOf(initial?.partOfSpeech.orEmpty()) }
    var meaning by remember(initial?.id) { mutableStateOf(initial?.meaning.orEmpty()) }
    var example by remember(initial?.id) { mutableStateOf(initial?.example.orEmpty()) }
    var exampleMeaning by remember(initial?.id) { mutableStateOf(initial?.exampleMeaning.orEmpty()) }
    var paused by remember(initial?.id) { mutableStateOf(initial?.paused ?: false) }
    var topic by remember(initial?.id) { mutableStateOf(initial?.topic.orEmpty()) }
    var collocations by remember(initial?.id) { mutableStateOf(initial?.collocations.orEmpty()) }
    var synonyms by remember(initial?.id) { mutableStateOf(initial?.synonyms.orEmpty()) }
    var wordFamily by remember(initial?.id) { mutableStateOf(initial?.wordFamily.orEmpty()) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    var tags by remember(initial?.id) { mutableStateOf(initial?.tags.orEmpty()) }
    val canSave = term.isNotBlank() && meaning.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Thêm từ mới" else "Chỉnh sửa từ") },
        text = {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(term, { term = it }, label = { Text("Từ / cụm từ *") }, singleLine = true)
                OutlinedTextField(meaning, { meaning = it }, label = { Text("Nghĩa tiếng Việt *") })
                OutlinedTextField(ipa, { ipa = it }, label = { Text("Phiên âm IPA") }, singleLine = true)
                OutlinedTextField(pos, { pos = it }, label = { Text("Từ loại") }, singleLine = true)
                OutlinedTextField(example, { example = it }, label = { Text("Câu ví dụ") })
                OutlinedTextField(exampleMeaning, { exampleMeaning = it }, label = { Text("Dịch nghĩa câu ví dụ") }, minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Tạm dừng ôn từ này", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Từ vẫn ở trong sổ nhưng không xuất hiện trong lượt ôn hay thử thách.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = paused, onCheckedChange = { paused = it })
                }
                OutlinedTextField(topic, { topic = it }, label = { Text("Chủ đề IELTS") }, singleLine = true)
                OutlinedTextField(collocations, { collocations = it }, label = { Text("Collocations / cụm từ đi kèm") }, minLines = 2)
                OutlinedTextField(synonyms, { synonyms = it }, label = { Text("Synonyms / từ đồng nghĩa") }, minLines = 2)
                OutlinedTextField(wordFamily, { wordFamily = it }, label = { Text("Word family / họ từ") }, minLines = 2)
                OutlinedTextField(tags, { tags = it }, label = { Text("Tags / thẻ (phân tách bằng dấu phẩy)") }, minLines = 2)
                OutlinedTextField(notes, { notes = it }, label = { Text("Ghi chú cá nhân") }, minLines = 2)
                Text("* Bắt buộc", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val base = initial ?: WordEntity(term = term.trim(), meaning = meaning.trim())
                    onSave(base.copy(
                        term = term.trim(),
                        ipa = ipa.trim(),
                        partOfSpeech = pos.trim(),
                        meaning = meaning.trim(),
                        example = example.trim(),
                        exampleMeaning = exampleMeaning.trim(),
                        paused = paused,
                        topic = topic.trim(),
                        collocations = collocations.trim(),
                        synonyms = synonyms.trim(),
                        wordFamily = wordFamily.trim(),
                        notes = notes.trim(),
                        tags = tags.trim()
                    ))
                }
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun ProgressMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 10.dp, horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

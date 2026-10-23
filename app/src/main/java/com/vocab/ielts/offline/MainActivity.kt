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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Edit
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
        setContent { VocabApp(database.wordDao()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabApp(dao: WordDao) {
    var tab by remember { mutableIntStateOf(0) }
    var quizStartWithDue by remember { mutableStateOf(true) }
    var learningPathTopic by remember { mutableStateOf<String?>(null) }
    var showPathLesson by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var libraryFilter by remember { mutableStateOf("Tất cả") }
    var selectedTopic by remember { mutableStateOf("Mọi chủ đề") }
    var currentIndex by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    val ratingInProgress = remember { mutableStateOf(false) }
    var editorWord by remember { mutableStateOf<WordEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var wordToDelete by remember { mutableStateOf<WordEntity?>(null) }
    var showSamplesConfirm by remember { mutableStateOf(false) }
    var dueOnly by remember { mutableStateOf(false) }
    var csvMessage by remember { mutableStateOf<String?>(null) }
    var pendingCsvWords by remember { mutableStateOf<List<WordEntity>?>(null) }
    var pendingCsvDuplicates by remember { mutableIntStateOf(0) }
    var pendingCsvInvalidRows by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val settingsPrefs = remember(context) { context.getSharedPreferences("vocab_settings", 0) }
    var darkTheme by remember(settingsPrefs) { mutableStateOf(settingsPrefs.getBoolean("dark_theme", true)) }
    var lessonTarget by remember(settingsPrefs) {
        mutableIntStateOf(settingsPrefs.getInt("lesson_target", 5).let { if (it in listOf(5, 10, 15, 20)) it else 5 })
    }
    var dailyGoal by remember(settingsPrefs) {
        mutableIntStateOf(StudyGoalLogic.normalizeGoal(settingsPrefs.getInt("daily_goal", 10)))
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
            "Đến hạn" -> word.nextReviewAtMillis == 0L || word.nextReviewAtMillis <= System.currentTimeMillis()
            "Từ yếu" -> word.lapseCount > 0 || (word.reviewCount > 0 && word.srsLevel <= 1)
            "Có thẻ" -> word.tags.isNotBlank()
            else -> true
        }
        matchesQuery && matchesMode && (selectedTopic == "Mọi chủ đề" || word.topic.trim().equals(selectedTopic, ignoreCase = true))
    }
    val reviewableWords = words.filterNot { it.excludedFromReview }
    val reviewDeck = if (dueOnly) reviewableWords.filter { it.nextReviewAtMillis == 0L || it.nextReviewAtMillis <= System.currentTimeMillis() } else reviewableWords
    val current = reviewDeck.getOrNull(currentIndex.coerceIn(0, (reviewDeck.size - 1).coerceAtLeast(0)))
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
                    val parsed = CsvCodec.parse(importedText)
                    if (parsed.error != null) {
                        csvMessage = parsed.error
                    } else {
                        val existingTerms = dao.observeAll().first().map { it.term.trim().lowercase() }.toMutableSet()
                        var duplicateCount = 0
                        val uniqueWords = parsed.words.filter { word ->
                            if (existingTerms.add(word.term.trim().lowercase())) true
                            else { duplicateCount++; false }
                        }
                        if (uniqueWords.isEmpty()) {
                            csvMessage = "Không có từ mới để nhập. Bỏ qua ${duplicateCount} từ trùng và ${parsed.invalidRows} dòng thiếu từ/nghĩa."
                        } else {
                            pendingCsvWords = uniqueWords
                            pendingCsvDuplicates = duplicateCount
                            pendingCsvInvalidRows = parsed.invalidRows
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

    MaterialTheme(
        colorScheme = if (darkTheme) {
            darkColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFFB9A5FF),
                onPrimary = androidx.compose.ui.graphics.Color(0xFF2B176E),
                primaryContainer = androidx.compose.ui.graphics.Color(0xFF34245F),
                onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFEAE3FF),
                secondary = androidx.compose.ui.graphics.Color(0xFFFFA98E),
                onSecondary = androidx.compose.ui.graphics.Color(0xFF52200C),
                secondaryContainer = androidx.compose.ui.graphics.Color(0xFF4D2C3A),
                onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE1D4),
                tertiary = androidx.compose.ui.graphics.Color(0xFF83E0CB),
                onTertiary = androidx.compose.ui.graphics.Color(0xFF003B35),
                tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF153F40),
                onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFC8F4E9),
                background = androidx.compose.ui.graphics.Color(0xFF12111B),
                onBackground = androidx.compose.ui.graphics.Color(0xFFF2EDF8),
                surface = androidx.compose.ui.graphics.Color(0xFF1D1A29),
                onSurface = androidx.compose.ui.graphics.Color(0xFFF2EDF8),
                surfaceVariant = androidx.compose.ui.graphics.Color(0xFF302A40),
                onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD0C6DD),
                outline = androidx.compose.ui.graphics.Color(0xFF8B7DA5)
            )
        } else {
            lightColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFF5B43C6),
                onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                primaryContainer = androidx.compose.ui.graphics.Color(0xFFE9E3FF),
                onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF25165D),
                secondary = androidx.compose.ui.graphics.Color(0xFFFF8A5B),
                onSecondary = androidx.compose.ui.graphics.Color(0xFF351306),
                secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE1D4),
                onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF52200C),
                tertiary = androidx.compose.ui.graphics.Color(0xFF178C82),
                onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFC8F4E9),
                onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF003B35),
                background = androidx.compose.ui.graphics.Color(0xFFFFF8F4),
                onBackground = androidx.compose.ui.graphics.Color(0xFF292536),
                surface = androidx.compose.ui.graphics.Color(0xFFFFFEFC),
                onSurface = androidx.compose.ui.graphics.Color(0xFF292536),
                surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF0ECF8),
                onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF615A70),
                outline = androidx.compose.ui.graphics.Color(0xFFB7ADC9)
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Vocab", fontWeight = FontWeight.Bold)
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
                    listOf("Trang chủ", "Sổ từ", "Lộ trình", "Ôn tập", "Thống kê").forEachIndexed { i, label ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = {
                                tab = i
                                if (i == 3) quizStartWithDue = true
                            },
                            icon = {
                                Icon(
                                    when (i) {
                                        0 -> Icons.Default.Home
                                        1 -> Icons.Default.MenuBook
                                        2 -> Icons.Default.School
                                        3 -> Icons.Default.Refresh
                                        else -> Icons.Default.QueryStats
                                    },
                                    contentDescription = label
                                )
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            when (tab) {
                0 -> LazyColumn(
                    Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { StudyReminderControl(onMessage = { csvMessage = it }) }
                    item {
                        DashboardOverview(
                            totalWords = words.size,
                            dueWords = words.count { it.nextReviewAtMillis == 0L || it.nextReviewAtMillis <= System.currentTimeMillis() },
                            learnedWords = words.count { it.reviewCount > 0 },
                            activeStreak = StudyActivityTracker.activeDayCount(statsPrefs),
                            todayActivities = StudyActivityTracker.countForDate(statsPrefs, java.time.LocalDate.now()),
                            dailyGoal = dailyGoal,
                            onGoalChange = { goal ->
                                dailyGoal = goal
                                settingsPrefs.edit().putInt("daily_goal", goal).apply()
                            },
                            onStartReview = {
                                quizStartWithDue = true
                                tab = if (words.isEmpty()) 1 else 3
                            },
                            onOpenPractice = {
                                quizStartWithDue = false
                                tab = 3
                            },
                            onOpenLibrary = { tab = 1 },
                            onOpenLearningPath = { tab = 2 },
                            onOpenStats = { tab = 4 }
                        )
                    }
                    item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("Ôn flashcard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); FilterChip(selected = dueOnly, onClick = { dueOnly = !dueOnly; currentIndex = 0; revealed = false }, label = { Text(if (dueOnly) "Chỉ từ đến hạn" else "Tất cả từ") }) } }
                    if (current == null) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(if (dueOnly && words.isNotEmpty()) "Không có từ đến hạn" else "Sổ từ đang trống", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(if (dueOnly && words.isNotEmpty()) "Bạn đã ôn hết từ đến hạn hiện tại. Có thể xem toàn bộ sổ từ hoặc quay lại sau." else "Bạn có thể tự nhập từ hoặc thêm vài từ mẫu để kiểm tra ứng dụng.")
                                    if (dueOnly && words.isNotEmpty()) OutlinedButton(onClick = { dueOnly = false; currentIndex = 0 }, modifier = Modifier.fillMaxWidth()) { Text("Xem toàn bộ từ") }
                                    Button(
                                        onClick = {
                                            importLauncher.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel"))
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Nhập từ bằng CSV") }
                                    OutlinedButton(onClick = { tab = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Mở sổ từ") }
                                    OutlinedButton(onClick = { showSamplesConfirm = true }, modifier = Modifier.fillMaxWidth()) { Text("Thêm 3 từ mẫu để thử") }
                                }
                            }
                        }
                    } else {
                        item {
                            Text("Thẻ ${currentIndex + 1} / ${reviewDeck.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Card(Modifier.fillMaxWidth().heightIn(min = 250.dp), shape = RoundedCornerShape(24.dp)) {
                                Column(
                                    Modifier.fillMaxWidth().padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(current.term, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                     OfflineSpeakButton(text = current.term)
                                    if (current.ipa.isNotBlank()) Text(current.ipa, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (current.partOfSpeech.isNotBlank()) Text(current.partOfSpeech, color = MaterialTheme.colorScheme.primary)
                                     ReviewWindowLabel(current)
                                    if (revealed) {
                                        Spacer(Modifier.height(18.dp))
                                        Text(current.meaning, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                        if (current.example.isNotBlank()) {
                                            Spacer(Modifier.height(10.dp))
                                            Text(current.example)
                                        }
                                        if (current.topic.isNotBlank()) Text("Chủ đề: ${current.topic}", style = MaterialTheme.typography.labelMedium)
                                        if (current.collocations.isNotBlank()) Text("Collocations: ${current.collocations}")
                                        if (current.synonyms.isNotBlank()) Text("Từ đồng nghĩa: ${current.synonyms}")
                                        if (current.wordFamily.isNotBlank()) Text("Họ từ: ${current.wordFamily}")
                                        if (current.tags.isNotBlank()) Text("Thẻ: ${current.tags}", style = MaterialTheme.typography.labelSmall)
                                        if (current.notes.isNotBlank()) Text("Ghi chú: ${current.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    OutlinedButton(onClick = { revealed = !revealed }, enabled = !ratingInProgress.value) {
                                        Text(if (revealed) "Ẩn đáp án" else "Hiện đáp án")
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { currentIndex = (currentIndex - 1 + reviewDeck.size) % reviewDeck.size; revealed = false },
                                    enabled = !ratingInProgress.value,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Trước") }
                                Button(
                                    onClick = { currentIndex = (currentIndex + 1) % reviewDeck.size; revealed = false },
                                    enabled = !ratingInProgress.value,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Tiếp theo") }
                            }
                            Text(if (revealed) "Đánh giá mức độ ghi nhớ" else "Hãy hiện đáp án trước khi đánh giá", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Lại" to ReviewRating.AGAIN, "Khó" to ReviewRating.HARD, "Tốt" to ReviewRating.GOOD, "Dễ" to ReviewRating.EASY).forEach { (label, rating) ->
                                    Button(
                                        onClick = {
                                            // Ignore rapid repeat taps before recording activity or calculating a second review.
                                            if (ratingInProgress.value) return@Button
                                            ratingInProgress.value = true
                                            val now = Instant.now()
                                            val state = ReviewState(
                                                level = current.srsLevel,
                                                nextReviewAt = current.nextReviewAtMillis.takeIf { it > 0L }?.let(Instant::ofEpochMilli),
                                                reviewCount = current.reviewCount,
                                                lapseCount = current.lapseCount
                                            )
                                            val next = SrsScheduler.review(state, rating, now)
                                            StudyActivityTracker.recordEvent(statsPrefs)
                                            scope.launch {
                                                try {
                                                    dao.update(current.copy(
                                                        srsLevel = next.level,
                                                        nextReviewAtMillis = next.nextReviewAt?.toEpochMilli() ?: 0L,
                                                        reviewCount = next.reviewCount,
                                                        lapseCount = next.lapseCount,
                                                        lastReviewedAtMillis = now.toEpochMilli()
                                                    ))
                                                    currentIndex = FlashcardLogic.nextIndexAfterRating(currentIndex, reviewDeck.size, dueOnly)
                                                    revealed = false
                                                } finally {
                                                    ratingInProgress.value = false
                                                }
                                            }
                                        },
                                        enabled = revealed && !ratingInProgress.value,
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) { Text(label, fontSize = 12.sp) }
                                }
                            }
                        }
                    }
                }
                1 -> Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text("📚", modifier = Modifier.padding(13.dp), fontSize = 28.sp)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Sổ tay từ vựng", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Bộ từ của riêng bạn · tìm, lọc và ôn tập", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), shape = RoundedCornerShape(14.dp)) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔤", fontSize = 18.sp)
                                    Text("${words.size}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("🔎  Tìm từ, nghĩa, ví dụ hoặc ghi chú…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("✨ Tất cả", "⏰ Đến hạn", "💪 Từ yếu", "🏷️ Có thẻ").forEach { option ->
                            FilterChip(
                                selected = libraryFilter == option,
                                onClick = { libraryFilter = option },
                                label = { Text(option) }
                            )
                        }
                    }
                    if (topics.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedTopic == "Mọi chủ đề",
                                onClick = { selectedTopic = "Mọi chủ đề" },
                                label = { Text("🌈 Mọi chủ đề") }
                            )
                            topics.forEach { topic ->
                                FilterChip(
                                    selected = selectedTopic.equals(topic, ignoreCase = true),
                                    onClick = { selectedTopic = topic },
                                    label = { Text(topic) }
                                )
                            }
                        }
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("📖  Đang hiển thị", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${filtered.size} / ${words.size} từ", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) },
                            modifier = Modifier.weight(1f)
                        ) { Text("📥  Nhập CSV") }
                        OutlinedButton(
                            onClick = { exportLauncher.launch("vocab-words.csv") },
                            modifier = Modifier.weight(1f)
                        ) { Text("📤  Xuất CSV") }
                    }
                    Text("CSV hỗ trợ cột bắt buộc term và meaning; các cột ipa, partOfSpeech, example, topic là tùy chọn.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BackupControls(dao = dao, onMessage = { csvMessage = it })
                    if (words.isEmpty()) {
                        Spacer(Modifier.height(18.dp))
                        Text("Chưa có từ nào. Nhấn nút + để nhập từ đầu tiên.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        items(filtered, key = { it.id }) { word ->
                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) {
                                            Text("🔤", modifier = Modifier.padding(10.dp), fontSize = 21.sp)
                                        }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(word.term, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface)
                                            if (word.ipa.isNotBlank()) Text("🔊  ${word.ipa}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { editorWord = word; showEditor = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Sửa từ")
                                        }
                                        IconButton(onClick = { wordToDelete = word }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Xóa từ")
                                        }
                                    }
                                    if (word.partOfSpeech.isNotBlank()) {
                                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(9.dp)) {
                                            Text("🏷️  ${word.partOfSpeech}", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                    }
                                    Text("🇻🇳  ${word.meaning}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    if (word.example.isNotBlank()) {
                                        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp)) {
                                            Text("💬  ${word.example}", Modifier.fillMaxWidth().padding(11.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (word.topic.isNotBlank()) Text("🌍  ${word.topic}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                                    if (word.collocations.isNotBlank()) Text("🧩  Cụm từ: ${word.collocations}", style = MaterialTheme.typography.bodySmall)
                                    if (word.synonyms.isNotBlank()) Text("🔁  Từ đồng nghĩa: ${word.synonyms}", style = MaterialTheme.typography.bodySmall)
                                    if (word.wordFamily.isNotBlank()) Text("🌱  Họ từ: ${word.wordFamily}", style = MaterialTheme.typography.bodySmall)
                                    if (word.tags.isNotBlank()) Text("🏷️  ${word.tags}", style = MaterialTheme.typography.labelSmall)
                                    if (word.notes.isNotBlank()) Text("📝  ${word.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (word.excludedFromReview) "Đã tạm bỏ khỏi lịch ôn" else "Đang nằm trong lịch ôn", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = if (word.excludedFromReview) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                                        TextButton(onClick = {
                                            scope.launch { dao.update(word.copy(excludedFromReview = !word.excludedFromReview)) }
                                        }) { Text(if (word.excludedFromReview) "Đưa lại vào ôn" else "Bỏ khỏi ôn") }
                                    }
                                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                                        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("🧠  SRS ${word.srsLevel}/5", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                            Text("🔁 ${word.reviewCount} lần", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> if (showPathLesson) {
                    Column(Modifier.fillMaxSize().padding(padding)) {
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
                            onReview = { word, rating ->
                                val now = Instant.now()
                                val state = ReviewState(
                                    level = word.srsLevel,
                                    nextReviewAt = word.nextReviewAtMillis.takeIf { it > 0L }?.let(Instant::ofEpochMilli),
                                    reviewCount = word.reviewCount,
                                    lapseCount = word.lapseCount
                                )
                                val next = SrsScheduler.review(state, rating, now)
                                StudyActivityTracker.recordEvent(statsPrefs)
                                scope.launch {
                                    dao.update(word.copy(
                                        srsLevel = next.level,
                                        nextReviewAtMillis = next.nextReviewAt?.toEpochMilli() ?: 0L,
                                        reviewCount = next.reviewCount,
                                        lapseCount = next.lapseCount,
                                        lastReviewedAtMillis = now.toEpochMilli()
                                    ))
                                }
                            }
                        )
                    }
                } else {
                    LearningPathScreen(
                        words = words,
                        modifier = Modifier.fillMaxSize().padding(padding),
                        sessionSize = lessonTarget,
                        onSessionSizeChange = { target ->
                            lessonTarget = target
                            settingsPrefs.edit().putInt("lesson_target", target).apply()
                        },
                        onStart = { topic -> learningPathTopic = topic; showPathLesson = true },
                        onPractice = { showPathLesson = false; quizStartWithDue = false; tab = 3 },
                        onOpenLibrary = { tab = 1 }
                    )
                }
                3 -> QuizScreen(
                    words = words,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    sessionScore = quizSessionCorrect,
                    sessionTotal = quizSessionTotal,
                    onAnswerRecorded = { word, correct, applySrs ->
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
                        if (applySrs) {
                            val now = Instant.now()
                            val previous = ReviewState(
                                level = word.srsLevel,
                                nextReviewAt = word.nextReviewAtMillis.takeIf { it > 0L }?.let(Instant::ofEpochMilli),
                                reviewCount = word.reviewCount,
                                lapseCount = word.lapseCount
                            )
                            val rating = if (correct) ReviewRating.GOOD else ReviewRating.AGAIN
                            val next = SrsScheduler.review(previous, rating, now)
                            scope.launch {
                                dao.update(word.copy(
                                    srsLevel = next.level,
                                    nextReviewAtMillis = next.nextReviewAt?.toEpochMilli() ?: 0L,
                                    reviewCount = next.reviewCount,
                                    lapseCount = next.lapseCount,
                                    lastReviewedAtMillis = now.toEpochMilli()
                                ))
                            }
                        }
                    },
                    onNewWordIntroduced = { word ->
                        scope.launch { dao.update(word.copy(isNew = false)) }
                    },
                    onSessionReset = {
                        quizSessionCorrect = 0
                        quizSessionTotal = 0
                        statsPrefs.edit()
                            .putInt("quiz_session_total", 0)
                            .putInt("quiz_session_correct", 0)
                            .apply()
                    },
                    startWithDue = quizStartWithDue
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val nowStats = System.currentTimeMillis()
                    val dueCount = words.count { it.nextReviewAtMillis == 0L || it.nextReviewAtMillis <= nowStats }
                    val learnedCount = words.count { it.reviewCount > 0 }
                    val masteredCount = words.count { it.srsLevel >= 4 }
                    val weakWords = words.filter { it.lapseCount > 0 || (it.reviewCount > 0 && it.srsLevel <= 1) }
                        .sortedWith(compareByDescending<WordEntity> { it.lapseCount }.thenBy { it.srsLevel }.thenBy { it.term.lowercase() }).take(5)
                    val week = StudyActivityTracker.lastSevenDays(statsPrefs)
                    val activeDays = week.count { it.activities > 0 }
                    val eventTotal = week.sumOf { it.activities }
                    val maxActivity = week.maxOfOrNull { it.activities }?.coerceAtLeast(1) ?: 1
                    val accuracy = if (quizTotal == 0) 0 else (quizCorrect * 100 / quizTotal)
                    val topicProgress = LearningPathLogic.topics(words, nowStats).take(6)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("BẢN ĐỒ TIẾN BỘ", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.ExtraBold)
                            Text("Thống kê học tập", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                            Text("Số liệu được tính từ hoạt động và sổ từ trên thiết bị này.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Tổng quan của bạn", style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text(if (words.isEmpty()) "Nhập từ để bắt đầu tạo dữ liệu" else "$learnedCount / ${words.size} từ đã được ôn",
                                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f))
                                    }
                                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary) {
                                        Text("✦", modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            color = MaterialTheme.colorScheme.onPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { if (words.isEmpty()) 0f else learnedCount.toFloat() / words.size },
                                    modifier = Modifier.fillMaxWidth().height(9.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    StatsMetric("📚", "Tổng từ", words.size.toString(), Modifier.weight(1f))
                                    StatsMetric("⏰", "Đến hạn", dueCount.toString(), Modifier.weight(1f))
                                    StatsMetric("🧠", "Vững", masteredCount.toString(), Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Độ chính xác khi luyện quiz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(if (quizTotal == 0) "—" else "$accuracy%",
                                        style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary)
                                    Column(Modifier.weight(1f).padding(bottom = 5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("$quizCorrect câu đúng / $quizTotal câu đã trả lời", style = MaterialTheme.typography.bodyMedium)
                                        Text(if (quizTotal == 0) "Chưa có dữ liệu — hãy thử một phiên quiz."
                                            else if (accuracy >= 80) "Độ chính xác đang tốt. Tiếp tục ôn đều để duy trì."
                                            else "Tập trung ôn lại các từ sai để cải thiện độ chính xác.",
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                LinearProgressIndicator(progress = { accuracy / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    StatsMini("Tổng lượt ôn", words.sumOf { it.reviewCount }.toString(), Modifier.weight(1f))
                                    StatsMini("Chuỗi hiện tại", "${StudyActivityTracker.activeDayCount(statsPrefs)} ngày", Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Nhịp học 7 ngày", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                        Text("$activeDays/7 ngày có hoạt động · $eventTotal lượt", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("▥", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                week.forEach { day ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                        Text(day.date.format(java.time.format.DateTimeFormatter.ofPattern("EEE")),
                                            modifier = Modifier.width(38.dp), style = MaterialTheme.typography.labelMedium)
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            LinearProgressIndicator(
                                                progress = { day.activities.toFloat() / maxActivity },
                                                modifier = Modifier.fillMaxWidth().height(9.dp),
                                                color = if (day.activities > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            Text(day.date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")),
                                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(day.activities.toString(), modifier = Modifier.width(25.dp),
                                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    }
                                }
                                HorizontalDivider()
                                Text("Mỗi lượt được ghi nhận khi bạn trả lời quiz hoặc đánh giá thẻ ôn tập.",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Trạng thái ghi nhớ", modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                    Text("SRS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                }
                                StatsDistributionRow("Chưa ôn lần nào", words.count { it.reviewCount == 0 }, words.size,
                                    MaterialTheme.colorScheme.outline)
                                StatsDistributionRow("Đang xây dựng", words.count { it.reviewCount > 0 && it.srsLevel in 1..3 }, words.size,
                                    MaterialTheme.colorScheme.primary)
                                StatsDistributionRow("Ghi nhớ vững", masteredCount, words.size,
                                    MaterialTheme.colorScheme.tertiary)
                                StatsDistributionRow("Cần ôn ngay", dueCount, words.size,
                                    MaterialTheme.colorScheme.secondary)
                                Text("Các nhóm có thể chồng lấp ở trạng thái đến hạn; số từ đến hạn được tính theo lịch ôn.",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Tiến độ theo chủ đề", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                if (topicProgress.isEmpty()) {
                                    Text("Chưa có dữ liệu chủ đề. Hãy thêm từ và gán chủ đề trong sổ từ; biểu đồ sẽ cập nhật tự động.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OutlinedButton(onClick = { tab = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Mở sổ từ") }
                                } else {
                                    topicProgress.forEach { topic ->
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(topic.topic, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                                Text("${topic.percent}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                                            }
                                            LinearProgressIndicator(progress = { topic.percent / 100f },
                                                modifier = Modifier.fillMaxWidth().height(7.dp))
                                            Text("${topic.learned}/${topic.total} đã ôn · ${topic.due} đến hạn · ${topic.weak} cần củng cố",
                                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (LearningPathLogic.topics(words, nowStats).size > topicProgress.size) {
                                        Text("Mở Lộ trình để xem toàn bộ ${LearningPathLogic.topics(words, nowStats).size} chủ đề.",
                                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        OutlinedButton(onClick = { tab = 2 }, modifier = Modifier.fillMaxWidth()) { Text("Xem toàn bộ lộ trình") }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Từ cần củng cố", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                Text("Ưu tiên các từ từng bị quên hoặc còn ở cấp ghi nhớ thấp.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (weakWords.isEmpty()) {
                                    Text(if (words.isEmpty()) "Sổ từ đang trống — thêm từ để bắt đầu." else "Chưa có từ yếu được ghi nhận. Hãy tiếp tục luyện tập để xác định từ cần củng cố.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    weakWords.forEach { word ->
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                                Text("↻", modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(word.term, fontWeight = FontWeight.Bold)
                                                Text(word.meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("SRS ${word.srsLevel}/5", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                Text("${word.lapseCount} lần quên", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                        HorizontalDivider()
                                    }
                                    OutlinedButton(onClick = { quizStartWithDue = false; tab = 3 }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Luyện tập ngay")
                                    }
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
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("🔒", fontSize = 22.sp)
                                Column {
                                    Text("Dữ liệu riêng tư, lưu trên thiết bị", fontWeight = FontWeight.Bold)
                                    Text("Thống kê hoạt động ngoại tuyến; không cần tài khoản hay đồng bộ đám mây.",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingCsvWords?.let { previewWords ->
        AlertDialog(
            onDismissRequest = { pendingCsvWords = null },
            title = { Text("Xác nhận nhập CSV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    pendingCsvWords = null
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) { dao.insertAll(wordsToInsert) }
                        }
                        csvMessage = if (result.isFailure) {
                            "Nhập file thất bại: ${result.exceptionOrNull()?.message ?: "lỗi dữ liệu"}"
                        } else {
                            "Đã nhập ${wordsToInsert.size} từ. Bỏ qua ${duplicates} từ trùng và ${invalidRows} dòng thiếu từ/nghĩa."
                        }
                    }
                }) { Text("Nhập ${previewWords.size} từ") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCsvWords = null }) { Text("Hủy") }
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
    var topic by remember(initial?.id) { mutableStateOf(initial?.topic.orEmpty()) }
    var collocations by remember(initial?.id) { mutableStateOf(initial?.collocations.orEmpty()) }
    var synonyms by remember(initial?.id) { mutableStateOf(initial?.synonyms.orEmpty()) }
    var wordFamily by remember(initial?.id) { mutableStateOf(initial?.wordFamily.orEmpty()) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    var tags by remember(initial?.id) { mutableStateOf(initial?.tags.orEmpty()) }
    var excludedFromReview by remember(initial?.id) { mutableStateOf(initial?.excludedFromReview ?: false) }
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
                OutlinedTextField(topic, { topic = it }, label = { Text("Chủ đề IELTS") }, singleLine = true)
                OutlinedTextField(collocations, { collocations = it }, label = { Text("Collocations / cụm từ đi kèm") }, minLines = 2)
                OutlinedTextField(synonyms, { synonyms = it }, label = { Text("Synonyms / từ đồng nghĩa") }, minLines = 2)
                OutlinedTextField(wordFamily, { wordFamily = it }, label = { Text("Word family / họ từ") }, minLines = 2)
                OutlinedTextField(tags, { tags = it }, label = { Text("Tags / thẻ (phân tách bằng dấu phẩy)") }, minLines = 2)
                OutlinedTextField(notes, { notes = it }, label = { Text("Ghi chú cá nhân") }, minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = excludedFromReview, onCheckedChange = { excludedFromReview = it })
                    Column(Modifier.weight(1f)) {
                        Text("Không đưa từ này vào lịch ôn")
                        Text("Từ vẫn được giữ trong sổ và có thể tìm kiếm; lịch sử ghi nhớ không bị xóa.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
                        topic = topic.trim(),
                        collocations = collocations.trim(),
                        synonyms = synonyms.trim(),
                        wordFamily = wordFamily.trim(),
                        notes = notes.trim(),
                        tags = tags.trim(),
                        excludedFromReview = excludedFromReview
                    ))
                }
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun StatsMetric(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(icon, fontSize = 19.sp)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatsMini(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatsDistributionRow(label: String, count: Int, total: Int, tint: androidx.compose.ui.graphics.Color) {
    val fraction = if (total == 0) 0f else (count.toFloat() / total).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("$count", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(8.dp),
            color = tint, trackColor = MaterialTheme.colorScheme.surfaceVariant)
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

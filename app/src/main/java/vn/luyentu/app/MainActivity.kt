package vn.luyentu.app

import android.os.Bundle
import android.os.Build
import android.net.Uri
import android.widget.Toast
import android.speech.tts.TextToSpeech
import java.util.Locale
import org.json.JSONObject
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LuyenTuApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LuyenTuApp() {
    val context = LocalContext.current
    val db = remember(context) { VocabularyDatabase(context) }
    val coroutineScope = rememberCoroutineScope()
    var words by remember { mutableStateOf(db.getAllWords()) }
    var dueWords by remember { mutableStateOf(db.getDueWords()) }
    var decks by remember { mutableStateOf(db.getDecks()) }
    var activeDeckId by rememberSaveable { mutableStateOf(db.getDecks().firstOrNull()?.id) }
    var reminderEnabled by remember { mutableStateOf(context.getSharedPreferences(DailyReminderWorker.PREFS, android.content.Context.MODE_PRIVATE).getBoolean(DailyReminderWorker.ENABLED_KEY, false)) }
    var dailyGoal by remember { mutableIntStateOf(context.getSharedPreferences(DailyReminderWorker.PREFS, android.content.Context.MODE_PRIVATE).getInt("daily_review_goal", 10)) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val refresh = {
        words = db.getAllWords()
        dueWords = db.getDueWords()
        decks = db.getDecks()
        if (activeDeckId != null && decks.none { it.id == activeDeckId }) activeDeckId = decks.firstOrNull()?.id
        LuyenTuWidgetProvider.updateAll(context)
    }
    val importCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val targetDeckId = activeDeckId
            coroutineScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                            InputStreamReader(stream, StandardCharsets.UTF_8).readText()
                        } ?: error("Không đọc được tệp.")
                        val parsed = CsvTools.parse(content)
                        val counts = db.importWords(parsed, targetDeckId)
                        Triple(parsed.size, counts.first, counts.second)
                    }
                    refresh()
                    val message = if (result.first == 0) {
                        "Tệp không có dòng từ vựng hợp lệ. Cần có từ tiếng Anh và nghĩa."
                    } else {
                        "Đã thêm ${result.second} từ; bỏ qua ${result.third} từ trùng/không hợp lệ."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Không thể nhập từ: " + (e.localizedMessage ?: "lỗi đọc tệp"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val exportTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        if (uri != null) {
            val targetDeckId = activeDeckId
            coroutineScope.launch {
                try {
                    val count = withContext(Dispatchers.IO) {
                        val selectedWords = db.getAllWords(deckId = targetDeckId)
                        val text = CsvTools.exportPlainText(selectedWords)
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            OutputStreamWriter(stream, StandardCharsets.UTF_8).use { writer -> writer.write(text) }
                        } ?: error("Không thể mở tệp để ghi.")
                        selectedWords.size
                    }
                    Toast.makeText(context, "Đã xuất $count từ dạng TXT/TSV.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Không thể xuất TXT: " + (e.localizedMessage ?: "lỗi ghi tệp"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            val savedGoal = dailyGoal
            val savedReminder = reminderEnabled
            coroutineScope.launch {
                try {
                    val counts = withContext(Dispatchers.IO) {
                        val backup = JSONObject(db.exportBackupJson())
                        backup.put("settings", JSONObject()
                            .put("dailyGoal", savedGoal)
                            .put("reminderEnabled", savedReminder))
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            OutputStreamWriter(stream, StandardCharsets.UTF_8).use { it.write(backup.toString(2)) }
                        } ?: error("Không thể mở tệp để ghi.")
                        db.wordCount() to db.reviewCount()
                    }
                    Toast.makeText(context, "Đã sao lưu ${counts.first} từ và ${counts.second} lượt ôn.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Sao lưu thất bại: " + (e.localizedMessage ?: "lỗi không xác định"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val backup = context.contentResolver.openInputStream(uri)?.use {
                            InputStreamReader(it, StandardCharsets.UTF_8).readText()
                        } ?: error("Không đọc được tệp.")
                        val root = JSONObject(backup)
                        val counts = db.restoreBackupJson(backup)
                        val settings = root.optJSONObject("settings")
                        val restoredGoal = settings?.optInt("dailyGoal", 10)?.coerceIn(5, 20)
                        val restoredReminder = settings?.optBoolean("reminderEnabled", false)
                        Triple(counts, restoredGoal, restoredReminder)
                    }
                    result.second?.let { dailyGoal = it }
                    result.third?.let { reminderEnabled = it }
                    if (result.second != null || result.third != null) {
                        context.getSharedPreferences(DailyReminderWorker.PREFS, android.content.Context.MODE_PRIVATE).edit().apply {
                            result.second?.let { putInt("daily_review_goal", it) }
                            result.third?.let { putBoolean(DailyReminderWorker.ENABLED_KEY, it) }
                        }.apply()
                        if (reminderEnabled) DailyReminderWorker.schedule(context) else DailyReminderWorker.cancel(context)
                    }
                    refresh()
                    Toast.makeText(context, "Khôi phục thành công: ${result.first.first} từ, ${result.first.second} lượt ôn.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Khôi phục thất bại: " + (e.localizedMessage ?: "tệp không hợp lệ"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) {
            val targetDeckId = activeDeckId
            coroutineScope.launch {
                try {
                    val count = withContext(Dispatchers.IO) {
                        val selectedWords = db.getAllWords(deckId = targetDeckId)
                        val csv = CsvTools.export(selectedWords)
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            OutputStreamWriter(stream, StandardCharsets.UTF_8).use { writer -> writer.write(csv) }
                        } ?: error("Không thể mở tệp để ghi.")
                        selectedWords.size
                    }
                    Toast.makeText(context, "Đã xuất $count từ dạng CSV.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Không thể xuất CSV: " + (e.localizedMessage ?: "lỗi ghi tệp"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val reminderPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            context.getSharedPreferences(DailyReminderWorker.PREFS, android.content.Context.MODE_PRIVATE)
                .edit().putBoolean(DailyReminderWorker.ENABLED_KEY, true).apply()
            DailyReminderWorker.schedule(context)
            reminderEnabled = true
            Toast.makeText(context, "Đã bật nhắc ôn hằng ngày lúc 20:00.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Bạn chưa cấp quyền thông báo nên nhắc ôn chưa được bật.", Toast.LENGTH_LONG).show()
        }
    }
    val enableReminder: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            reminderPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.getSharedPreferences(DailyReminderWorker.PREFS, android.content.Context.MODE_PRIVATE)
                .edit().putBoolean(DailyReminderWorker.ENABLED_KEY, true).apply()
            DailyReminderWorker.schedule(context)
            reminderEnabled = true
            Toast.makeText(context, "Đã bật nhắc ôn hằng ngày lúc 20:00.", Toast.LENGTH_SHORT).show()
        }
    }
    val disableReminder: () -> Unit = {
        context.getSharedPreferences(DailyReminderWorker.PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean(DailyReminderWorker.ENABLED_KEY, false).apply()
        DailyReminderWorker.cancel(context)
        reminderEnabled = false
        Toast.makeText(context, "Đã tắt nhắc ôn hằng ngày.", Toast.LENGTH_SHORT).show()
    }
    val updateDailyGoal: (Int) -> Unit = { goal ->
        context.getSharedPreferences(DailyReminderWorker.PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putInt("daily_review_goal", goal).apply()
        dailyGoal = goal
        Toast.makeText(context, "Đã đặt mục tiêu ${goal} lượt ôn/ngày.", Toast.LENGTH_SHORT).show()
    }
    val activeWords = if (activeDeckId == null) words else words.filter { it.deckId == activeDeckId }
    val activeDueWords = if (activeDeckId == null) dueWords else dueWords.filter { it.deckId == activeDeckId }
    val masteredCount = db.masteredWordCount()
    val masteryPercent = if (words.isEmpty()) 0 else (masteredCount * 100 / words.size).coerceIn(0, 100)
    val titles = listOf("Trang chủ", "Bộ từ", "Luyện tập", "Thống kê", "Cá nhân", "Lộ trình")
    val icons = listOf(Icons.Default.Home, Icons.Default.AutoStories, Icons.Default.School, Icons.Default.BarChart, Icons.Default.Settings, Icons.Default.Route)

    val luyenTuColors = darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF35D07F),
        onPrimary = androidx.compose.ui.graphics.Color(0xFF07130D),
        secondary = androidx.compose.ui.graphics.Color(0xFF22B8F0),
        tertiary = androidx.compose.ui.graphics.Color(0xFFB76BFF),
        background = androidx.compose.ui.graphics.Color(0xFF111722),
        surface = androidx.compose.ui.graphics.Color(0xFF1B2431),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF273243),
        onSurface = androidx.compose.ui.graphics.Color(0xFFF3F5F8),
        onBackground = androidx.compose.ui.graphics.Color(0xFFF3F5F8)
    )
    MaterialTheme(colorScheme = luyenTuColors) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Column {
                    Text(if (selected == 0) "Luyện Từ" else titles.getOrElse(selected) { "Luyện Từ" }, fontWeight = FontWeight.Bold)
                    Text("HỌC TỪ VỰNG • OFFLINE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }
            ) },
            bottomBar = { NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                titles.forEachIndexed { i, title ->
                    NavigationBarItem(selected = selected == i, onClick = { selected = i; refresh() },
                        icon = { Icon(icons[i], contentDescription = title) }, label = { Text(title, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant))
                }
            } }
        ) { padding ->
            when (selected) {
                0 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Metric("Tổng số từ", words.size.toString(), Modifier.weight(1f))
                        Metric("Đã thuộc", masteredCount.toString(), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Metric("Tiến độ", "$masteryPercent%", Modifier.weight(1f))
                        Metric("Đến hạn ôn", dueWords.size.toString(), Modifier.weight(1f))
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF263D36))) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text("Ôn tập ngắt quãng (SRS)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text("Ôn đúng thời điểm để ghi nhớ từ lâu hơn.", color = androidx.compose.ui.graphics.Color(0xFFD4E9DF))
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { selected = 2; refresh() }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (dueWords.isEmpty()) "Bắt đầu luyện tập" else "Ôn ngay • ${dueWords.size} từ đến hạn")
                            }
                        }
                    }
                    Text("LỐI TẮT LUYỆN TẬP", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardAction("Flashcard", "Lật thẻ và ghi nhớ", androidx.compose.ui.graphics.Color(0xFF646DE8), Modifier.weight(1f)) { selected = 2 }
                        DashboardAction("Trắc nghiệm", "Chọn nghĩa đúng", androidx.compose.ui.graphics.Color(0xFFFF8A16), Modifier.weight(1f)) { selected = 2 }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardAction("Sổ từ", "Quản lý bộ từ", androidx.compose.ui.graphics.Color(0xFF168FCA), Modifier.weight(1f)) { selected = 1 }
                        DashboardAction("Thống kê", "Theo dõi tiến độ", androidx.compose.ui.graphics.Color(0xFF8C43D8), Modifier.weight(1f)) { selected = 3 }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Metric("Chuỗi học", "${db.currentStreakDays()} ngày", Modifier.weight(1f))
                        Metric("Hôm nay", "${db.reviewedToday()}/${dailyGoal}", Modifier.weight(1f))
                    }
                    Text("Dữ liệu được lưu trên thiết bị. Không cần tài khoản hoặc kết nối mạng.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                1 -> VocabularyScreen(
                    padding, words, decks, activeDeckId, { newId -> activeDeckId = newId }, db, refresh,
                    onImport = { importCsv.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) },
                    onExport = { exportCsv.launch(if (activeDeckId == null) "LuyenTu-all-words.csv" else "LuyenTu-deck-${activeDeckId}.csv") },
                    onExportText = { exportTxt.launch(if (activeDeckId == null) "LuyenTu-all-words.txt" else "LuyenTu-deck-${activeDeckId}.txt") }
                )
                2 -> PracticeScreen(padding, activeDueWords, activeWords, db, refresh)
                3 -> StatisticsScreen(padding, activeWords, activeDueWords, db)
                4 -> SettingsScreen(
                    padding,
                    onImport = { importCsv.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) },
                    onExport = { exportCsv.launch("LuyenTu-vocabulary.csv") },
                    onExportText = { exportTxt.launch("LuyenTu-vocabulary.txt") },
                    onBackup = { exportBackup.launch("LuyenTu-backup.json") },
                    onRestore = { importBackup.launch(arrayOf("application/json", "text/*", "*/*")) },
                    reminderEnabled = reminderEnabled,
                    onEnableReminder = enableReminder,
                    onDisableReminder = disableReminder,
                    dailyGoal = dailyGoal,
                    onDailyGoalChange = updateDailyGoal
                )
                5 -> RoadmapScreen(padding, words, db)
                else -> SettingsScreen(
                    padding,
                    onImport = { importCsv.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) },
                    onExport = { exportCsv.launch("LuyenTu-vocabulary.csv") },
                    onExportText = { exportTxt.launch("LuyenTu-vocabulary.txt") },
                    onBackup = { exportBackup.launch("LuyenTu-backup.json") },
                    onRestore = { importBackup.launch(arrayOf("application/json", "text/*", "*/*")) },
                    reminderEnabled = reminderEnabled,
                    onEnableReminder = enableReminder,
                    onDisableReminder = disableReminder,
                    dailyGoal = dailyGoal,
                    onDailyGoalChange = updateDailyGoal
                )
            }
        }
    }
}

@Composable
private fun DashboardAction(title: String, subtitle: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.heightIn(min = 112.dp), onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.White)
        }
    }
}

@Composable
private fun VocabularyScreen(
    padding: PaddingValues,
    words: List<VocabularyWord>,
    decksFromApp: List<VocabularyDeck>,
    activeDeckId: Long?,
    onActiveDeckChange: (Long?) -> Unit,
    db: VocabularyDatabase,
    refresh: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onExportText: () -> Unit
) {
    var term by rememberSaveable { mutableStateOf("") }
    var meaning by rememberSaveable { mutableStateOf("") }
    var ipa by rememberSaveable { mutableStateOf("") }
    var example by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedTopic by rememberSaveable { mutableStateOf("Tất cả") }
    var message by rememberSaveable { mutableStateOf("") }
    var editingWord by remember { mutableStateOf<VocabularyWord?>(null) }
    var editTerm by remember(editingWord?.id) { mutableStateOf(editingWord?.term.orEmpty()) }
    var editMeaning by remember(editingWord?.id) { mutableStateOf(editingWord?.meaning.orEmpty()) }
    var editIpa by remember(editingWord?.id) { mutableStateOf(editingWord?.ipa.orEmpty()) }
    var editTopic by remember(editingWord?.id) { mutableStateOf(editingWord?.topic.orEmpty()) }
    var editExample by remember(editingWord?.id) { mutableStateOf(editingWord?.example.orEmpty()) }
    var editError by remember { mutableStateOf("") }
    var addWordDialog by rememberSaveable { mutableStateOf(false) }
    var viewingDeckContent by rememberSaveable { mutableStateOf(false) }
    var deckSearch by rememberSaveable { mutableStateOf("") }
    var decks by remember { mutableStateOf(decksFromApp) }
    var folders by remember { mutableStateOf(db.getFolders()) }
    LaunchedEffect(decksFromApp) { decks = decksFromApp }
    var createDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var newContainerName by rememberSaveable { mutableStateOf("") }
    var selectedFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deckMenuExpanded by remember { mutableStateOf(false) }
    var moveDeckMenuExpanded by remember { mutableStateOf(false) }
    var movingWordId by remember { mutableStateOf<Long?>(null) }
    var confirmDeleteDeck by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    val deckWords = if (activeDeckId == null) words else words.filter { it.deckId == activeDeckId }
    val topics = deckWords.map { it.topic.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    val filtered = deckWords.filter {
        (search.isBlank() || it.term.contains(search, true) || it.meaning.contains(search, true) || it.topic.contains(search, true)) &&
            (selectedTopic == "Tất cả" || it.topic == selectedTopic)
    }

    if (!viewingDeckContent) {
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bộ từ của tôi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(decks.size.toString() + " bộ từ • " + folders.size + " thư mục",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { createDialog = "folder"; newContainerName = "" }) { Icon(Icons.Default.CreateNewFolder, "Tạo thư mục") }
                IconButton(onClick = { createDialog = "deck"; newContainerName = ""; selectedFolderId = null }) { Icon(Icons.Default.AddBox, "Tạo bộ từ") }
            }
            OutlinedTextField(
                value = deckSearch, onValueChange = { deckSearch = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Tìm bộ từ hoặc thư mục") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                onClick = { onActiveDeckChange(null); viewingDeckContent = true }
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.CollectionsBookmark, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Tất cả từ vựng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(words.size.toString() + " từ • Xem toàn bộ từ trong thư viện", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
            val visibleDecks = decks.filter { deck ->
                deck.name.contains(deckSearch, true) ||
                    folders.any { folder -> folder.id == deck.folderId && folder.name.contains(deckSearch, true) }
            }
            val unfiledDecks = visibleDecks.filter { it.folderId == null }
            if (unfiledDecks.isNotEmpty()) {
                Text("BỘ TỪ CỦA BẠN", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                unfiledDecks.forEach { deck ->
                    LibraryDeckCard(
                        deck = deck, folderName = null,
                        onOpen = { onActiveDeckChange(deck.id); viewingDeckContent = true },
                        onRename = { onActiveDeckChange(deck.id); createDialog = "renameDeck"; newContainerName = deck.name },
                        onDelete = { onActiveDeckChange(deck.id); confirmDeleteDeck = true }
                    )
                }
            }
            folders.forEach { folder ->
                val folderDecks = visibleDecks.filter { it.folderId == folder.id }
                if (folder.name.contains(deckSearch, true) || folderDecks.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Text(folder.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { editingFolderId = folder.id; createDialog = "renameFolder"; newContainerName = folder.name }) { Icon(Icons.Default.Edit, "Đổi tên thư mục") }
                        IconButton(onClick = { confirmDeleteFolderId = folder.id }) { Icon(Icons.Default.Delete, "Xóa thư mục") }
                    }
                    if (folderDecks.isEmpty()) {
                        Text("Chưa có bộ từ trong thư mục này.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        folderDecks.forEach { deck ->
                            LibraryDeckCard(
                                deck = deck, folderName = folder.name,
                                onOpen = { onActiveDeckChange(deck.id); viewingDeckContent = true },
                                onRename = { onActiveDeckChange(deck.id); createDialog = "renameDeck"; newContainerName = deck.name },
                                onDelete = { onActiveDeckChange(deck.id); confirmDeleteDeck = true }
                            )
                        }
                    }
                }
            }
            if (visibleDecks.isEmpty() && deckSearch.isNotBlank()) Text("Không tìm thấy bộ từ hoặc thư mục.")
            Text("Chọn một bộ từ để xem, thêm và luyện tập các từ trong bộ đó.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = { viewingDeckContent = false }) { Icon(Icons.Default.ArrowBack, "Quay lại bộ từ") }
            Column(Modifier.weight(1f)) {
                Text(if (activeDeckId == null) "Tất cả từ vựng" else db.deckName(activeDeckId), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${deckWords.size} từ • ${if (activeDeckId == null) "tất cả bộ từ" else db.deckName(activeDeckId)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onImport) { Icon(Icons.Default.FileOpen, "Nhập từ vào bộ") }
            IconButton(onClick = onExport) { Icon(Icons.Default.FileDownload, "Xuất bộ từ CSV") }
            IconButton(onClick = onExportText) { Icon(Icons.Default.TextSnippet, "Xuất bộ từ TXT/TSV") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box {
                OutlinedButton(onClick = { deckMenuExpanded = true }) {
                    Text(if (activeDeckId == null) "Tất cả bộ từ" else db.deckName(activeDeckId), maxLines = 1)
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = deckMenuExpanded, onDismissRequest = { deckMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Tất cả bộ từ") }, onClick = {
                        onActiveDeckChange(null); deckMenuExpanded = false
                    })
                    decks.forEach { deck ->
                        DropdownMenuItem(text = { Text("${deck.name} (${deck.wordCount})") }, onClick = {
                            onActiveDeckChange(deck.id); deckMenuExpanded = false
                        })
                    }
                }
            }
            if (activeDeckId != null) {
                Box {
                    IconButton(onClick = { moveDeckMenuExpanded = true }) { Icon(Icons.Default.FolderOpen, "Chuyển thư mục") }
                    DropdownMenu(expanded = moveDeckMenuExpanded, onDismissRequest = { moveDeckMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Không thư mục") }, onClick = {
                            db.moveDeckToFolder(activeDeckId, null)
                            moveDeckMenuExpanded = false
                            decks = db.getDecks()
                            refresh()
                        })
                        folders.forEach { folder ->
                            DropdownMenuItem(text = { Text(folder.name) }, onClick = {
                                db.moveDeckToFolder(activeDeckId, folder.id)
                                moveDeckMenuExpanded = false
                                decks = db.getDecks()
                                refresh()
                            })
                        }
                    }
                }
                IconButton(onClick = { createDialog = "renameDeck"; newContainerName = db.deckName(activeDeckId) }) {
                    Icon(Icons.Default.Edit, "Đổi tên bộ từ")
                }
                IconButton(onClick = { confirmDeleteDeck = true }) { Icon(Icons.Default.Delete, "Xóa bộ từ") }
            }
        }
        if (folders.isNotEmpty()) {
            Text("THƯ MỤC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(folders, key = { it.id }) { folder ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        AssistChip(onClick = {
                            editingFolderId = folder.id
                            createDialog = "renameFolder"
                            newContainerName = folder.name
                        }, label = { Text(folder.name) }, leadingIcon = { Icon(Icons.Default.Folder, null) })
                        IconButton(onClick = { confirmDeleteFolderId = folder.id }) {
                            Icon(Icons.Default.Close, "Xóa thư mục ${folder.name}")
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { addWordDialog = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Thêm từ")
            }
            OutlinedButton(onClick = { search = "" }) { Icon(Icons.Default.FilterListOff, null); Spacer(Modifier.width(4.dp)); Text("Xóa lọc") }
        }
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Tìm từ") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(selected = selectedTopic == "Tất cả", onClick = { selectedTopic = "Tất cả" }, label = { Text("Tất cả") }) }
            items(topics) { itemTopic ->
                FilterChip(selected = selectedTopic == itemTopic, onClick = { selectedTopic = itemTopic }, label = { Text(itemTopic) })
            }
        }
        Text("${filtered.size} từ", style = MaterialTheme.typography.labelLarge)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(filtered, key = { it.id }) { word ->
                Card { Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 2.dp)) {
                    Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                        Text(word.term, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (word.ipa.isNotBlank()) Text(word.ipa, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(word.meaning)
                        if (word.example.isNotBlank()) Text(word.example, style = MaterialTheme.typography.bodySmall)
                        if (word.topic.isNotBlank()) Text(word.topic, style = MaterialTheme.typography.labelSmall)
                        if (word.repetitions > 0) Text("Đã ôn ${word.repetitions} lần", style = MaterialTheme.typography.labelSmall)
                    }
                    Box {
                        IconButton(onClick = { movingWordId = word.id }) { Icon(Icons.Default.DriveFileMove, "Chuyển bộ từ") }
                        DropdownMenu(expanded = movingWordId == word.id, onDismissRequest = { movingWordId = null }) {
                            decks.filter { it.id != word.deckId }.forEach { deck ->
                                DropdownMenuItem(text = { Text(deck.name) }, onClick = {
                                    db.assignWordToDeck(word.id, deck.id)
                                    movingWordId = null
                                    decks = db.getDecks()
                                    refresh()
                                })
                            }
                        }
                    }
                    IconButton(onClick = { editingWord = word; editError = "" }) { Icon(Icons.Default.Edit, "Sửa từ") }
                    IconButton(onClick = { db.deleteWord(word.id); refresh() }) { Icon(Icons.Default.Delete, "Xoá từ") }
                } }
            }
            if (filtered.isEmpty()) item { Text(if (search.isBlank()) "Chưa có từ trong bộ này. Nhấn Thêm từ để bắt đầu." else "Không tìm thấy từ.") }
        }
    }
    }
    if (addWordDialog) {
        AlertDialog(
            onDismissRequest = { addWordDialog = false },
            title = { Text("Thêm từ vào bộ") },
            text = {
                Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(term, { term = it; message = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Từ tiếng Anh *") }, singleLine = true)
                    OutlinedTextField(meaning, { meaning = it; message = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Nghĩa tiếng Việt *") })
                    OutlinedTextField(ipa, { ipa = it }, modifier = Modifier.fillMaxWidth(), label = { Text("IPA / phiên âm") }, singleLine = true)
                    OutlinedTextField(topic, { topic = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Chủ đề / tag") }, singleLine = true)
                    OutlinedTextField(example, { example = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Câu ví dụ") })
                    Text("Bộ đích: ${if (activeDeckId == null) "Bộ từ mặc định" else db.deckName(activeDeckId)}",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    message = when {
                        term.isBlank() || meaning.isBlank() -> "Cần nhập từ và nghĩa."
                        db.addWord(term, meaning, example, ipa, topic, activeDeckId) -> {
                            term = ""; meaning = ""; example = ""; ipa = ""; topic = ""
                            addWordDialog = false
                            refresh()
                            "Đã lưu từ trên thiết bị."
                        }
                        else -> "Từ đã tồn tại hoặc dữ liệu không hợp lệ."
                    }
                }) { Text("Lưu từ") }
            },
            dismissButton = { TextButton(onClick = { addWordDialog = false }) { Text("Hủy") } }
        )
    }
    editingWord?.let { word ->
        AlertDialog(
            onDismissRequest = { editingWord = null },
            title = { Text("Chỉnh sửa từ vựng") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(editTerm, { editTerm = it }, label = { Text("Từ tiếng Anh *") }, singleLine = true)
                    OutlinedTextField(editMeaning, { editMeaning = it }, label = { Text("Nghĩa tiếng Việt *") })
                    OutlinedTextField(editIpa, { editIpa = it }, label = { Text("IPA") }, singleLine = true)
                    OutlinedTextField(editTopic, { editTopic = it }, label = { Text("Chủ đề") }, singleLine = true)
                    OutlinedTextField(editExample, { editExample = it }, label = { Text("Câu ví dụ") })
                    if (editError.isNotBlank()) Text(editError, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (db.updateWord(word.id, editTerm, editMeaning, editExample, editIpa, editTopic)) {
                        editingWord = null
                        refresh()
                    } else {
                        editError = "Cần nhập từ và nghĩa; từ tiếng Anh không được trùng."
                    }
                }) { Text("Lưu thay đổi") }
            },
            dismissButton = { TextButton(onClick = { editingWord = null }) { Text("Huỷ") } }
        )
    }
    if (createDialog == "folder" || createDialog == "deck" || createDialog == "renameDeck" || createDialog == "renameFolder") {
        AlertDialog(
            onDismissRequest = { createDialog = null },
            title = { Text(when (createDialog) {
                "folder" -> "Tạo thư mục"
                "renameDeck" -> "Đổi tên bộ từ"
                "renameFolder" -> "Đổi tên thư mục"
                else -> "Tạo bộ từ mới"
            }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newContainerName,
                        onValueChange = { newContainerName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (createDialog == "folder" || createDialog == "renameFolder") "Tên thư mục" else "Tên bộ từ") },
                        singleLine = true
                    )
                    if (createDialog == "deck" && folders.isNotEmpty()) {
                        Text("Lưu trong thư mục", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item { FilterChip(selected = selectedFolderId == null, onClick = { selectedFolderId = null }, label = { Text("Không thư mục") }) }
                            items(folders, key = { it.id }) { folder ->
                                FilterChip(selected = selectedFolderId == folder.id, onClick = { selectedFolderId = folder.id }, label = { Text(folder.name) })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val ok = when (createDialog) {
                        "folder" -> db.createFolder(newContainerName) != null
                        "renameDeck" -> activeDeckId?.let { db.renameDeck(it, newContainerName) } == true
                        "renameFolder" -> editingFolderId?.let { db.renameFolder(it, newContainerName) } == true
                        else -> db.createDeck(newContainerName, selectedFolderId) != null
                    }
                    if (ok) {
                        if (createDialog == "deck") onActiveDeckChange(db.getDecks().lastOrNull()?.id)
                        decks = db.getDecks()
                        folders = db.getFolders()
                        createDialog = null
                        newContainerName = ""
                        refresh()
                    }
                }, enabled = newContainerName.isNotBlank()) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { createDialog = null }) { Text("Hủy") } }
        )
    }
    if (confirmDeleteFolderId != null) {
        val folderIdToDelete = confirmDeleteFolderId
        AlertDialog(
            onDismissRequest = { confirmDeleteFolderId = null },
            title = { Text("Xóa thư mục?") },
            text = { Text("Các bộ từ trong thư mục vẫn được giữ và chuyển sang trạng thái không thuộc thư mục.") },
            confirmButton = {
                TextButton(onClick = {
                    folderIdToDelete?.let { db.deleteFolder(it) }
                    folders = db.getFolders()
                    decks = db.getDecks()
                    confirmDeleteFolderId = null
                    refresh()
                }) { Text("Xóa thư mục") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteFolderId = null }) { Text("Hủy") } }
        )
    }
    if (confirmDeleteDeck && activeDeckId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDeck = false },
            title = { Text("Xóa bộ từ?") },
            text = { Text("Bộ từ sẽ bị xóa khỏi danh sách. Các từ sẽ không bị xóa mà trở thành từ chưa phân bộ.") },
            confirmButton = {
                TextButton(onClick = {
                    val removedId = activeDeckId
                    if (removedId != null && db.deleteDeck(removedId)) {
                        decks = db.getDecks()
                        onActiveDeckChange(decks.firstOrNull()?.id)
                        refresh()
                    }
                    confirmDeleteDeck = false
                }) { Text("Xóa bộ từ") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteDeck = false }) { Text("Hủy") } }
        )
    }
}


@Composable
private fun LibraryDeckCard(
    deck: VocabularyDeck,
    folderName: String?,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(Icons.Default.AutoStories, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(deck.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(deck.wordCount.toString() + " từ • " + deck.dueCount + " đến hạn",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!folderName.isNullOrBlank()) Text(folderName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                Text("Xem bộ từ  ›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onRename) { Icon(Icons.Default.Edit, "Đổi tên bộ từ") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Xóa bộ từ") }
        }
    }
}

@Composable
private fun SettingsScreen(padding: PaddingValues, onImport: () -> Unit, onExport: () -> Unit, onExportText: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit, reminderEnabled: Boolean, onEnableReminder: () -> Unit, onDisableReminder: () -> Unit, dailyGoal: Int, onDailyGoalChange: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Cài đặt & dữ liệu", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Nhập và xuất bộ từ bằng CSV hoặc TXT/TSV. Dữ liệu được nhập vào bộ nhớ cục bộ của ứng dụng; không cần đăng nhập.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mục tiêu ôn tập", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Đặt số lượt ôn mong muốn mỗi ngày.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 20).forEach { goal ->
                        FilterChip(selected = dailyGoal == goal, onClick = { onDailyGoalChange(goal) }, label = { Text("$goal lượt") })
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nhắc ôn hằng ngày", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(if (reminderEnabled) "Đang bật • 20:00 mỗi ngày" else "Nhận thông báo trên thiết bị lúc 20:00. Không gửi dữ liệu ra ngoài.")
                if (reminderEnabled) {
                    OutlinedButton(onClick = onDisableReminder, modifier = Modifier.fillMaxWidth()) { Text("Tắt nhắc ôn") }
                } else {
                    Button(onClick = onEnableReminder, modifier = Modifier.fillMaxWidth()) { Text("Bật nhắc ôn") }
                }
            }
        }
        Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FileOpen, null)
            Spacer(Modifier.width(8.dp))
            Text("Nhập từ file CSV/TXT")
        }
        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FileDownload, null)
            Spacer(Modifier.width(8.dp))
            Text("Xuất bộ từ ra CSV")
        }
        OutlinedButton(onClick = onExportText, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.TextSnippet, null)
            Spacer(Modifier.width(8.dp))
            Text("Xuất bộ từ ra TXT/TSV")
        }
        HorizontalDivider()
        Text("Sao lưu toàn bộ dữ liệu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Bao gồm từ vựng, lịch ôn, lịch sử ôn và mục tiêu/cài đặt nhắc nhở. Khôi phục sẽ thay thế dữ liệu hiện tại.")
        Button(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Backup, null)
            Spacer(Modifier.width(8.dp))
            Text("Tạo bản sao lưu JSON")
        }
        OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Restore, null)
            Spacer(Modifier.width(8.dp))
            Text("Khôi phục bản sao lưu")
        }
        HorizontalDivider()
        Text("Định dạng CSV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Tiêu đề cột được hỗ trợ: term, meaning, ipa, topic, example. Một số tên tương đương như word, translation, definition và sentence cũng được nhận diện.")
        Text("Mỗi từ cần có từ tiếng Anh và nghĩa. Từ trùng sẽ được bỏ qua, không ghi đè dữ liệu đang có.")
        Text("TXT hỗ trợ một từ mỗi dòng theo dạng term - meaning hoặc TSV với các cột term, meaning, example, ipa, topic. Nếu chọn vị trí lưu trữ đám mây trong bộ chọn Android, việc chuyển tệp qua dịch vụ đó có thể cần Internet. Bản thân ứng dụng không cần mạng để học và quản lý từ.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PracticeScreen(
    padding: PaddingValues,
    dueWords: List<VocabularyWord>,
    allWords: List<VocabularyWord>,
    db: VocabularyDatabase,
    refresh: () -> Unit
) {
    val context = LocalContext.current
    // Six user-facing modes; cloze is a challenge inside Special mode.
    val modes = listOf("Thẻ từ", "Trắc nghiệm", "Nối từ", "Gõ từ vựng", "Nghe viết", "Đặc biệt")
    var speechEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var speechReady by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val languageStatus = engine.setLanguage(Locale.US)
                speechReady = languageStatus != TextToSpeech.LANG_MISSING_DATA && languageStatus != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
        speechEngine = engine
        onDispose { engine.stop(); engine.shutdown(); speechEngine = null; speechReady = false }
    }
    var mode by rememberSaveable { mutableStateOf("Thẻ từ") }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var sessionLimit by rememberSaveable { mutableIntStateOf(20) }
    var sessionTotal by remember { mutableIntStateOf(0) }
    var sessionStarted by remember { mutableStateOf(false) }
    var sessionIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var revealed by rememberSaveable { mutableStateOf(false) }
    var typedAnswer by rememberSaveable { mutableStateOf("") }
    var chosenAnswer by rememberSaveable { mutableStateOf<String?>(null) }
    var answered by rememberSaveable { mutableStateOf(false) }
    var feedback by rememberSaveable { mutableStateOf("") }
    var feedbackCorrect by rememberSaveable { mutableStateOf(false) }
    var selectedPairTermId by rememberSaveable { mutableStateOf<Long?>(null) }
    var matchedPairIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pairMistakeIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pairFeedback by rememberSaveable { mutableStateOf("") }
    val sessionWords = if (sessionStarted) sessionIds.mapNotNull { id -> dueWords.firstOrNull { it.id == id } } else emptyList()
    val safeIndex = index.coerceIn(0, (sessionWords.size - 1).coerceAtLeast(0))
    val current = sessionWords.getOrNull(safeIndex)
    val activeMode = remember(current?.id, mode) {
        if (mode == "Đặc biệt") listOf("Thẻ từ", "Trắc nghiệm", "Nghe viết", "Điền từ", "Nối từ", "Gõ từ vựng").random()
        else mode
    }
    val choices = remember(current?.id, allWords.size, activeMode) {
        if (current == null) {
            emptyList()
        } else {
            // Meanings can differ only by case or whitespace; normalize before de-duplicating
            // so a multiple-choice question never shows visually equivalent answers twice.
            fun meaningKey(value: String) = value.trim().lowercase().replace(Regex("\\s+"), " ")
            val correctMeaning = current.meaning.trim()
            val correctKey = meaningKey(correctMeaning)
            val distractors = allWords
                .map { it.meaning.trim() }
                .filter { it.isNotBlank() && meaningKey(it) != correctKey }
                .distinctBy { meaningKey(it) }
                .shuffled()
                .take(3)
            (distractors + correctMeaning).shuffled()
        }
    }
    val matchBatch = if (activeMode == "Nối từ") sessionWords.take(4) else emptyList()
    val shuffledMatchTerms = remember(matchBatch.map { it.id }) { matchBatch.shuffled() }
    val shuffledMatchMeanings = remember(matchBatch.map { it.id }) { matchBatch.shuffled() }

    LaunchedEffect(current?.id, activeMode, mode) {
        revealed = false
        typedAnswer = ""
        chosenAnswer = null
        answered = false
        feedback = ""
        feedbackCorrect = false
        selectedPairTermId = null
        matchedPairIds = emptySet()
        pairMistakeIds = emptySet()
        pairFeedback = ""
    }

    fun normalize(value: String) = value.trim().lowercase().replace(Regex("\\s+"), " ").trimEnd('.', '!', '?')
    fun recordGrade(grade: Int) {
        val word = current ?: return
        db.reviewWord(word.id, grade)
        sessionIds = sessionIds.filterNot { it == word.id }
        refresh()
        index = 0
        revealed = false
        typedAnswer = ""
        chosenAnswer = null
        answered = false
        feedback = ""
    }
    fun recordAnswer(correct: Boolean) = recordGrade(if (correct) 2 else 0)

    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Luyện tập", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Chọn dạng bài tập", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            modes.take(3).forEach { item ->
                FilterChip(selected = mode == item, onClick = { mode = item; index = 0 }, label = { Text(item) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            modes.drop(3).forEach { item ->
                FilterChip(selected = mode == item, onClick = { mode = item; index = 0 }, label = { Text(item) })
            }
        }
        if (sessionStarted) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Còn ${sessionWords.size} / $sessionTotal câu trong phiên", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { sessionStarted = false; sessionIds = emptyList(); index = 0 }) { Text("Kết thúc phiên") }
            }
        } else {
            Text("Còn ${dueWords.size} từ đến hạn ôn.")
        }
        if (!sessionStarted) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Thiết lập phiên học", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Chọn số từ tối đa cho phiên này. Danh sách từ được cố định khi bạn bắt đầu.")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10, 20, 30, 50).forEach { amount ->
                            FilterChip(selected = sessionLimit == amount, onClick = { sessionLimit = amount }, label = { Text("$amount từ") })
                        }
                    }
                    Text("Phiên này sẽ có tối đa ${minOf(sessionLimit, dueWords.size)} từ.", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = {
                            sessionIds = dueWords.shuffled().take(sessionLimit).map { it.id }
                            sessionTotal = sessionIds.size
                            index = 0
                            sessionStarted = sessionIds.isNotEmpty()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = dueWords.isNotEmpty()
                    ) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Bắt đầu phiên học") }
                    if (dueWords.isEmpty()) Text("Không có từ đến hạn trong bộ đang chọn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (current == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                    Text("Hoàn thành phiên học!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Bạn đã xử lý đủ ${sessionTotal} câu đã chọn. Các từ sai đã được lên lịch ôn lại.")
                    Button(onClick = { sessionStarted = false; sessionIds = emptyList(); sessionTotal = 0 }, modifier = Modifier.fillMaxWidth()) { Text("Thiết lập phiên tiếp theo") }
                }
            }
        } else {
            Text("Từ ${sessionTotal - sessionWords.size + 1} / $sessionTotal  •  Dạng bài: ${if (mode == "Đặc biệt") "Đặc biệt · $activeMode" else activeMode}", style = MaterialTheme.typography.labelLarge)
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (activeMode) {
                        "Thẻ từ" -> {
                            Text(current.term, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            if (current.ipa.isNotBlank()) Text(current.ipa, color = MaterialTheme.colorScheme.primary)
                            if (revealed) {
                                HorizontalDivider()
                                Text(current.meaning, style = MaterialTheme.typography.titleLarge)
                                if (current.example.isNotBlank()) Text(current.example)
                                if (current.topic.isNotBlank()) Text("Chủ đề: ${current.topic}", style = MaterialTheme.typography.labelMedium)
                            } else {
                                Text("Tự nhớ nghĩa và cách dùng trước khi xem đáp án.")
                                Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) { Text("Hiện đáp án") }
                            }
                        }
                        "Trắc nghiệm" -> {
                            Text(current.term, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            if (current.ipa.isNotBlank()) Text(current.ipa, color = MaterialTheme.colorScheme.primary)
                            if (choices.size < 2) {
                                Text(
                                    "Chưa đủ nghĩa khác nhau để tạo câu trắc nghiệm. Hãy nhập thêm từ vựng có nghĩa khác nhau, hoặc chuyển sang Thẻ từ / Gõ từ vựng.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text("Chọn nghĩa chính xác:")
                                choices.forEach { choice ->
                                    OutlinedButton(
                                        onClick = { if (!answered) { chosenAnswer = choice; answered = true; feedbackCorrect = choice == current.meaning; feedback = if (feedbackCorrect) "Chính xác!" else "Đáp án đúng: ${current.meaning}" } },
                                        modifier = Modifier.fillMaxWidth(), enabled = !answered,
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (answered && choice == current.meaning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                    ) { Text(choice) }
                                }
                            }
                        }
                        "Nối từ" -> {
                            Text("Nối từng từ tiếng Anh với nghĩa tương ứng.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (matchBatch.size < 2) {
                                Text("Cần ít nhất 2 từ trong phiên để chơi ghép cặp.")
                                Button(onClick = { mode = "Thẻ từ" }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Chuyển từ còn lại sang Thẻ từ")
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.Top) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("TỪ VỰNG", style = MaterialTheme.typography.labelSmall)
                                        shuffledMatchTerms.forEach { pair ->
                                            OutlinedButton(
                                                onClick = { if (pair.id !in matchedPairIds) { selectedPairTermId = pair.id; pairFeedback = "" } },
                                                enabled = pair.id !in matchedPairIds,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = when {
                                                        pair.id in matchedPairIds -> MaterialTheme.colorScheme.primaryContainer
                                                        selectedPairTermId == pair.id -> MaterialTheme.colorScheme.secondaryContainer
                                                        else -> MaterialTheme.colorScheme.surface
                                                    }
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                            ) { Text(if (pair.id in matchedPairIds) "✓ ${pair.term}" else pair.term) }
                                        }
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("NGHĨA", style = MaterialTheme.typography.labelSmall)
                                        shuffledMatchMeanings.forEach { pair ->
                                            OutlinedButton(
                                                onClick = {
                                                    if (selectedPairTermId != null && pair.id !in matchedPairIds) {
                                                        if (selectedPairTermId == pair.id) {
                                                            val nextMatched = matchedPairIds + pair.id
                                                            matchedPairIds = nextMatched
                                                            pairFeedback = "Chính xác: ${pair.term}"
                                                            selectedPairTermId = null
                                                            if (nextMatched.containsAll(matchBatch.map { it.id })) {
                                                                val matchedIds = matchBatch.map { it.id }.toSet()
                                                                matchBatch.forEach { matchedWord ->
                                                                    db.reviewWord(matchedWord.id, if (matchedWord.id in pairMistakeIds) 0 else 2)
                                                                }
                                                                sessionIds = sessionIds.filterNot { it in matchedIds }
                                                                refresh()
                                                                matchedPairIds = emptySet()
                                                                pairMistakeIds = emptySet()
                                                                pairFeedback = "Hoàn thành phiên ghép cặp!"
                                                            }
                                                        } else {
                                                            pairMistakeIds = pairMistakeIds + selectedPairTermId!! + pair.id
                                                            pairFeedback = "Chưa khớp — hãy thử lại."
                                                            selectedPairTermId = null
                                                        }
                                                    }
                                                },
                                                enabled = pair.id !in matchedPairIds,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (pair.id in matchedPairIds) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                            ) { Text(if (pair.id in matchedPairIds) "✓ ${pair.meaning}" else pair.meaning) }
                                        }
                                    }
                                }
                                if (selectedPairTermId != null) Text("Đã chọn: ${matchBatch.firstOrNull { it.id == selectedPairTermId }?.term ?: ""}. Chọn nghĩa ở cột bên phải.")
                                if (pairFeedback.isNotBlank()) Text(pairFeedback, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        "Nghe viết", "Gõ từ vựng" -> {
                            if (activeMode == "Nghe viết") {
                                Text("Nghe và nhập từ tiếng Anh", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Hãy nghe âm thanh và nhập từ tiếng Anh bạn nghe được.")
                                Button(onClick = { speechEngine?.speak(current.term, TextToSpeech.QUEUE_FLUSH, null, "luyentu-word-${current.id}") }, modifier = Modifier.fillMaxWidth(), enabled = speechReady) {
                                    Icon(Icons.Default.VolumeUp, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (speechReady) "Phát âm từ" else "Đang khởi tạo giọng đọc")
                                }
                                Text("Muốn phát âm hoàn toàn offline, hãy cài sẵn giọng tiếng Anh ngoại tuyến trong cài đặt Android.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("Dịch nghĩa sang tiếng Anh", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(current.meaning, style = MaterialTheme.typography.headlineSmall)
                                if (current.example.isNotBlank()) Text("Gợi ý ngữ cảnh: ${current.example}")
                            }
                            OutlinedTextField(typedAnswer, { typedAnswer = it; feedback = "" }, Modifier.fillMaxWidth(), label = { Text("Từ tiếng Anh") }, singleLine = true)
                            Button(onClick = {
                                if (typedAnswer.isNotBlank() && !answered) {
                                    answered = true
                                    feedbackCorrect = normalize(typedAnswer) == normalize(current.term)
                                    feedback = if (feedbackCorrect) "Chính xác!" else "Đáp án đúng: ${current.term}"
                                }
                            }, modifier = Modifier.fillMaxWidth(), enabled = !answered && typedAnswer.isNotBlank()) { Text("Kiểm tra") }
                        }
                        "Điền từ" -> {
                            val sentenceHasTerm = current.example.contains(current.term, ignoreCase = true)
                            val sentence = if (sentenceHasTerm) current.example.replace(Regex(Regex.escape(current.term), RegexOption.IGNORE_CASE), "______") else "Nghĩa tiếng Việt: ${current.meaning}"
                            Text("Hoàn thành câu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(sentence, style = MaterialTheme.typography.bodyLarge)
                            if (!sentenceHasTerm) Text("Chưa có câu ví dụ chứa từ này; hãy nhập từ tiếng Anh tương ứng.")
                            OutlinedTextField(typedAnswer, { typedAnswer = it; feedback = "" }, Modifier.fillMaxWidth(), label = { Text("Từ còn thiếu") }, singleLine = true)
                            Button(onClick = {
                                if (typedAnswer.isNotBlank() && !answered) {
                                    answered = true
                                    feedbackCorrect = normalize(typedAnswer) == normalize(current.term)
                                    feedback = if (feedbackCorrect) "Chính xác!" else "Đáp án đúng: ${current.term}"
                                }
                            }, modifier = Modifier.fillMaxWidth(), enabled = !answered && typedAnswer.isNotBlank()) { Text("Kiểm tra") }
                        }
                        else -> Unit
                    }
                    if (feedback.isNotBlank()) {
                        Text(feedback, color = if (feedbackCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        if (activeMode != "Thẻ từ") {
                            Button(onClick = { recordAnswer(feedbackCorrect) }, modifier = Modifier.fillMaxWidth()) { Text("Lưu kết quả & tiếp tục") }
                        }
                    }
                }
            }
            if (activeMode == "Thẻ từ" && revealed) {
                Text("Bạn nhớ từ này đến đâu?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { recordGrade(0) }, modifier = Modifier.weight(1f)) { Text("Quên") }
                    OutlinedButton(onClick = { recordGrade(1) }, modifier = Modifier.weight(1f)) { Text("Khó") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { recordGrade(2) }, modifier = Modifier.weight(1f)) { Text("Nhớ") }
                    Button(onClick = { recordGrade(3) }, modifier = Modifier.weight(1f)) { Text("Dễ") }
                }
            }
        }
    }
}

@Composable
private fun StatisticsScreen(
    padding: PaddingValues,
    words: List<VocabularyWord>,
    dueWords: List<VocabularyWord>,
    db: VocabularyDatabase
) {
    val daily = remember(db.reviewCount()) { db.dailyReviewCounts() }
    val history = remember(db.reviewCount()) { db.recentReviewHistory(20) }
    val maxDaily = (daily.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val dateFormat = remember { java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()) }
    var wordSearch by rememberSaveable { mutableStateOf("") }
    var wordFilter by rememberSaveable { mutableStateOf("Tất cả") }
    val dueIds = dueWords.map { it.id }.toSet()
    val filteredWords = words.filter { word ->
        (wordSearch.isBlank() || word.term.contains(wordSearch, true) || word.meaning.contains(wordSearch, true)) &&
        when (wordFilter) {
            "Đến hạn" -> word.id in dueIds
            "Đang học" -> word.learningLevel < 3
            "Đã vững" -> word.learningLevel >= 3
            else -> true
        }
    }
    val levelCounts = (0..5).map { level -> level to words.count { it.learningLevel == level } }
    val maxLevelCount = (levelCounts.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val levelColors = listOf(
        androidx.compose.ui.graphics.Color(0xFF8794A6),
        androidx.compose.ui.graphics.Color(0xFFFFA24A),
        androidx.compose.ui.graphics.Color(0xFFF2C744),
        androidx.compose.ui.graphics.Color(0xFF35BDEB),
        androidx.compose.ui.graphics.Color(0xFFBB83F6),
        androidx.compose.ui.graphics.Color(0xFF45D18A)
    )
    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        item { Text("Thống kê học tập", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("Tổng số từ", words.size.toString(), Modifier.weight(1f))
                Metric("Đến hạn", dueWords.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("Lượt ôn hôm nay", db.reviewedToday().toString(), Modifier.weight(1f))
                Metric("Từ đã vững", db.masteredWordCount().toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("Lần quên", db.totalLapses().toString(), Modifier.weight(1f))
                Metric("Chuỗi học", "${db.currentStreakDays()} ngày", Modifier.weight(1f))
            }
        }
        item { Text("Phân bố từ vựng theo cấp độ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    levelCounts.forEach { (level, count) ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Lv $level", modifier = Modifier.width(38.dp), style = MaterialTheme.typography.labelSmall)
                            LinearProgressIndicator(
                                progress = count.toFloat() / maxLevelCount,
                                modifier = Modifier.weight(1f).height(8.dp),
                                color = levelColors[level],
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(count.toString(), modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        item { Text("Hoạt động 7 ngày gần nhất", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        items(daily) { (day, count) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(day, modifier = Modifier.width(42.dp), style = MaterialTheme.typography.labelMedium)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    LinearProgressIndicator(progress = count.toFloat() / maxDaily, modifier = Modifier.fillMaxWidth().height(8.dp))
                    Text("$count lượt ôn", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item { Text("Danh sách từ đang học (${filteredWords.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item {
            OutlinedTextField(
                value = wordSearch,
                onValueChange = { wordSearch = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tìm từ...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("Tất cả", "Đến hạn", "Đang học", "Đã vững")) { filter ->
                    FilterChip(selected = wordFilter == filter, onClick = { wordFilter = filter }, label = { Text(filter) })
                }
            }
        }
        if (filteredWords.isEmpty()) {
            item { Text("Không có từ nào phù hợp với bộ lọc hiện tại.", style = MaterialTheme.typography.bodySmall) }
        } else {
            items(filteredWords, key = { it.id }) { word ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(word.term, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(word.meaning, style = MaterialTheme.typography.bodySmall)
                            Text("Lv ${word.learningLevel} • ${if (word.id in dueIds) "Đến hạn" else if (word.learningLevel >= 3) "Đã vững" else "Đang học"}",
                                style = MaterialTheme.typography.labelSmall, color = levelColors[word.learningLevel])
                        }
                        Text("${word.intervalDays} ngày", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item { Text("Lịch sử ôn gần đây", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        if (history.isEmpty()) {
            item { Text("Chưa có lịch sử. Hoàn thành một lượt ôn để bắt đầu ghi nhận tiến độ.") }
        } else {
            items(history) { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.term, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                when (entry.grade) { 0 -> "Quên"; 1 -> "Khó"; 2 -> "Nhớ"; else -> "Dễ" },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (entry.grade == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(dateFormat.format(java.util.Date(entry.reviewedAt)), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private data class RoadmapMilestone(val title: String, val detail: String, val progress: Int, val target: Int)

@Composable
private fun RoadmapScreen(padding: PaddingValues, words: List<VocabularyWord>, db: VocabularyDatabase) {
    val milestones = listOf(
        RoadmapMilestone("Bắt đầu bộ từ", "Thêm hoặc nhập 20 từ vựng đầu tiên.", words.size, 20),
        RoadmapMilestone("Xây thói quen ôn", "Hoàn thành 100 lượt ôn chủ động.", db.reviewCount(), 100),
        RoadmapMilestone("Củng cố trí nhớ", "Đạt 10 từ đã vững theo lịch ôn.", db.masteredWordCount(), 10),
        RoadmapMilestone("Duy trì đều đặn", "Học liên tục 7 ngày để tạo chuỗi học.", db.currentStreakDays(), 7)
    )
    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp)
    ) {
        item { Text("Lộ trình học cá nhân", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text("Các cột mốc chạy hoàn toàn trên thiết bị. Bạn có thể tự nhập bộ từ; không cần tài khoản hoặc gói trả phí.") }
        items(milestones) { milestone ->
            val done = milestone.progress >= milestone.target
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (done) Icons.Default.CheckCircle else Icons.Default.Flag, null, tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(milestone.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Text(milestone.detail, style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(progress = (milestone.progress.toFloat() / milestone.target).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth())
                    Text("${milestone.progress.coerceAtMost(milestone.target)} / ${milestone.target}" + if (done) " • Hoàn thành" else "", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(16.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    } }
}

@Composable
private fun SimpleScreen(padding: PaddingValues, title: String, description: String) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodyLarge)
        Text("Màn hình chưa hoàn chỉnh.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    var words by remember { mutableStateOf(db.getAllWords()) }
    var dueWords by remember { mutableStateOf(db.getDueWords()) }
    var reminderEnabled by remember { mutableStateOf(context.getSharedPreferences(DailyReminderWorker.PREFS, android.content.Context.MODE_PRIVATE).getBoolean(DailyReminderWorker.ENABLED_KEY, false)) }
    var dailyGoal by remember { mutableIntStateOf(context.getSharedPreferences(DailyReminderWorker.PREFS, android.content.Context.MODE_PRIVATE).getInt("daily_review_goal", 10)) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val refresh = {
        words = db.getAllWords()
        dueWords = db.getDueWords()
        LuyenTuWidgetProvider.updateAll(context)
    }
    val importCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val csv = context.contentResolver.openInputStream(uri)?.use { stream ->
                    InputStreamReader(stream, StandardCharsets.UTF_8).readText()
                }.orEmpty()
                val parsed = CsvTools.parse(csv)
                var added = 0
                parsed.forEach { row -> if (db.addWord(row.term, row.meaning, row.example, row.ipa, row.topic)) added++ }
                refresh()
                Toast.makeText(context, "Đã nhập $added/${parsed.size} từ mới từ CSV/TXT. Từ trùng được bỏ qua.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Không thể nhập CSV: " + (e.localizedMessage ?: "lỗi đọc tệp"), Toast.LENGTH_LONG).show()
            }
        }
    }
    val exportTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream, StandardCharsets.UTF_8).use { writer -> writer.write(CsvTools.exportPlainText(db.getAllWords())) }
                }
                Toast.makeText(context, "Đã xuất bộ từ dạng TXT.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Không thể xuất TXT: " + (e.localizedMessage ?: "lỗi ghi tệp"), Toast.LENGTH_LONG).show()
            }
        }
    }
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            try {
                val backup = JSONObject(db.exportBackupJson())
                backup.put("settings", JSONObject()
                    .put("dailyGoal", dailyGoal)
                    .put("reminderEnabled", reminderEnabled))
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream, StandardCharsets.UTF_8).use { it.write(backup.toString(2)) }
                }
                Toast.makeText(context, "Đã sao lưu ${words.size} từ và ${db.reviewCount()} lượt ôn.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Sao lưu thất bại: " + (e.localizedMessage ?: "lỗi không xác định"), Toast.LENGTH_LONG).show()
            }
        }
    }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val backup = context.contentResolver.openInputStream(uri)?.use {
                    InputStreamReader(it, StandardCharsets.UTF_8).readText()
                } ?: error("Không đọc được tệp.")
                val root = JSONObject(backup)
                val counts = db.restoreBackupJson(backup)
                val settings = root.optJSONObject("settings")
                if (settings != null) {
                    dailyGoal = settings.optInt("dailyGoal", 10).coerceIn(5, 20)
                    reminderEnabled = settings.optBoolean("reminderEnabled", false)
                    context.getSharedPreferences(DailyReminderWorker.PREFS, android.content.Context.MODE_PRIVATE).edit()
                        .putInt("daily_review_goal", dailyGoal)
                        .putBoolean(DailyReminderWorker.ENABLED_KEY, reminderEnabled).apply()
                    if (reminderEnabled) DailyReminderWorker.schedule(context) else DailyReminderWorker.cancel(context)
                }
                refresh()
                Toast.makeText(context, "Khôi phục thành công: ${counts.first} từ, ${counts.second} lượt ôn.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Khôi phục thất bại: " + (e.localizedMessage ?: "tệp không hợp lệ"), Toast.LENGTH_LONG).show()
            }
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream, StandardCharsets.UTF_8).use { writer -> writer.write(CsvTools.export(db.getAllWords())) }
                }
                Toast.makeText(context, "Đã xuất bộ từ CSV.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Không thể xuất CSV: " + (e.localizedMessage ?: "lỗi ghi tệp"), Toast.LENGTH_LONG).show()
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
    val titles = listOf("Tổng quan", "Sổ từ", "Luyện tập", "Thống kê", "Cài đặt", "Lộ trình")
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
                0 -> Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("TỔNG SỐ TỪ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(words.size.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("ĐẾN HẠN ÔN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(dueWords.size.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFFFFB547))
                            }
                        }
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
                1 -> VocabularyScreen(padding, words, db, refresh)
                2 -> PracticeScreen(padding, dueWords, words, db, refresh)
                3 -> StatisticsScreen(padding, words, dueWords, db)
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
private fun VocabularyScreen(padding: PaddingValues, words: List<VocabularyWord>, db: VocabularyDatabase, refresh: () -> Unit) {
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
    val topics = words.map { it.topic.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    val filtered = words.filter {
        (search.isBlank() || it.term.contains(search, true) || it.meaning.contains(search, true) || it.topic.contains(search, true)) &&
            (selectedTopic == "Tất cả" || it.topic == selectedTopic)
    }

    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Sổ từ của bạn", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(term, { term = it; message = "" }, Modifier.fillMaxWidth(), label = { Text("Từ tiếng Anh *") }, singleLine = true)
        OutlinedTextField(meaning, { meaning = it; message = "" }, Modifier.fillMaxWidth(), label = { Text("Nghĩa tiếng Việt *") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(ipa, { ipa = it }, Modifier.weight(1f), label = { Text("IPA") }, singleLine = true)
            OutlinedTextField(topic, { topic = it }, Modifier.weight(1f), label = { Text("Chủ đề") }, singleLine = true)
        }
        OutlinedTextField(example, { example = it }, Modifier.fillMaxWidth(), label = { Text("Câu ví dụ (tuỳ chọn)") })
        Button(onClick = {
            message = when {
                term.isBlank() || meaning.isBlank() -> "Cần nhập từ và nghĩa."
                db.addWord(term, meaning, example, ipa, topic) -> {
                    term = ""; meaning = ""; example = ""; ipa = ""; topic = ""; refresh()
                    "Đã lưu trên thiết bị."
                }
                else -> "Từ đã tồn tại hoặc dữ liệu không hợp lệ."
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Lưu từ") }
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
                    IconButton(onClick = { editingWord = word; editError = "" }) { Icon(Icons.Default.Edit, "Sửa từ") }
                    IconButton(onClick = { db.deleteWord(word.id); refresh() }) { Icon(Icons.Default.Delete, "Xoá từ") }
                } }
            }
            if (filtered.isEmpty()) item { Text(if (search.isBlank()) "Chưa có từ. Hãy thêm từ đầu tiên ở biểu mẫu trên." else "Không tìm thấy từ.") }
        }
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
    val modes = listOf("Thẻ từ", "Trắc nghiệm", "Nghe & gõ", "Điền từ", "Ghép cặp", "Dịch từ", "Ôn hỗn hợp")
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
    var revealed by rememberSaveable { mutableStateOf(false) }
    var typedAnswer by rememberSaveable { mutableStateOf("") }
    var chosenAnswer by rememberSaveable { mutableStateOf<String?>(null) }
    var answered by rememberSaveable { mutableStateOf(false) }
    var feedback by rememberSaveable { mutableStateOf("") }
    var feedbackCorrect by rememberSaveable { mutableStateOf(false) }
    val safeIndex = index.coerceIn(0, (dueWords.size - 1).coerceAtLeast(0))
    val current = dueWords.getOrNull(safeIndex)
    val activeMode = remember(current?.id, mode) {
        if (mode == "Ôn hỗn hợp") listOf("Thẻ từ", "Trắc nghiệm", "Nghe & gõ", "Điền từ", "Ghép cặp", "Dịch từ").random()
        else mode
    }
    val choices = remember(current?.id, allWords.size, activeMode) {
        if (current == null) emptyList()
        else if (activeMode == "Ghép cặp") {
            (allWords.map { it.term }.filter { it.isNotBlank() && !it.equals(current.term, true) }.distinct().shuffled().take(3) + current.term).shuffled()
        } else {
            (allWords.map { it.meaning }.filter { it.isNotBlank() && it != current.meaning }.distinct().shuffled().take(3) + current.meaning).shuffled()
        }
    }

    LaunchedEffect(current?.id, activeMode, mode) {
        revealed = false
        typedAnswer = ""
        chosenAnswer = null
        answered = false
        feedback = ""
        feedbackCorrect = false
    }

    fun normalize(value: String) = value.trim().lowercase().replace(Regex("\\s+"), " ").trimEnd('.', '!', '?')
    fun recordAnswer(correct: Boolean) {
        if (current == null) return
        db.reviewWord(current.id, if (correct) 2 else 0)
        refresh()
        index = 0
        revealed = false
        typedAnswer = ""
        chosenAnswer = null
        answered = false
        feedback = ""
    }

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
        Text("Còn ${dueWords.size} từ đến hạn ôn.")
        if (current == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                    Text(if (allWords.isEmpty()) "Sổ từ đang trống" else "Bạn đã ôn hết các từ đến hạn!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (allWords.isEmpty()) "Hãy thêm từ hoặc nhập bộ từ CSV trong mục Sổ từ/Cài đặt để bắt đầu." else "Thêm từ mới hoặc quay lại khi tới lịch ôn tiếp theo.")
                }
            }
        } else {
            Text("Từ ${safeIndex + 1} / ${dueWords.size}  •  Dạng bài: $activeMode", style = MaterialTheme.typography.labelLarge)
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
                            Text("Chọn nghĩa chính xác:")
                            choices.forEach { choice ->
                                OutlinedButton(
                                    onClick = { if (!answered) { chosenAnswer = choice; answered = true; feedbackCorrect = choice == current.meaning; feedback = if (feedbackCorrect) "Chính xác!" else "Đáp án đúng: ${current.meaning}" } },
                                    modifier = Modifier.fillMaxWidth(), enabled = !answered,
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (answered && choice == current.meaning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                ) { Text(choice) }
                            }
                        }
                        "Ghép cặp" -> {
                            Text(current.meaning, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Chọn từ tiếng Anh khớp với nghĩa:")
                            choices.forEach { choice ->
                                OutlinedButton(
                                    onClick = { if (!answered) { chosenAnswer = choice; answered = true; feedbackCorrect = choice.equals(current.term, true); feedback = if (feedbackCorrect) "Đã ghép đúng!" else "Đáp án đúng: ${current.term}" } },
                                    modifier = Modifier.fillMaxWidth(), enabled = !answered,
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (answered && choice.equals(current.term, true)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                ) { Text(choice) }
                            }
                        }
                        "Nghe & gõ", "Dịch từ" -> {
                            if (activeMode == "Nghe & gõ") {
                                Text("Nghe và nhập từ tiếng Anh", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Nghĩa: ${current.meaning}")
                                Text("IPA: ${current.ipa.ifBlank { "Chưa có IPA" }}", style = MaterialTheme.typography.bodySmall)
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
                    OutlinedButton(onClick = { db.reviewWord(current.id, 0); refresh(); index = 0 }, modifier = Modifier.weight(1f)) { Text("Quên") }
                    OutlinedButton(onClick = { db.reviewWord(current.id, 1); refresh(); index = 0 }, modifier = Modifier.weight(1f)) { Text("Khó") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { db.reviewWord(current.id, 2); refresh(); index = 0 }, modifier = Modifier.weight(1f)) { Text("Nhớ") }
                    Button(onClick = { db.reviewWord(current.id, 3); refresh(); index = 0 }, modifier = Modifier.weight(1f)) { Text("Dễ") }
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

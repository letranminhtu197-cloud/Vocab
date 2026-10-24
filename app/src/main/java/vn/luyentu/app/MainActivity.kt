package vn.luyentu.app

import android.os.Bundle
import android.net.Uri
import android.widget.Toast
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val refresh = {
        words = db.getAllWords()
        dueWords = db.getDueWords()
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
                Toast.makeText(context, "Đã nhập $added/${parsed.size} từ mới. Từ trùng được bỏ qua.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Không thể nhập CSV: " + (e.localizedMessage ?: "lỗi đọc tệp"), Toast.LENGTH_LONG).show()
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
    val titles = listOf("Tổng quan", "Sổ từ", "Luyện tập", "Thống kê", "Cài đặt")
    val icons = listOf(Icons.Default.Home, Icons.Default.AutoStories, Icons.Default.School, Icons.Default.BarChart, Icons.Default.Settings)

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Column {
                Text("LuyenTu 2.0", fontWeight = FontWeight.Bold)
                Text("Học từ vựng cá nhân • Offline", style = MaterialTheme.typography.labelMedium)
            } }) },
            bottomBar = { NavigationBar {
                titles.forEachIndexed { i, title ->
                    NavigationBarItem(selected == i, { selected = i; refresh() },
                        icon = { Icon(icons[i], contentDescription = title) }, label = { Text(title) })
                }
            } }
        ) { padding ->
            when (selected) {
                0 -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("Không tài khoản. Không quảng cáo. Không máy chủ.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Từ vựng và tiến độ được lưu cục bộ trên thiết bị. Bạn tự quản lý bộ từ của mình.")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric("Tổng số từ", words.size.toString(), Modifier.weight(1f))
                        Metric("Đến hạn ôn", dueWords.size.toString(), Modifier.weight(1f))
                    }
                    Button(onClick = { selected = 2; refresh() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Bắt đầu ôn tập")
                    }
                    Text("Tiến độ và lịch ôn được lưu trên điện thoại; không cần kết nối mạng.")
                    Text("Các phần nhập/xuất bộ từ, bài tập đa dạng và thống kê chi tiết vẫn đang được phát triển.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                1 -> VocabularyScreen(padding, words, db, refresh)
                2 -> PracticeScreen(padding, dueWords, words, db, refresh)
                3 -> Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Thống kê", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric("Tổng số từ", words.size.toString(), Modifier.weight(1f))
                        Metric("Lượt ôn", db.reviewCount().toString(), Modifier.weight(1f))
                    }
                    Metric("Đến hạn ôn", dueWords.size.toString(), Modifier.fillMaxWidth())
                    Text("Số liệu được tính từ cơ sở dữ liệu trên thiết bị.")
                }
                else -> SettingsScreen(padding, onImport = { importCsv.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) }, onExport = { exportCsv.launch("LuyenTu-vocabulary.csv") })
            }
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
    var message by rememberSaveable { mutableStateOf("") }
    val filtered = words.filter { search.isBlank() || it.term.contains(search, true) || it.meaning.contains(search, true) || it.topic.contains(search, true) }

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
                    IconButton(onClick = { db.deleteWord(word.id); refresh() }) { Icon(Icons.Default.Delete, "Xoá từ") }
                } }
            }
            if (filtered.isEmpty()) item { Text(if (search.isBlank()) "Chưa có từ. Hãy thêm từ đầu tiên ở biểu mẫu trên." else "Không tìm thấy từ.") }
        }
    }
}


@Composable
private fun SettingsScreen(padding: PaddingValues, onImport: () -> Unit, onExport: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Cài đặt & dữ liệu", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Quản lý bộ từ bằng tệp CSV. Dữ liệu được nhập vào bộ nhớ cục bộ của ứng dụng; không cần đăng nhập.")
        Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FileOpen, null)
            Spacer(Modifier.width(8.dp))
            Text("Nhập từ file CSV")
        }
        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FileDownload, null)
            Spacer(Modifier.width(8.dp))
            Text("Xuất bộ từ ra CSV")
        }
        HorizontalDivider()
        Text("Định dạng CSV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Tiêu đề cột được hỗ trợ: term, meaning, ipa, topic, example. Một số tên tương đương như word, translation, definition và sentence cũng được nhận diện.")
        Text("Mỗi từ cần có từ tiếng Anh và nghĩa. Từ trùng sẽ được bỏ qua, không ghi đè dữ liệu đang có.")
        Text("Nếu chọn vị trí lưu trữ đám mây trong bộ chọn Android, việc chuyển tệp qua dịch vụ đó có thể cần Internet. Bản thân ứng dụng không cần mạng để học và quản lý từ.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val modes = listOf("Thẻ từ", "Trắc nghiệm", "Nghe & gõ", "Điền từ", "Ghép cặp", "Ôn hỗn hợp")
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
        if (mode == "Ôn hỗn hợp") listOf("Thẻ từ", "Trắc nghiệm", "Nghe & gõ", "Điền từ", "Ghép cặp").random()
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

    LaunchedEffect(current?.id, activeMode) {
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
        Text("Còn \${dueWords.size} từ đến hạn ôn.")
        if (current == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                    Text(if (allWords.isEmpty()) "Sổ từ đang trống" else "Bạn đã ôn hết các từ đến hạn!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (allWords.isEmpty()) "Hãy thêm từ hoặc nhập bộ từ CSV trong mục Sổ từ/Cài đặt để bắt đầu." else "Thêm từ mới hoặc quay lại khi tới lịch ôn tiếp theo.")
                }
            }
        } else {
            Text("Từ \${safeIndex + 1} / \${dueWords.size}  •  Dạng bài: $activeMode", style = MaterialTheme.typography.labelLarge)
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
                                if (current.topic.isNotBlank()) Text("Chủ đề: \${current.topic}", style = MaterialTheme.typography.labelMedium)
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
                                    onClick = { if (!answered) { chosenAnswer = choice; answered = true; feedbackCorrect = choice == current.meaning; feedback = if (feedbackCorrect) "Chính xác!" else "Đáp án đúng: \${current.meaning}" } },
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
                                    onClick = { if (!answered) { chosenAnswer = choice; answered = true; feedbackCorrect = choice.equals(current.term, true); feedback = if (feedbackCorrect) "Đã ghép đúng!" else "Đáp án đúng: \${current.term}" } },
                                    modifier = Modifier.fillMaxWidth(), enabled = !answered,
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (answered && choice.equals(current.term, true)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                ) { Text(choice) }
                            }
                        }
                        "Nghe & gõ" -> {
                            Text("Nghe và nhập từ tiếng Anh", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Nghĩa: \${current.meaning}")
                            Text("IPA: \${current.ipa.ifBlank { "Chưa có IPA" }}", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { speechEngine?.speak(current.term, TextToSpeech.QUEUE_FLUSH, null, "luyentu-word-${current.id}") }, modifier = Modifier.fillMaxWidth(), enabled = speechReady) {
                                Icon(Icons.Default.VolumeUp, null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (speechReady) "Phát âm từ" else "Đang khởi tạo giọng đọc")
                            }
                            Text("Âm thanh dùng dịch vụ Text-to-Speech trên thiết bị. Muốn dùng hoàn toàn offline, hãy cài sẵn giọng tiếng Anh ngoại tuyến trong cài đặt Android.", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(typedAnswer, { typedAnswer = it; feedback = "" }, Modifier.fillMaxWidth(), label = { Text("Từ tiếng Anh") }, singleLine = true)
                            Button(onClick = {
                                if (typedAnswer.isNotBlank() && !answered) {
                                    answered = true
                                    feedbackCorrect = normalize(typedAnswer) == normalize(current.term)
                                    feedback = if (feedbackCorrect) "Chính xác!" else "Đáp án đúng: \${current.term}"
                                }
                            }, modifier = Modifier.fillMaxWidth(), enabled = !answered && typedAnswer.isNotBlank()) { Text("Kiểm tra") }
                        }
                        "Điền từ" -> {
                            val sentenceHasTerm = current.example.contains(current.term, ignoreCase = true)
                            val sentence = if (sentenceHasTerm) current.example.replace(Regex(Regex.escape(current.term), RegexOption.IGNORE_CASE), "______") else "Nghĩa tiếng Việt: \${current.meaning}"
                            Text("Hoàn thành câu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(sentence, style = MaterialTheme.typography.bodyLarge)
                            if (!sentenceHasTerm) Text("Chưa có câu ví dụ chứa từ này; hãy nhập từ tiếng Anh tương ứng.")
                            OutlinedTextField(typedAnswer, { typedAnswer = it; feedback = "" }, Modifier.fillMaxWidth(), label = { Text("Từ còn thiếu") }, singleLine = true)
                            Button(onClick = {
                                if (typedAnswer.isNotBlank() && !answered) {
                                    answered = true
                                    feedbackCorrect = normalize(typedAnswer) == normalize(current.term)
                                    feedback = if (feedbackCorrect) "Chính xác!" else "Đáp án đúng: \${current.term}"
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

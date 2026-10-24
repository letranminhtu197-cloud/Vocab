package vn.luyentu.app

import android.os.Bundle
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
                2 -> PracticeScreen(padding, dueWords, db, refresh)
                3 -> Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Thống kê", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric("Tổng số từ", words.size.toString(), Modifier.weight(1f))
                        Metric("Lượt ôn", db.reviewCount().toString(), Modifier.weight(1f))
                    }
                    Metric("Đến hạn ôn", dueWords.size.toString(), Modifier.fillMaxWidth())
                    Text("Số liệu được tính từ cơ sở dữ liệu trên thiết bị.")
                }
                else -> SimpleScreen(padding, "Cài đặt", "Ứng dụng không cần tài khoản hoặc kết nối máy chủ. Nhập/xuất CSV và sao lưu cục bộ sẽ được bổ sung.")
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
private fun PracticeScreen(padding: PaddingValues, dueWords: List<VocabularyWord>, db: VocabularyDatabase, refresh: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var revealed by rememberSaveable { mutableStateOf(false) }
    val current = dueWords.getOrNull(index.coerceAtMost((dueWords.size - 1).coerceAtLeast(0)))

    LaunchedEffect(dueWords.size) {
        if (dueWords.isEmpty()) index = 0 else if (index >= dueWords.size) index = 0
        revealed = false
    }

    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Ôn tập cách quãng", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Còn ${dueWords.size} từ đến hạn. Hãy tự nhớ nghĩa trước khi mở đáp án.")
        if (current == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                    Text("Bạn đã ôn hết các từ đến hạn!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Thêm từ mới trong Sổ từ hoặc quay lại sau khi đến lịch ôn tiếp theo.")
                }
            }
        } else {
            Text("Từ ${index + 1} / ${dueWords.size}", style = MaterialTheme.typography.labelLarge)
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(current.term, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (current.ipa.isNotBlank()) Text(current.ipa, color = MaterialTheme.colorScheme.primary)
                    if (revealed) {
                        HorizontalDivider()
                        Text(current.meaning, style = MaterialTheme.typography.titleLarge)
                        if (current.example.isNotBlank()) Text(current.example, style = MaterialTheme.typography.bodyMedium)
                        if (current.topic.isNotBlank()) Text("Chủ đề: ${current.topic}", style = MaterialTheme.typography.labelMedium)
                    } else {
                        Text("Hãy thử nhớ nghĩa tiếng Việt và cách dùng.", style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) { Text("Hiện đáp án") }
                    }
                }
            }
            if (revealed) {
                Text("Bạn nhớ từ này đến đâu?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { db.reviewWord(current.id, 0); refresh(); revealed = false; index = 0 }, modifier = Modifier.weight(1f)) { Text("Quên") }
                    OutlinedButton(onClick = { db.reviewWord(current.id, 1); refresh(); revealed = false; index = 0 }, modifier = Modifier.weight(1f)) { Text("Khó") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { db.reviewWord(current.id, 2); refresh(); revealed = false; index = 0 }, modifier = Modifier.weight(1f)) { Text("Nhớ") }
                    Button(onClick = { db.reviewWord(current.id, 3); refresh(); revealed = false; index = 0 }, modifier = Modifier.weight(1f)) { Text("Dễ") }
                }
                Text("Lịch ôn được điều chỉnh theo mức độ nhớ và lưu cục bộ.", style = MaterialTheme.typography.bodySmall)
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

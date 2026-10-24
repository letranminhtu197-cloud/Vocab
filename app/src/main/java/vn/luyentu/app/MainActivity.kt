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
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val refresh = { words = db.getAllWords() }
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
                        Metric("Đến hạn ôn", "0", Modifier.weight(1f))
                    }
                    Text("Đã triển khai: giao diện chính và sổ từ lưu bằng SQLite cục bộ.")
                    Text("Tiếp theo: nhập CSV, bài tập chủ động, SRS, thống kê và sao lưu.")
                }
                1 -> VocabularyScreen(padding, words, db, refresh)
                2 -> SimpleScreen(padding, "Luyện tập", "Các dạng bài tập và thuật toán SRS sẽ được triển khai tiếp theo.")
                3 -> SimpleScreen(padding, "Thống kê", "Thống kê sẽ được tính từ lịch sử học lưu trên thiết bị.")
                else -> SimpleScreen(padding, "Cài đặt", "Ứng dụng không cần tài khoản hoặc kết nối máy chủ. Nhập/xuất CSV sẽ được bổ sung.")
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
                    }
                    IconButton(onClick = { db.deleteWord(word.id); refresh() }) { Icon(Icons.Default.Delete, "Xoá từ") }
                } }
            }
            if (filtered.isEmpty()) item { Text(if (search.isBlank()) "Chưa có từ. Hãy thêm từ đầu tiên ở biểu mẫu trên." else "Không tìm thấy từ.") }
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

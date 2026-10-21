package com.vocab.ielts.offline

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
    var query by remember { mutableStateOf("") }
    var currentIndex by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var editorWord by remember { mutableStateOf<WordEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showSamplesConfirm by remember { mutableStateOf(false) }
    var dueOnly by remember { mutableStateOf(false) }
    var csvMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val words by dao.observeAll().collectAsState(initial = emptyList())
    val filtered = words.filter {
        it.term.contains(query, true) || it.meaning.contains(query, true) ||
            it.topic.contains(query, true) || it.example.contains(query, true)
    }
    val reviewDeck = if (dueOnly) words.filter { it.nextReviewAtMillis == 0L || it.nextReviewAtMillis <= System.currentTimeMillis() } else words
    val current = reviewDeck.getOrNull(currentIndex.coerceIn(0, (reviewDeck.size - 1).coerceAtLeast(0)))
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val importedText = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    }
                }.getOrNull()
                if (importedText == null) {
                    csvMessage = "Không đọc được file. Hãy chọn một file CSV hợp lệ."
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
                        val insertError = runCatching {
                            if (uniqueWords.isNotEmpty()) withContext(Dispatchers.IO) { dao.insertAll(uniqueWords) }
                        }.exceptionOrNull()
                        csvMessage = if (insertError != null) {
                            "Nhập file thất bại: ${insertError.message ?: "lỗi dữ liệu"}"
                        } else {
                            "Đã nhập ${uniqueWords.size} từ. Bỏ qua ${duplicateCount} từ trùng và ${parsed.invalidRows} dòng thiếu từ/nghĩa."
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
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF356859),
            secondary = androidx.compose.ui.graphics.Color(0xFFF2B544),
            background = androidx.compose.ui.graphics.Color(0xFFF6F8F5)
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Column {
                        Text("Vocab", fontWeight = FontWeight.Bold)
                        Text("IELTS Vocabulary • Offline", fontSize = 12.sp)
                    }
                })
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
                    listOf("Trang chủ", "Sổ từ", "Luyện tập", "Thống kê").forEachIndexed { i, label ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = {
                                Icon(
                                    when (i) {
                                        0 -> Icons.Default.Home
                                        1 -> Icons.Default.MenuBook
                                        2 -> Icons.Default.Refresh
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
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text("Học theo nhịp của bạn", color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text("Từ trong sổ: ${words.size} • Đến hạn: ${words.count { it.nextReviewAtMillis == 0L || it.nextReviewAtMillis <= System.currentTimeMillis() }}", color = MaterialTheme.colorScheme.onPrimary)
                                Text("Dữ liệu được lưu cục bộ trên thiết bị.", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("Ôn flashcard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); FilterChip(selected = dueOnly, onClick = { dueOnly = !dueOnly; currentIndex = 0; revealed = false }, label = { Text(if (dueOnly) "Chỉ từ đến hạn" else "Tất cả từ") }) } }
                    if (current == null) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(if (dueOnly && words.isNotEmpty()) "Không có từ đến hạn" else "Sổ từ đang trống", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(if (dueOnly && words.isNotEmpty()) "Bạn đã ôn hết từ đến hạn hiện tại. Có thể xem toàn bộ sổ từ hoặc quay lại sau." else "Bạn có thể tự nhập từ hoặc thêm vài từ mẫu để kiểm tra ứng dụng.")
                                    if (dueOnly && words.isNotEmpty()) OutlinedButton(onClick = { dueOnly = false; currentIndex = 0 }, modifier = Modifier.fillMaxWidth()) { Text("Xem toàn bộ từ") }
                                    Button(onClick = { tab = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Mở sổ từ") }
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
                                    if (current.ipa.isNotBlank()) Text(current.ipa, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (current.partOfSpeech.isNotBlank()) Text(current.partOfSpeech, color = MaterialTheme.colorScheme.primary)
                                    if (revealed) {
                                        Spacer(Modifier.height(18.dp))
                                        Text(current.meaning, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                        if (current.example.isNotBlank()) {
                                            Spacer(Modifier.height(10.dp))
                                            Text(current.example)
                                        }
                                        if (current.topic.isNotBlank()) Text(current.topic, style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    OutlinedButton(onClick = { revealed = !revealed }) {
                                        Text(if (revealed) "Ẩn đáp án" else "Hiện đáp án")
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { currentIndex = (currentIndex - 1 + reviewDeck.size) % reviewDeck.size; revealed = false },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Trước") }
                                Button(
                                    onClick = { currentIndex = (currentIndex + 1) % reviewDeck.size; revealed = false },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Tiếp theo") }
                            }
                            Text("Đánh giá sau khi hiện đáp án", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Lại" to ReviewRating.AGAIN, "Khó" to ReviewRating.HARD, "Tốt" to ReviewRating.GOOD, "Dễ" to ReviewRating.EASY).forEach { (label, rating) ->
                                    Button(
                                        onClick = {
                                            val now = Instant.now()
                                            val state = ReviewState(
                                                level = current.srsLevel,
                                                nextReviewAt = current.nextReviewAtMillis.takeIf { it > 0L }?.let(Instant::ofEpochMilli),
                                                reviewCount = current.reviewCount,
                                                lapseCount = current.lapseCount
                                            )
                                            val next = SrsScheduler.review(state, rating, now)
                                            scope.launch {
                                                dao.update(current.copy(
                                                    srsLevel = next.level,
                                                    nextReviewAtMillis = next.nextReviewAt?.toEpochMilli() ?: 0L,
                                                    reviewCount = next.reviewCount,
                                                    lapseCount = next.lapseCount,
                                                    lastReviewedAtMillis = now.toEpochMilli()
                                                ))
                                            }
                                            currentIndex = if (reviewDeck.size > 1) (currentIndex + 1) % reviewDeck.size else 0
                                            revealed = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) { Text(label, fontSize = 12.sp) }
                                }
                            }
                        }
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
                        label = { Text("Tìm từ, nghĩa, ví dụ hoặc chủ đề") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Nhập CSV") }
                        OutlinedButton(
                            onClick = { exportLauncher.launch("vocab-words.csv") },
                            modifier = Modifier.weight(1f)
                        ) { Text("Xuất CSV") }
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
                        items(filtered, key = { it.id }) { word ->
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(word.term, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        IconButton(onClick = { editorWord = word; showEditor = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Sửa từ")
                                        }
                                        IconButton(onClick = { scope.launch { dao.delete(word) } }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Xóa từ")
                                        }
                                    }
                                    if (word.ipa.isNotBlank() || word.partOfSpeech.isNotBlank()) Text(listOf(word.ipa, word.partOfSpeech).filter { it.isNotBlank() }.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                                    Text(word.meaning)
                                    if (word.example.isNotBlank()) Text(word.example, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (word.topic.isNotBlank()) Text(word.topic, style = MaterialTheme.typography.labelSmall)
                                    Text("SRS cấp ${word.srsLevel}/5 • Đã ôn ${word.reviewCount} lần", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                2 -> QuizScreen(words = words, modifier = Modifier.fillMaxSize().padding(padding))
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Text("Thống kê", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                    item { StatCard("Tổng số từ", words.size.toString()) }
                    item { StatCard("Đã ôn ít nhất một lần", words.count { it.reviewCount > 0 }.toString()) }
                    item { StatCard("Tổng lượt ôn", words.sumOf { it.reviewCount }.toString()) }
                    item { StatCard("Cấp SRS 5", words.count { it.srsLevel == 5 }.toString()) }
                    item { StatCard("Chế độ hoạt động", "Ngoại tuyến") }
                }
            }
        }
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
                    if (editorWord == null) dao.insert(word) else dao.update(word)
                }
                showEditor = false
                editorWord = null
            }
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
                OutlinedTextField(topic, { topic = it }, label = { Text("Chủ đề") }, singleLine = true)
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
                        topic = topic.trim()
                    ))
                }
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
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

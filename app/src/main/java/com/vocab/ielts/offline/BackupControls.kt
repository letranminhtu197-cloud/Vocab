package com.vocab.ielts.offline

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupControls(dao: WordDao, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingMessage by remember { mutableStateOf<String?>(null) }
    var pendingRestoreWords by remember { mutableStateOf<List<WordEntity>?>(null) }
    var pendingRestoreDuplicates by remember { mutableIntStateOf(0) }
    var pendingRestoreInvalidRows by remember { mutableIntStateOf(0) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val bytesResult = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            total += n
                            require(total <= 20 * 1024 * 1024) { "File sao lưu vượt giới hạn 20 MB." }
                            output.write(buffer, 0, n)
                        }
                        output.toByteArray()
                    } ?: error("Không mở được file.")
                }
            }
            if (bytesResult.isFailure) {
                pendingMessage = "Không đọc được file: ${bytesResult.exceptionOrNull()?.message ?: "lỗi đọc"}"
            } else {
                val parsed = BackupCodec.restore(bytesResult.getOrThrow())
                if (parsed.error != null) pendingMessage = parsed.error else {
                    val existing = dao.observeAll().first().map { it.term.trim().lowercase() }.toMutableSet()
                    var duplicates = 0
                    val unique = parsed.words.filter { word ->
                        if (existing.add(word.term.trim().lowercase())) true else { duplicates++; false }
                    }
                    if (unique.isEmpty()) {
                        pendingMessage = "Không có từ mới để khôi phục. Bỏ qua ${duplicates} từ trùng và ${parsed.invalidRows} dòng lỗi."
                    } else {
                        pendingRestoreWords = unique
                        pendingRestoreDuplicates = duplicates
                        pendingRestoreInvalidRows = parsed.invalidRows
                    }
                }
            }
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching {
                val words = dao.observeAll().first()
                val bytes = BackupCodec.create(words)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Không mở được file đích.")
                }
                words.size
            }
            pendingMessage = result.fold(
                onSuccess = { "Đã sao lưu ${it} từ cùng tiến độ SRS." },
                onFailure = { "Sao lưu thất bại: ${it.message ?: "lỗi ghi file"}" }
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }, modifier = Modifier.weight(1f)) {
            Text("Khôi phục ZIP")
        }
        OutlinedButton(onClick = { backupLauncher.launch("vocab-backup.zip") }, modifier = Modifier.weight(1f)) {
            Text("Sao lưu ZIP")
        }
    }
    pendingRestoreWords?.let { previewWords ->
        AlertDialog(
            onDismissRequest = { pendingRestoreWords = null },
            title = { Text("Xác nhận khôi phục ZIP") },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sẽ thêm ${previewWords.size} từ mới cùng tiến độ SRS. Dữ liệu đang có sẽ không bị ghi đè.")
                    Text("Bỏ qua ${pendingRestoreDuplicates} từ trùng và ${pendingRestoreInvalidRows} dòng lỗi.")
                    Text("Xem trước", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    previewWords.take(5).forEach { word ->
                        Text("• ${word.term} — ${word.meaning}")
                    }
                    if (previewWords.size > 5) Text("… và ${previewWords.size - 5} từ khác")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val wordsToRestore = previewWords
                    val duplicates = pendingRestoreDuplicates
                    val invalidRows = pendingRestoreInvalidRows
                    pendingRestoreWords = null
                    scope.launch {
                        val save = runCatching {
                            withContext(Dispatchers.IO) { dao.insertAll(wordsToRestore) }
                        }
                        pendingMessage = if (save.isFailure) {
                            "Khôi phục thất bại: ${save.exceptionOrNull()?.message ?: "lỗi dữ liệu"}"
                        } else {
                            "Đã khôi phục ${wordsToRestore.size} từ; bỏ qua ${duplicates} từ trùng và ${invalidRows} dòng lỗi."
                        }
                    }
                }) { Text("Khôi phục ${previewWords.size} từ") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreWords = null }) { Text("Hủy") }
            }
        )
    }
    pendingMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { pendingMessage = null },
            title = { Text("Sao lưu / khôi phục") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { onMessage(message); pendingMessage = null }) { Text("Đóng") } }
        )
    }
}

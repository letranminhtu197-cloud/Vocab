package com.vocab.ielts.offline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MatchGameDialog(words: List<WordEntity>, onDismiss: () -> Unit) {
    val pairs = remember(words) { ActiveLearningLogic.matchPairs(words).shuffled() }
    val terms = remember(pairs) { pairs.shuffled() }
    val meanings = remember(pairs) { pairs.shuffled() }
    val matched = remember(pairs) { mutableStateListOf<Long>() }
    var selectedTerm by remember { mutableStateOf<Long?>(null) }
    var selectedMeaning by remember { mutableStateOf<Long?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val canCheck = selectedTerm?.let { it !in matched } == true &&
        selectedMeaning?.let { it !in matched } == true
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trò chơi ghép từ", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Ghép ${pairs.size} từ tiếng Anh với nghĩa đã nhập. Kết quả trò chơi không thay đổi lịch SRS.")
                if (pairs.isEmpty()) {
                    Text("Chưa đủ dữ liệu. Cần ít nhất hai từ có cả từ tiếng Anh và nghĩa khác nhau.")
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("TỪ VỰNG", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            terms.forEach { pair ->
                                val done = pair.wordId in matched
                                FilterChip(
                                    selected = selectedTerm == pair.wordId,
                                    enabled = !done,
                                    onClick = { selectedTerm = pair.wordId; feedback = null },
                                    label = { Text(pair.term) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("NGHĨA", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            meanings.forEach { pair ->
                                val done = pair.wordId in matched
                                FilterChip(
                                    selected = selectedMeaning == pair.wordId,
                                    enabled = !done,
                                    onClick = { selectedMeaning = pair.wordId; feedback = null },
                                    label = { Text(pair.meaning) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    Text("Đã ghép: ${matched.size}/${pairs.size} · Lượt thử: $attempts")
                    if (feedback != null) {
                        Text(
                            feedback.orEmpty(),
                            color = if (feedback == "Chính xác!") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (matched.size == pairs.size) {
                        Text("Hoàn thành! Bạn đã ghép đủ các cặp.", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pairs.isNotEmpty() && matched.size < pairs.size) {
                    Button(
                        enabled = canCheck,
                        onClick = {
                            attempts += 1
                            if (selectedTerm == selectedMeaning) {
                                matched.add(selectedTerm!!)
                                feedback = "Chính xác!"
                            } else {
                                feedback = "Chưa khớp — thử lại."
                            }
                            selectedTerm = null
                            selectedMeaning = null
                        }
                    ) { Text("Kiểm tra") }
                }
                TextButton(onClick = onDismiss) { Text(if (pairs.isNotEmpty() && matched.size == pairs.size) "Hoàn tất" else "Đóng") }
            }
        }
    )
}

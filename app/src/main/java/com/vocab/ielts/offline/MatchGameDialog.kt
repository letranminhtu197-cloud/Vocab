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
        title = { Text("Ghép từ với nghĩa", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Ghép tối đa 6 cặp từ và nghĩa đã nhập. Trò chơi không thay đổi lịch SRS.")
                if (pairs.isEmpty()) {
                    Text("Chưa đủ dữ liệu: cần ít nhất hai từ có nghĩa khác nhau và không bị trùng.")
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("TỪ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            terms.forEach { pair ->
                                FilterChip(
                                    selected = selectedTerm == pair.wordId,
                                    enabled = pair.wordId !in matched,
                                    onClick = { selectedTerm = pair.wordId; feedback = null },
                                    label = { Text(pair.term) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("NGHĨA", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            meanings.forEach { pair ->
                                FilterChip(
                                    selected = selectedMeaning == pair.wordId,
                                    enabled = pair.wordId !in matched,
                                    onClick = { selectedMeaning = pair.wordId; feedback = null },
                                    label = { Text(pair.meaning) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    Text("Đã ghép: ${matched.size}/${pairs.size} · Lượt thử: $attempts")
                    feedback?.let { Text(it, color = if (it == "Chính xác!") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                    if (matched.size == pairs.size) Text("Hoàn thành!", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pairs.isNotEmpty() && matched.size < pairs.size) {
                    Button(enabled = canCheck, onClick = {
                        attempts++
                        if (selectedTerm == selectedMeaning) {
                            matched.add(selectedTerm!!)
                            feedback = "Chính xác!"
                        } else feedback = "Chưa khớp — thử lại."
                        selectedTerm = null
                        selectedMeaning = null
                    }) { Text("Kiểm tra") }
                }
                TextButton(onClick = onDismiss) { Text("Đóng") }
            }
        }
    )
}

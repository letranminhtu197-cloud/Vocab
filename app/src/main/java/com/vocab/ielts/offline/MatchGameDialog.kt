package com.vocab.ielts.offline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MatchGameDialog(words: List<WordEntity>, onDismiss: () -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    val pairs = remember(words, round) { ActiveLearningLogic.matchPairs(words).shuffled() }
    val terms = remember(round, pairs) { pairs.shuffled() }
    val meanings = remember(round, pairs) { pairs.shuffled() }
    val context = LocalContext.current
    val statsPrefs = remember(context) { context.getSharedPreferences("vocab_learning_stats", 0) }
    val startedAtMillis = remember(round, pairs) { System.currentTimeMillis() }
    val matched = remember(round, pairs) { mutableStateListOf<Long>() }
    LaunchedEffect(round, pairs, matched.size) {
        if (pairs.isNotEmpty() && matched.size == pairs.size) {
            statsPrefs.edit()
                .putInt("match_games_completed", statsPrefs.getInt("match_games_completed", 0) + 1)
                .putLong("match_duration_total_ms", statsPrefs.getLong("match_duration_total_ms", 0L) + (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0L))
                .apply()
        }
    }
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
                Text("Ghép từ với nghĩa dựa trên bộ dữ liệu của bạn. Trò chơi không thay đổi lịch ôn SRS.", style = MaterialTheme.typography.bodyMedium)
                if (pairs.isEmpty()) {
                    Text("Chưa đủ dữ liệu: cần ít nhất hai từ có nghĩa khác nhau và không bị trùng.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("Tiến độ", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                            Text("${matched.size}/${pairs.size} cặp", fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(
                            progress = { matched.size.toFloat() / pairs.size.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth().height(7.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("TỪ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            terms.forEach { pair ->
                                FilterChip(
                                    selected = selectedTerm == pair.wordId,
                                    enabled = pair.wordId !in matched,
                                    onClick = { selectedTerm = pair.wordId; feedback = null },
                                    label = { Text(pair.term, maxLines = 2, overflow = TextOverflow.Ellipsis) },
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
                                    label = { Text(pair.meaning, maxLines = 4, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    Text("Lượt thử: $attempts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        val isCorrectPair = selectedTerm == selectedMeaning
                        statsPrefs.edit()
                            .putInt("match_attempts", statsPrefs.getInt("match_attempts", 0) + 1)
                            .putInt("match_correct", statsPrefs.getInt("match_correct", 0) + if (isCorrectPair) 1 else 0)
                            .apply()
                        if (isCorrectPair) {
                            matched.add(selectedTerm!!)
                            feedback = "Chính xác!"
                        } else feedback = "Chưa khớp — thử lại."
                        selectedTerm = null
                        selectedMeaning = null
                    }) { Text("Kiểm tra") }
                }
                if (pairs.isNotEmpty() && matched.size == pairs.size) {
                    TextButton(onClick = {
                        round++
                        selectedTerm = null
                        selectedMeaning = null
                        attempts = 0
                        feedback = null
                    }) { Text("Chơi lại") }
                }
                TextButton(onClick = onDismiss) { Text("Đóng") }
            }
        }
    )
}

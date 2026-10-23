package com.vocab.ielts.offline

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val MATCH_BATCH_SIZE = 4

/** Offline word-to-meaning matching using only the learner's own library. */
@Composable
fun MatchingGameScreen(
    words: List<WordEntity>,
    modifier: Modifier = Modifier,
    onAnswerRecorded: (WordEntity, Boolean) -> Unit
) {
    val wordIds = remember(words) { words.map { it.id } }
    val topics = remember(words) { words.map { it.topic.trim() }.filter { it.isNotEmpty() }.distinct().sortedBy { it.lowercase() } }
    var selectedTopic by remember(wordIds) { mutableStateOf("Tất cả chủ đề") }
    val eligibleWords = words.filter { !it.paused }
    val scopedWords = if (selectedTopic == "Tất cả chủ đề") eligibleWords
        else eligibleWords.filter { it.topic.trim().equals(selectedTopic, ignoreCase = true) }

    var roundOffset by remember(wordIds) { mutableIntStateOf(0) }
    var selectedTermId by remember(wordIds) { mutableStateOf<Long?>(null) }
    var selectedMeaningId by remember(wordIds) { mutableStateOf<Long?>(null) }
    val matchedIds = remember(wordIds) { mutableStateListOf<Long>() }
    val mistakenIds = remember(wordIds) { mutableStateListOf<Long>() }
    var feedback by remember(wordIds) { mutableStateOf<String?>(null) }
    var completed by remember(wordIds) { mutableStateOf(false) }
    var totalMatched by remember(wordIds) { mutableIntStateOf(0) }
    var perfectMatched by remember(wordIds) { mutableIntStateOf(0) }

    val currentRound = scopedWords.drop(roundOffset).take(MATCH_BATCH_SIZE)
    val meaningOrder = remember(selectedTopic, roundOffset, currentRound.map { it.id }) { currentRound.shuffled() }

    fun choosePair(termId: Long?, meaningId: Long?) {
        if (termId == null || meaningId == null) return
        if (termId == meaningId) {
            val matched = currentRound.firstOrNull { it.id == termId } ?: return
            val correctWithoutMistake = termId !in mistakenIds
            matchedIds.add(termId)
            totalMatched += 1
            if (correctWithoutMistake) perfectMatched += 1
            onAnswerRecorded(matched, correctWithoutMistake)
            selectedTermId = null
            selectedMeaningId = null
            feedback = if (correctWithoutMistake) "Đúng cặp!" else "Đã ghép đúng sau một lần nhầm."
            if (matchedIds.size == currentRound.size) {
                val nextOffset = roundOffset + currentRound.size
                if (nextOffset >= scopedWords.size) {
                    completed = true
                } else {
                    roundOffset = nextOffset
                    matchedIds.clear()
                    mistakenIds.clear()
                    feedback = null
                }
            }
        } else {
            if (termId !in mistakenIds) mistakenIds.add(termId)
            if (meaningId !in mistakenIds) mistakenIds.add(meaningId)
            selectedTermId = null
            selectedMeaningId = null
            feedback = "Chưa khớp. Hãy thử ghép lại."
        }
    }

    fun restart() {
        roundOffset = 0
        selectedTermId = null
        selectedMeaningId = null
        matchedIds.clear()
        mistakenIds.clear()
        feedback = null
        completed = false
        totalMatched = 0
        perfectMatched = 0
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Ghép từ với nghĩa", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Tìm các cặp đúng trong sổ từ của bạn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (topics.isNotEmpty()) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    (listOf("Tất cả chủ đề") + topics).forEach { topic ->
                        FilterChip(
                            selected = selectedTopic.equals(topic, ignoreCase = true),
                            onClick = {
                                selectedTopic = topic
                                restart()
                            },
                            label = { Text(topic) }
                        )
                    }
                }
            }
        }
        if (scopedWords.size < 2) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cần ít nhất 2 từ", fontWeight = FontWeight.Bold)
                        Text("Thêm từ vào sổ tay hoặc chọn một chủ đề có nhiều từ hơn.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else if (completed) {
            item {
                Card(shape = RoundedCornerShape(22.dp), colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Đã ghép xong", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Ghép đúng ngay lần đầu: $perfectMatched / $totalMatched cặp.")
                        Button(onClick = { restart() }, modifier = Modifier.fillMaxWidth()) { Text("Chơi lại") }
                    }
                }
            }
        } else {
            item {
                Text("${roundOffset + matchedIds.size} / ${scopedWords.size} cặp", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("TỪ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                currentRound.forEach { word ->
                                    val isMatched = word.id in matchedIds
                                    val isSelected = selectedTermId == word.id
                                    Surface(
                                        onClick = {
                                            if (!isMatched) {
                                                selectedTermId = if (isSelected) null else word.id
                                                choosePair(selectedTermId, selectedMeaningId)
                                            }
                                        },
                                        color = when {
                                            isMatched -> MaterialTheme.colorScheme.primaryContainer
                                            isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(13.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Text(word.term, Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                            fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("NGHĨA", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                meaningOrder.forEach { word ->
                                    val isMatched = word.id in matchedIds
                                    val isSelected = selectedMeaningId == word.id
                                    Surface(
                                        onClick = {
                                            if (!isMatched) {
                                                selectedMeaningId = if (isSelected) null else word.id
                                                choosePair(selectedTermId, selectedMeaningId)
                                            }
                                        },
                                        color = when {
                                            isMatched -> MaterialTheme.colorScheme.primaryContainer
                                            isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(13.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Text(word.meaning, Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        Text(
                            feedback ?: "Chọn một từ rồi chọn nghĩa tương ứng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (feedback?.startsWith("Chưa") == true) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(onClick = { restart() }, modifier = Modifier.fillMaxWidth()) { Text("Bắt đầu lại phiên ghép") }
            }
        }
    }
}

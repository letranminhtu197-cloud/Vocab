package com.vocab.ielts.offline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FlashcardReviewScreen(
    allWords: List<WordEntity>,
    deck: List<WordEntity>,
    dueOnly: Boolean,
    currentIndex: Int,
    revealed: Boolean,
    ratingInProgress: Boolean,
    onDueOnlyChange: (Boolean) -> Unit,
    onIndexChange: (Int) -> Unit,
    onRevealChange: (Boolean) -> Unit,
    onRate: (WordEntity, ReviewRating) -> Unit
) {
    val current = deck.getOrNull(currentIndex.coerceIn(0, (deck.size - 1).coerceAtLeast(0)))
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ôn flashcard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Nhớ lại trước khi lật thẻ.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilterChip(
                    selected = dueOnly,
                    onClick = { onDueOnlyChange(!dueOnly) },
                    label = { Text(if (dueOnly) "Đến hạn" else "Tất cả từ") }
                )
            }
        }
        if (current == null) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (dueOnly && allWords.isNotEmpty()) "Không có từ đến hạn" else "Sổ từ đang trống",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (dueOnly && allWords.isNotEmpty()) "Bạn đã ôn hết từ đến hạn hiện tại. Quay lại sau hoặc xem toàn bộ từ."
                            else "Thêm từ hoặc nhập khóa học trong Sổ từ để bắt đầu.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (dueOnly && allWords.isNotEmpty()) {
                            OutlinedButton(onClick = { onDueOnlyChange(false); onIndexChange(0) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Xem toàn bộ từ")
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Text("Thẻ ${currentIndex + 1} / ${deck.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                SegmentProgressBar(
                    progress = (currentIndex + 1).toFloat() / deck.size.coerceAtLeast(1),
                    segments = deck.size.coerceAtMost(12)
                )
                Card(Modifier.fillMaxWidth().heightIn(min = 280.dp), shape = RoundedCornerShape(24.dp)) {
                    Column(
                        Modifier.fillMaxWidth().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(current.term, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        if (current.ipa.isNotBlank()) Text(current.ipa, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (current.partOfSpeech.isNotBlank()) Text(current.partOfSpeech, color = MaterialTheme.colorScheme.primary)
                        OfflineSpeakButton(text = current.term, allowSlow = true)
                        ReviewWindowLabel(current)
                        if (revealed) {
                            Spacer(Modifier.height(4.dp))
                            Text(current.meaning, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                            if (current.example.isNotBlank()) Text(current.example, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (current.exampleMeaning.isNotBlank()) Text(current.exampleMeaning, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (current.topic.isNotBlank()) Text("Chủ đề: ${current.topic}", style = MaterialTheme.typography.labelMedium)
                            if (current.collocations.isNotBlank()) Text("Cụm từ: ${current.collocations}")
                            if (current.synonyms.isNotBlank()) Text("Từ đồng nghĩa: ${current.synonyms}")
                            if (current.wordFamily.isNotBlank()) Text("Họ từ: ${current.wordFamily}")
                            if (current.tags.isNotBlank()) Text("Thẻ: ${current.tags}", style = MaterialTheme.typography.labelSmall)
                            if (current.notes.isNotBlank()) Text("Ghi chú: ${current.notes}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { onRevealChange(!revealed) }, enabled = !ratingInProgress) {
                            Text(if (revealed) "Ẩn đáp án" else "Hiện đáp án")
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onIndexChange((currentIndex - 1 + deck.size) % deck.size); onRevealChange(false) },
                        enabled = !ratingInProgress,
                        modifier = Modifier.weight(1f)
                    ) { Text("Trước") }
                    Button(
                        onClick = { onIndexChange((currentIndex + 1) % deck.size); onRevealChange(false) },
                        enabled = !ratingInProgress,
                        modifier = Modifier.weight(1f)
                    ) { Text("Tiếp theo") }
                }
                Text(
                    if (revealed) "Đánh giá mức độ ghi nhớ" else "Hãy nhớ lại nghĩa rồi hiện đáp án.",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Lại" to ReviewRating.AGAIN, "Khó" to ReviewRating.HARD,
                        "Tốt" to ReviewRating.GOOD, "Dễ" to ReviewRating.EASY).forEach { (label, rating) ->
                        Button(
                            onClick = { onRate(current, rating) },
                            enabled = revealed && !ratingInProgress,
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 3.dp, vertical = 8.dp)
                        ) { Text(label, fontSize = 12.sp) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

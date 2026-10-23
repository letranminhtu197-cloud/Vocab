package com.vocab.ielts.offline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Chi tiết một mục từ: toàn bộ trường dữ liệu, trạng thái ghi nhớ và các thao tác
 * nhanh. Trước đây thông tin chỉ nằm rải trong thẻ ở sổ từ nên khó xem lại.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailSheet(
    word: WordEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPractice: () -> Unit,
    onTogglePause: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state = word.toReviewState()
    val window = SrsScheduler.goldenWindow(state, Instant.now())
    val windowLabel = when (window) {
        "NEW" -> "Mới — chưa ôn lần nào"
        "UPCOMING" -> "Chưa tới hạn ôn"
        "GOLDEN" -> "Đang trong thời điểm vàng để ôn"
        else -> "Đã quá hạn ôn"
    }
    val nextReview = word.nextReviewAtMillis
        .takeIf { it > 0L }
        ?.let { millis ->
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
        }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(word.term, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (word.ipa.isNotBlank()) Text(word.ipa, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (word.partOfSpeech.isNotBlank()) Text(word.partOfSpeech, color = MaterialTheme.colorScheme.primary)
            }
            if (word.meaning.isNotBlank()) Text(word.meaning, style = MaterialTheme.typography.titleMedium)
            if (word.paused) {
                Text(
                    "Đang tạm dừng ôn — từ vẫn được giữ trong sổ.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            OfflineSpeakButton(text = word.term, allowSlow = true)

            HorizontalDivider()
            Text("Trạng thái ghi nhớ", fontWeight = FontWeight.SemiBold)
            Text("Cấp SRS: ${word.srsLevel}/5 • Đã ôn: ${word.reviewCount} lượt • Quên: ${word.lapseCount} lượt")
            Text(windowLabel, color = MaterialTheme.colorScheme.primary)
            Text(
                if (nextReview != null) "Lịch ôn kế tiếp: $nextReview"
                else "Chưa có lịch ôn — từ này sẽ xuất hiện trong lượt ôn tới.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()
            DetailField("Ví dụ", word.example)
            DetailField("Dịch nghĩa câu ví dụ", word.exampleMeaning)
            DetailField("Chủ đề", word.topic)
            DetailField("Collocations", word.collocations)
            DetailField("Từ đồng nghĩa", word.synonyms)
            DetailField("Họ từ", word.wordFamily)
            DetailField("Thẻ", word.tags)
            DetailField("Ghi chú", word.notes)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onPractice, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.School, contentDescription = null)
                    Text("  Luyện từ này")
                }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Text("  Sửa")
                }
            }
            OutlinedButton(onClick = onTogglePause, modifier = Modifier.fillMaxWidth()) {
                Text(if (word.paused) "Ôn lại từ này" else "Tạm dừng ôn từ này")
            }
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("  Xóa khỏi sổ từ", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

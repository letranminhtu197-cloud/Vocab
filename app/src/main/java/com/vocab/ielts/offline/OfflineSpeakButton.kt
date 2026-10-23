package com.vocab.ielts.offline

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Phát âm bằng Text-to-Speech ngoại tuyến. Có thêm nút "Nghe chậm" (tốc độ 0.6) để luyện
 * nghe kỹ, giống cặp nút phát âm thường/phát âm chậm của các app học từ vựng.
 */
@Composable
fun OfflineSpeakButton(
    text: String,
    label: String = "Nghe phát âm",
    allowSlow: Boolean = false
) {
    val context = LocalContext.current
    val engineRef = remember { mutableStateOf<TextToSpeech?>(null) }
    var offlineReady by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Đang kiểm tra giọng đọc ngoại tuyến…") }

    DisposableEffect(context) {
        offlineReady = false
        statusText = "Đang kiểm tra giọng đọc ngoại tuyến…"
        val engine = TextToSpeech(context.applicationContext) { initStatus ->
            val tts = engineRef.value
            if (initStatus != TextToSpeech.SUCCESS || tts == null) {
                offlineReady = false
                statusText = "Không khởi tạo được bộ đọc trên thiết bị."
            } else {
                val voice: Voice? = tts.voices
                    ?.filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
                    ?.sortedWith(
                        compareByDescending<Voice> { it.locale.country.equals("US", true) }
                            .thenByDescending { it.quality }
                    )
                    ?.firstOrNull()
                if (voice == null) {
                    offlineReady = false
                    statusText = "Chưa có giọng tiếng Anh ngoại tuyến. Cài giọng đọc offline trong cài đặt Text-to-Speech của Android."
                } else {
                    tts.voice = voice
                    offlineReady = true
                    statusText = "Dùng giọng tiếng Anh có sẵn trên máy; không gọi dịch vụ mạng."
                }
            }
        }
        engineRef.value = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            engineRef.value = null
        }
    }

    fun speak(rate: Float) {
        val engine = engineRef.value ?: return
        val value = text.trim()
        if (!offlineReady || value.isEmpty()) return
        engine.setSpeechRate(rate)
        engine.speak(value, TextToSpeech.QUEUE_FLUSH, null, "vocab-offline-speech")
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { speak(1.0f) },
                enabled = offlineReady && text.isNotBlank()
            ) { Text(label) }
            if (allowSlow) {
                OutlinedButton(
                    onClick = { speak(0.6f) },
                    enabled = offlineReady && text.isNotBlank()
                ) { Text("Nghe chậm") }
            }
        }
        Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

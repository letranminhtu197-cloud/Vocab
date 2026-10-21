package com.vocab.ielts.offline

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun OfflineSpeakButton(text: String, label: String = "Nghe phát âm") {
    val context = LocalContext.current
    val engineRef = remember { mutableStateOf<TextToSpeech?>(null) }
    var offlineReady by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

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
                    ?.sortedWith(compareByDescending<Voice> { it.locale.country.equals("US", true) }.thenByDescending { it.quality })
                    ?.firstOrNull()
                if (voice == null) {
                    offlineReady = false
                    statusText = "Chưa có giọng tiếng Anh ngoại tuyến. Cài giọng đọc offline trong cài đặt Text-to-Speech của Android."
                } else {
                    tts.voice = voice
                    offlineReady = true
                    statusText = "Đang dùng giọng đọc có sẵn trên máy; không gọi dịch vụ mạng."
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

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
            onClick = {
                val value = text.trim()
                if (offlineReady && value.isNotEmpty()) {
                    engineRef.value?.speak(value, TextToSpeech.QUEUE_FLUSH, null, "vocab-offline-speech")
                }
            },
            enabled = offlineReady && text.isNotBlank()
        ) { Text(label) }
        statusText?.let { Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
    }
}

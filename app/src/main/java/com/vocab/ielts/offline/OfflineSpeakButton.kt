package com.vocab.ielts.offline

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
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

@Composable
fun OfflineSpeakButton(
    text: String,
    label: String = "Nghe phát âm",
    onAvailabilityChanged: (Boolean, String) -> Unit = { _, _ -> },
    onAudioReplay: () -> Unit = {}
) {
    val context = LocalContext.current
    val engineRef = remember { mutableStateOf<TextToSpeech?>(null) }
    var offlineReady by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Đang kiểm tra giọng đọc ngoại tuyến…") }

    DisposableEffect(context) {
        offlineReady = false
        statusText = "Đang kiểm tra giọng đọc ngoại tuyến…"
        onAvailabilityChanged(false, statusText)
        val engine = TextToSpeech(context.applicationContext) { initStatus ->
            val tts = engineRef.value
            if (initStatus != TextToSpeech.SUCCESS || tts == null) {
                offlineReady = false
                statusText = "Không khởi tạo được bộ đọc trên thiết bị."
                onAvailabilityChanged(false, statusText)
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
                    statusText = "Chưa có giọng tiếng Anh ngoại tuyến. Kiểm tra cài đặt Text-to-Speech của Android."
                    onAvailabilityChanged(false, statusText)
                } else {
                    tts.voice = voice
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit
                        override fun onDone(utteranceId: String?) = Unit
                        override fun onError(utteranceId: String?) {
                            Handler(Looper.getMainLooper()).post {
                                offlineReady = false
                                statusText = "Phát âm bị gián đoạn hoặc thất bại. Hãy chuyển sang dạng bài khác."
                                onAvailabilityChanged(false, statusText)
                            }
                        }
                        override fun onError(utteranceId: String?, errorCode: Int) {
                            onError(utteranceId)
                        }
                    })
                    offlineReady = true
                    statusText = "Đã sẵn sàng dùng giọng tiếng Anh có sẵn trên máy."
                    onAvailabilityChanged(true, statusText)
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
                    val result = engineRef.value?.speak(value, TextToSpeech.QUEUE_FLUSH, null, "vocab-offline-speech")
                    if (result == TextToSpeech.SUCCESS) {
                        onAudioReplay()
                        statusText = "Đang phát âm ngoại tuyến."
                    } else {
                        statusText = "Không phát được âm thanh. Bạn có thể chuyển sang dạng bài khác."
                        onAvailabilityChanged(false, statusText)
                    }
                }
            },
            enabled = offlineReady && text.isNotBlank()
        ) { Text(label) }
        Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

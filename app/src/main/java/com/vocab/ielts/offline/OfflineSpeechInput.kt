package com.vocab.ielts.offline

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Nhận diện từ tiếng Anh bằng API on-device của Android.
 * Không dùng createSpeechRecognizer() thông thường và không chuyển sang nhận diện qua mạng.
 */
@Composable
fun OfflineSpeechInput(
    enabled: Boolean,
    onRecognized: (String) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var listening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Chỉ dùng nhận diện giọng nói ngoại tuyến trên thiết bị.") }
    val supported = remember(context) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    }
    val recognizer = remember(context, supported) {
        if (supported) SpeechRecognizer.createOnDeviceSpeechRecognizer(context) else null
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        status = if (granted) "Đã cấp quyền microphone. Nhấn nút để đọc từ."
        else "Chưa có quyền microphone; bạn có thể cấp quyền để dùng chế độ nói."
    }

    DisposableEffect(recognizer) {
        if (recognizer != null) {
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                    status = "Đang nghe… hãy đọc từ tiếng Anh một lần, rõ ràng."
                }

                override fun onBeginningOfSpeech() {
                    listening = true
                }

                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    status = "Đang xử lý trên thiết bị…"
                }

                override fun onError(error: Int) {
                    listening = false
                    status = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "Chưa nhận diện được từ. Hãy thử lại trong môi trường yên tĩnh."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "Android chưa cấp quyền microphone."
                        else -> "Nhận diện thất bại (mã $error). Kiểm tra gói ngôn ngữ offline rồi thử lại."
                    }
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    val recognized = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                        .trim()
                    if (recognized.isBlank()) {
                        status = "Chưa nghe rõ từ. Hãy thử lại."
                    } else {
                        status = "Đã nhận diện: $recognized"
                        onRecognized(recognized)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
        onDispose {
            recognizer?.cancel()
            recognizer?.destroy()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = {
                if (!hasPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else if (recognizer != null && !listening) {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    }
                    status = "Chuẩn bị nghe bằng bộ nhận diện ngoại tuyến…"
                    runCatching { recognizer.startListening(intent) }
                        .onFailure {
                            listening = false
                            status = "Không khởi động được nhận diện offline. Hãy kiểm tra cài đặt giọng nói của Android."
                        }
                }
            },
            enabled = enabled && supported && !listening,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    listening -> "Đang nghe…"
                    !supported -> "Không hỗ trợ nhận diện offline"
                    !hasPermission -> "Cấp quyền và đọc từ"
                    else -> "Đọc từ bằng giọng nói"
                }
            )
        }
        Text(
            when {
                !supported -> "Thiết bị hoặc dịch vụ giọng nói chưa cung cấp API nhận diện ngoại tuyến. Vocab không dùng dịch vụ nhận diện qua mạng."
                !hasPermission -> "Cần quyền microphone. Quyền chỉ được hỏi khi bạn chủ động dùng chế độ này."
                else -> status
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            "Kết quả chỉ kiểm tra từ mà bộ nhận diện nghe được, không chấm điểm giọng, trọng âm hay độ chuẩn phát âm.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

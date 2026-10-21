package com.vocab.ielts.offline

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun StudyReminderControl(onMessage: (String) -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(StudyReminder.isEnabled(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            enabled = true
            StudyReminder.setEnabled(context, true)
            onMessage("Đã bật nhắc ôn hằng ngày lúc khoảng 19:00. Android có thể điều chỉnh vài phút để tiết kiệm pin.")
        } else {
            enabled = false
            StudyReminder.setEnabled(context, false)
            onMessage("Chưa cấp quyền thông báo nên nhắc ôn chưa được bật. Có thể bật quyền trong Cài đặt Android.")
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Nhắc ôn hằng ngày", style = MaterialTheme.typography.titleMedium)
                Text("Khoảng 19:00 • Hoạt động ngoại tuyến • Có thể tắt bất cứ lúc nào", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = { turnOn ->
                if (!turnOn) {
                    enabled = false
                    StudyReminder.setEnabled(context, false)
                    onMessage("Đã tắt nhắc ôn hằng ngày.")
                } else if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    enabled = true
                    StudyReminder.setEnabled(context, true)
                    onMessage("Đã bật nhắc ôn hằng ngày lúc khoảng 19:00. Android có thể điều chỉnh vài phút để tiết kiệm pin.")
                }
            })
        }
    }
}
package com.vocab.ielts.offline

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object StudyReminder {
    const val PREFS = "vocab_local_settings"
    const val KEY_ENABLED = "daily_reminder_enabled"
    private const val CHANNEL_ID = "vocab_study_reminders"
    private const val ALARM_REQUEST = 8107

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) schedule(context) else cancel(context)
    }

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    /** Lịch nhắc dự phòng: 19:00 mỗi ngày khi chưa tính được "thời điểm vàng" nào. */
    fun schedule(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context)
        alarm.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, nextReminderAt(System.currentTimeMillis()), AlarmManager.INTERVAL_DAY, pending
        )
    }

    /**
     * Đặt nhắc tại đúng "thời điểm vàng" tính từ lịch ôn của các từ. Dùng chung request code
     * với lịch dự phòng nên mốc mới sẽ thay mốc cũ, tránh hai thông báo trùng nhau.
     */
    fun scheduleGoldenTime(context: Context, triggerAtMillis: Long) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context)
        alarm.cancel(pending)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        } else {
            alarm.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, ALARM_REQUEST, Intent(context, StudyReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    internal fun nextReminderAt(nowMillis: Long): Long {
        val next = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context)
        alarm.cancel(pending)
        pending.cancel()
    }

    fun showNotification(context: Context, message: String? = null) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Nhắc ôn từ vựng", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Nhắc học từ vựng IELTS theo lịch ôn"
                }
            )
        }
        val openApp = PendingIntent.getActivity(
            context, 8108, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = message ?: "Ôn các từ mới và từ đến hạn trong Vocab nhé."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Đến giờ ôn từ vựng")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(8109, notification)
        }
    }
}

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!StudyReminder.isEnabled(context)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val words = VocabDatabase.getInstance(context).wordDao().observeAll().first()
                val now = System.currentTimeMillis()
                StudyReminder.showNotification(context, GoldenTimeLogic.notificationMessage(words, now))
                val next = GoldenTimeLogic.nextReminderAt(words, now)
                if (next != null) StudyReminder.scheduleGoldenTime(context, next) else StudyReminder.schedule(context)
            } catch (_: Exception) {
                StudyReminder.showNotification(context, null)
            } finally {
                pending.finish()
            }
        }
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED || !StudyReminder.isEnabled(context)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val words = VocabDatabase.getInstance(context).wordDao().observeAll().first()
                val now = System.currentTimeMillis()
                val next = GoldenTimeLogic.nextReminderAt(words, now)
                if (next != null) StudyReminder.scheduleGoldenTime(context, next) else StudyReminder.schedule(context)
            } catch (_: Exception) {
                StudyReminder.schedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

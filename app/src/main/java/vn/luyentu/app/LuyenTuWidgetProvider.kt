package vn.luyentu.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class LuyenTuWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateWidget(context, manager, id) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = android.content.ComponentName(context, LuyenTuWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { id -> updateWidget(context, manager, id) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.luyentu_widget)
            val db = VocabularyDatabase(context)
            val prefs = context.getSharedPreferences("luyentu_widget_prefs", Context.MODE_PRIVATE)
            val strategy = prefs.getString("word_strategy", "due") ?: "due"
            val words = db.getAllWords()
            val dueWords = db.getDueWords()
            val chosen = when (strategy) {
                "alphabetical" -> words.minByOrNull { it.term.lowercase() }
                "random" -> words.randomOrNull()
                else -> dueWords.firstOrNull() ?: words.firstOrNull()
            }
            views.setTextViewText(R.id.widget_word, chosen?.term ?: "Thêm từ đầu tiên")
            views.setTextViewText(R.id.widget_meaning, chosen?.meaning ?: "Mở LuyenTu để nhập bộ từ của bạn")
            views.setTextViewText(R.id.widget_streak, "🔥 ${db.currentStreakDays()} ngày học • ${dueWords.size} từ đến hạn")
            views.setTextViewText(R.id.widget_avatar, prefs.getString("avatar", "📚") ?: "📚")
            val intent = PendingIntent.getActivity(
                context, 7001, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, intent)
            views.setOnClickPendingIntent(R.id.widget_open, intent)
            manager.updateAppWidget(widgetId, views)
            db.close()
        }
    }
}

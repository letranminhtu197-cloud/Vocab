package com.vocab.ielts.offline

import android.content.SharedPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Local-only study history, keyed by ISO date. */
object StudyActivityTracker {
    private const val PREFIX = "study_activity_"
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun recordEvent(prefs: SharedPreferences, date: LocalDate = LocalDate.now()) {
        val key = PREFIX + date.format(formatter)
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    fun countForDate(prefs: SharedPreferences, date: LocalDate): Int =
        prefs.getInt(PREFIX + date.format(formatter), 0).coerceAtLeast(0)

    fun activeDayCount(prefs: SharedPreferences, date: LocalDate = LocalDate.now()): Int {
        var cursor = date
        var streak = 0
        if (countForDate(prefs, cursor) == 0) cursor = cursor.minusDays(1)
        while (countForDate(prefs, cursor) > 0) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun lastSevenDays(prefs: SharedPreferences, date: LocalDate = LocalDate.now()): List<DailyStudyCount> =
        (6L downTo 0L).map { offset ->
            val day = date.minusDays(offset)
            DailyStudyCount(day, countForDate(prefs, day))
        }
}

data class DailyStudyCount(val date: LocalDate, val activities: Int)

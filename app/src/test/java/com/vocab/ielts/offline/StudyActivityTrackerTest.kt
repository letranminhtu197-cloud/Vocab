package com.vocab.ielts.offline

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito
import java.time.LocalDate

class StudyActivityTrackerTest {
    private fun prefs(values: MutableMap<String, Int> = mutableMapOf()): SharedPreferences {
        val prefs = Mockito.mock(SharedPreferences::class.java)
        Mockito.`when`(prefs.getInt(Mockito.anyString(), Mockito.anyInt())).thenAnswer { call ->
            values[call.getArgument<String>(0)] ?: call.getArgument<Int>(1)
        }
        val editor = Mockito.mock(SharedPreferences.Editor::class.java)
        Mockito.`when`(prefs.edit()).thenReturn(editor)
        Mockito.`when`(editor.putInt(Mockito.anyString(), Mockito.anyInt())).thenAnswer { call ->
            values[call.getArgument<String>(0)] = call.getArgument<Int>(1)
            editor
        }
        return prefs
    }

    @Test fun recordEventIncrementsMatchingDay() {
        val values = mutableMapOf<String, Int>()
        val prefs = prefs(values)
        val day = LocalDate.of(2026, 10, 22)
        StudyActivityTracker.recordEvent(prefs, day)
        StudyActivityTracker.recordEvent(prefs, day)
        assertEquals(2, StudyActivityTracker.countForDate(prefs, day))
        assertEquals(0, StudyActivityTracker.countForDate(prefs, day.minusDays(1)))
    }

    @Test fun streakIncludesYesterdayWhenTodayIsInactive() {
        val values = mutableMapOf(
            "study_activity_2026-10-20" to 1,
            "study_activity_2026-10-21" to 2
        )
        assertEquals(2, StudyActivityTracker.activeDayCount(prefs(values), LocalDate.of(2026, 10, 22)))
    }

    @Test fun streakBreaksAtFirstInactiveDay() {
        val values = mutableMapOf(
            "study_activity_2026-10-19" to 1,
            "study_activity_2026-10-21" to 1,
            "study_activity_2026-10-22" to 1
        )
        assertEquals(2, StudyActivityTracker.activeDayCount(prefs(values), LocalDate.of(2026, 10, 22)))
    }

    @Test fun lastSevenDaysAreOldestToNewest() {
        val day = LocalDate.of(2026, 10, 22)
        val result = StudyActivityTracker.lastSevenDays(prefs(), day)
        assertEquals(7, result.size)
        assertEquals(day.minusDays(6), result.first().date)
        assertEquals(day, result.last().date)
    }
}

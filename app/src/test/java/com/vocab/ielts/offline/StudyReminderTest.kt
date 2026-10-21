package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class StudyReminderTest {
    private fun localTime(hour: Int, minute: Int): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    @Test fun reminderBeforeSevenPmUsesSameDay() {
        val now = localTime(18, 59).timeInMillis
        val scheduled = Calendar.getInstance().apply { timeInMillis = StudyReminder.nextReminderAt(now) }
        assertEquals(localTime(18, 59).get(Calendar.DAY_OF_YEAR), scheduled.get(Calendar.DAY_OF_YEAR))
        assertEquals(19, scheduled.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, scheduled.get(Calendar.MINUTE))
    }

    @Test fun reminderAfterSevenPmUsesNextDay() {
        val now = localTime(19, 1).timeInMillis
        val tomorrow = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, 1) }
        val scheduled = Calendar.getInstance().apply { timeInMillis = StudyReminder.nextReminderAt(now) }
        assertEquals(tomorrow.get(Calendar.DAY_OF_YEAR), scheduled.get(Calendar.DAY_OF_YEAR))
        assertEquals(19, scheduled.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, scheduled.get(Calendar.MINUTE))
    }
}
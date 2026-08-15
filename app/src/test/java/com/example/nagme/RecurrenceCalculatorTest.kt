package com.example.nagme

import com.example.nagme.data.Nag
import com.example.nagme.data.RecurrenceType
import com.example.nagme.logic.RecurrenceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class RecurrenceCalculatorTest {

    private fun time(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0
    ): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun once_future_returnsScheduledTime() {
        val start = time(2026, 8, 12, 18)

        val nag = Nag(
            text = "Test",
            startDateTime = start,
            recurrenceType = RecurrenceType.ONCE
        )

        val result = RecurrenceCalculator.nextOccurrence(
            nag,
            time(2026, 8, 12, 17)
        )

        assertEquals(start, result)
    }

    @Test
    fun once_past_returnsNull() {
        val nag = Nag(
            text = "Test",
            startDateTime = time(2026, 8, 12, 18),
            recurrenceType = RecurrenceType.ONCE
        )

        val result = RecurrenceCalculator.nextOccurrence(
            nag,
            time(2026, 8, 12, 19)
        )

        assertNull(result)
    }

    @Test
    fun daily_beforeTime_returnsToday() {
        val nag = Nag(
            text = "Test",
            startDateTime = time(2026, 8, 10, 18),
            recurrenceType = RecurrenceType.DAILY
        )

        val result = RecurrenceCalculator.nextOccurrence(
            nag,
            time(2026, 8, 12, 17)
        )

        assertEquals(
            time(2026, 8, 12, 18),
            result
        )
    }

    @Test
    fun daily_afterTime_returnsTomorrow() {
        val nag = Nag(
            text = "Test",
            startDateTime = time(2026, 8, 10, 18),
            recurrenceType = RecurrenceType.DAILY
        )

        val result = RecurrenceCalculator.nextOccurrence(
            nag,
            time(2026, 8, 12, 19)
        )

        assertEquals(
            time(2026, 8, 13, 18),
            result
        )
    }

    @Test
    fun weekdays_afterFriday_returnsMonday() {
        val nag = Nag(
            text = "Work Nag",
            startDateTime = time(2026, 8, 10, 18),
            recurrenceType = RecurrenceType.WEEKDAYS
        )

        // Friday 14 August 2026 after the Nag time.
        val result = RecurrenceCalculator.nextOccurrence(
            nag,
            time(2026, 8, 14, 19)
        )

        assertEquals(
            time(2026, 8, 17, 18),
            result
        )
    }

    @Test
    fun weekends_onFriday_returnsSaturday() {
        val nag = Nag(
            text = "Weekend Nag",
            startDateTime = time(2026, 8, 10, 10),
            recurrenceType = RecurrenceType.WEEKENDS
        )

        val result = RecurrenceCalculator.nextOccurrence(
            nag,
            time(2026, 8, 14, 12)
        )

        assertEquals(
            time(2026, 8, 15, 10),
            result
        )
    }

    @Test
    fun weekly_afterOccurrence_returnsNextWeek() {
        // Monday 10 August 2026 at 18:00.
        val nag = Nag(
            text = "Weekly Nag",
            startDateTime = time(2026, 8, 10, 18),
            recurrenceType = RecurrenceType.WEEKLY
        )

        val result = RecurrenceCalculator.nextOccurrence(
            nag,
            time(2026, 8, 10, 19)
        )

        assertEquals(
            time(2026, 8, 17, 18),
            result
        )
    }

    @Test
    fun selectedDays_returnsNextSelectedDay() {
        val nag = Nag(
            text = "Selected days",
            startDateTime = time(2026, 8, 10, 18),
            recurrenceType = RecurrenceType.SELECTED_DAYS,
            selectedDays = setOf(
                Calendar.MONDAY,
                Calendar.WEDNESDAY,
                Calendar.FRIDAY
            )
        )

        // Tuesday 11 August 2026.
        val result = RecurrenceCalculator.nextOccurrence(
            nag,
            time(2026, 8, 11, 19)
        )

        assertEquals(
            time(2026, 8, 12, 18),
            result
        )
    }

    @Test
    fun selectedDays_empty_returnsNull() {
        val nag = Nag(
            text = "Selected days",
            startDateTime = time(2026, 8, 10, 18),
            recurrenceType = RecurrenceType.SELECTED_DAYS,
            selectedDays = emptySet()
        )

        val result = RecurrenceCalculator.nextOccurrence(
            nag,
            time(2026, 8, 11, 12)
        )

        assertNull(result)
    }
}
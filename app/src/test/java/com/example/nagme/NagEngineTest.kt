package com.example.nagme

import com.example.nagme.data.Nag
import com.example.nagme.data.OccurrenceStatus
import com.example.nagme.data.RecurrenceType
import com.example.nagme.logic.NagEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class NagEngineTest {

    private val minute = 60_000L

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
    fun dailyNag_fullLifecycle_createsTomorrowOccurrence() = runBlocking {

        val repository = FakeNagRepository()
        val engine = NagEngine(repository)

        val startTime = time(
            year = 2026,
            month = 8,
            day = 12,
            hour = 18
        )

        val nag = Nag(
            id = 1,
            text = "Take tablets",
            startDateTime = startTime,
            recurrenceType = RecurrenceType.DAILY,
            nagIntervalMinutes = 10
        )

        repository.createNag(nag)

        // Create today's scheduled occurrence.
        val occurrenceId = engine.createInitialOccurrence(
            nag = nag,
            now = time(2026, 8, 12, 17)
        )

        assertNotNull(occurrenceId)

        val scheduled = repository.getOccurrence(
            occurrenceId!!
        )!!

        assertEquals(
            OccurrenceStatus.SCHEDULED,
            scheduled.status
        )

        assertEquals(
            startTime,
            scheduled.nextNagAt
        )

        // 6:00 PM - occurrence becomes active.
        val active = engine.startDueOccurrence(
            nag = nag,
            occurrence = scheduled,
            now = startTime
        )

        assertEquals(
            OccurrenceStatus.ACTIVE,
            active.status
        )

        // Nag #1 speaks at 6:00 PM.
        val afterFirstNag = engine.recordNagSpoken(
            nag = nag,
            occurrence = active,
            spokenAt = startTime
        )

        assertEquals(1, afterFirstNag.nagCount)

        assertEquals(
            startTime + 10 * minute,
            afterFirstNag.nextNagAt
        )

        // User snoozes at 6:05 PM for 30 minutes.
        val snoozed = engine.snoozeOccurrence(
            occurrence = afterFirstNag,
            snoozeMinutes = 30,
            now = startTime + 5 * minute
        )

        assertEquals(1, snoozed.nagCount)

        assertEquals(
            startTime + 35 * minute,
            snoozed.nextNagAt
        )

        // Nag #2 speaks when snooze ends at 6:35 PM.
        val afterSecondNag = engine.recordNagSpoken(
            nag = nag,
            occurrence = snoozed,
            spokenAt = startTime + 35 * minute
        )

        assertEquals(2, afterSecondNag.nagCount)

        assertEquals(
            startTime + 45 * minute,
            afterSecondNag.nextNagAt
        )

        // User presses DONE at 6:37 PM.
        val completed = engine.completeOccurrence(
            nag = nag,
            occurrence = afterSecondNag,
            completedAt = startTime + 37 * minute
        )

        assertEquals(
            OccurrenceStatus.DONE,
            completed.status
        )

        assertNull(completed.nextNagAt)

        assertEquals(
            startTime + 37 * minute,
            completed.completedAt
        )

        // The completed occurrence remains as history and
        // tomorrow's occurrence should now exist.
        val allOccurrences = repository.allOccurrences()

        assertEquals(2, allOccurrences.size)

        val historicalOccurrence = allOccurrences.first {
            it.id == occurrenceId
        }

        assertEquals(
            OccurrenceStatus.DONE,
            historicalOccurrence.status
        )

        val tomorrow = allOccurrences.first {
            it.id != occurrenceId
        }

        assertEquals(
            OccurrenceStatus.SCHEDULED,
            tomorrow.status
        )

        assertEquals(
            time(2026, 8, 13, 18),
            tomorrow.scheduledAt
        )

        assertEquals(
            time(2026, 8, 13, 18),
            tomorrow.nextNagAt
        )
    }

    @Test
    fun onceNag_done_doesNotCreateAnotherOccurrence() = runBlocking {

        val repository = FakeNagRepository()
        val engine = NagEngine(repository)

        val startTime = time(2026, 8, 12, 18)

        val nag = Nag(
            id = 1,
            text = "Put bins out",
            startDateTime = startTime,
            recurrenceType = RecurrenceType.ONCE,
            nagIntervalMinutes = 10
        )

        repository.createNag(nag)

        val occurrenceId = engine.createInitialOccurrence(
            nag = nag,
            now = time(2026, 8, 12, 17)
        )!!

        val scheduled =
            repository.getOccurrence(occurrenceId)!!

        val active = engine.startDueOccurrence(
            nag = nag,
            occurrence = scheduled,
            now = startTime
        )

        val completed = engine.completeOccurrence(
            nag = nag,
            occurrence = active,
            completedAt = startTime + minute
        )

        assertEquals(
            OccurrenceStatus.DONE,
            completed.status
        )

        assertEquals(
            1,
            repository.allOccurrences().size
        )
    }

    @Test
    fun disabledNag_doesNotCreateOccurrence() = runBlocking {

        val repository = FakeNagRepository()
        val engine = NagEngine(repository)

        val nag = Nag(
            id = 1,
            text = "Disabled",
            enabled = false,
            startDateTime = time(2026, 8, 12, 18),
            recurrenceType = RecurrenceType.DAILY
        )

        repository.createNag(nag)

        val result = engine.createInitialOccurrence(
            nag = nag,
            now = time(2026, 8, 12, 17)
        )

        assertNull(result)
        assertEquals(
            0,
            repository.allOccurrences().size
        )
    }
}
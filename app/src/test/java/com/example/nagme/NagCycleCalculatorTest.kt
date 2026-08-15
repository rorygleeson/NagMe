package com.example.nagme

import com.example.nagme.data.Nag
import com.example.nagme.data.NagOccurrence
import com.example.nagme.data.OccurrenceStatus
import com.example.nagme.logic.NagCycleCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NagCycleCalculatorTest {

    private val minute = 60_000L

    private fun baseNag(
        nagIntervalMinutes: Int? = 10,
        maxNagCount: Int? = null,
        maxNagDurationMinutes: Int? = null
    ): Nag {
        return Nag(
            text = "Test Nag",
            startDateTime = 1_000_000L,
            nagIntervalMinutes = nagIntervalMinutes,
            maxNagCount = maxNagCount,
            maxNagDurationMinutes = maxNagDurationMinutes
        )
    }

    private fun baseOccurrence(): NagOccurrence {
        return NagOccurrence(
            nagId = 1,
            scheduledAt = 1_000_000L,
            nextNagAt = 1_000_000L
        )
    }

    @Test
    fun startOccurrence_setsActiveAndDueNow() {
        val now = 2_000_000L

        val result = NagCycleCalculator.startOccurrence(
            nag = baseNag(),
            occurrence = baseOccurrence(),
            now = now
        )

        assertEquals(OccurrenceStatus.ACTIVE, result.status)
        assertEquals(now, result.startedAt)
        assertEquals(now, result.nextNagAt)
        assertEquals(0, result.nagCount)
    }

    @Test
    fun afterNagSpoken_schedulesNextNag() {
        val spokenAt = 2_000_000L

        val started = NagCycleCalculator.startOccurrence(
            nag = baseNag(nagIntervalMinutes = 10),
            occurrence = baseOccurrence(),
            now = spokenAt
        )

        val result = NagCycleCalculator.afterNagSpoken(
            nag = baseNag(nagIntervalMinutes = 10),
            occurrence = started,
            spokenAt = spokenAt
        )

        assertEquals(OccurrenceStatus.ACTIVE, result.status)
        assertEquals(1, result.nagCount)
        assertEquals(spokenAt, result.lastNagAt)
        assertEquals(
            spokenAt + 10 * minute,
            result.nextNagAt
        )
    }

    @Test
    fun maxNagCount_expiresAtLimit() {
        val nag = baseNag(
            nagIntervalMinutes = 5,
            maxNagCount = 3
        )

        val occurrence = baseOccurrence().copy(
            status = OccurrenceStatus.ACTIVE,
            nagCount = 2
        )

        val result = NagCycleCalculator.afterNagSpoken(
            nag = nag,
            occurrence = occurrence,
            spokenAt = 2_000_000L
        )

        assertEquals(OccurrenceStatus.EXPIRED, result.status)
        assertEquals(3, result.nagCount)
        assertNull(result.nextNagAt)
    }

    @Test
    fun maxDuration_setsExpiresAtWhenOccurrenceStarts() {
        val now = 2_000_000L

        val result = NagCycleCalculator.startOccurrence(
            nag = baseNag(maxNagDurationMinutes = 60),
            occurrence = baseOccurrence(),
            now = now
        )

        assertEquals(
            now + 60 * minute,
            result.expiresAt
        )
    }

    @Test
    fun maxDuration_expiresWhenNextNagWouldBeTooLate() {
        val now = 2_000_000L

        val nag = baseNag(
            nagIntervalMinutes = 10,
            maxNagDurationMinutes = 15
        )

        val started = NagCycleCalculator.startOccurrence(
            nag = nag,
            occurrence = baseOccurrence(),
            now = now
        )

        val first = NagCycleCalculator.afterNagSpoken(
            nag = nag,
            occurrence = started,
            spokenAt = now
        )

        val result = NagCycleCalculator.afterNagSpoken(
            nag = nag,
            occurrence = first,
            spokenAt = now + 10 * minute
        )

        assertEquals(OccurrenceStatus.EXPIRED, result.status)
        assertEquals(2, result.nagCount)
        assertNull(result.nextNagAt)
    }

    @Test
    fun snooze_doesNotResetNagCount() {
        val now = 2_000_000L

        val occurrence = baseOccurrence().copy(
            status = OccurrenceStatus.ACTIVE,
            nagCount = 3,
            nextNagAt = now
        )

        val result = NagCycleCalculator.snooze(
            occurrence = occurrence,
            now = now,
            snoozeMinutes = 30
        )

        assertEquals(OccurrenceStatus.ACTIVE, result.status)
        assertEquals(3, result.nagCount)
        assertEquals(
            now + 30 * minute,
            result.nextNagAt
        )
        assertEquals(
            now + 30 * minute,
            result.snoozedUntil
        )
    }

    @Test
    fun complete_stopsImmediately() {
        val completedAt = 3_000_000L

        val occurrence = baseOccurrence().copy(
            status = OccurrenceStatus.ACTIVE,
            nagCount = 4,
            nextNagAt = 4_000_000L
        )

        val result = NagCycleCalculator.complete(
            occurrence = occurrence,
            completedAt = completedAt
        )

        assertEquals(OccurrenceStatus.DONE, result.status)
        assertEquals(completedAt, result.completedAt)
        assertNull(result.nextNagAt)
        assertNull(result.snoozedUntil)
        assertEquals(4, result.nagCount)
    }

    @Test
    fun nagOnce_expiresAfterFirstSpokenNag() {
        val nag = baseNag(
            nagIntervalMinutes = null
        )

        val started = NagCycleCalculator.startOccurrence(
            nag = nag,
            occurrence = baseOccurrence(),
            now = 2_000_000L
        )

        val result = NagCycleCalculator.afterNagSpoken(
            nag = nag,
            occurrence = started,
            spokenAt = 2_000_000L
        )

        assertEquals(OccurrenceStatus.EXPIRED, result.status)
        assertEquals(1, result.nagCount)
        assertNull(result.nextNagAt)
    }

    @Test
    fun snoozePastExpiry_expiresOccurrence() {
        val now = 2_000_000L

        val occurrence = baseOccurrence().copy(
            status = OccurrenceStatus.ACTIVE,
            nagCount = 2,
            expiresAt = now + 20 * minute
        )

        val result = NagCycleCalculator.snooze(
            occurrence = occurrence,
            now = now,
            snoozeMinutes = 30
        )

        assertEquals(OccurrenceStatus.EXPIRED, result.status)
        assertNull(result.nextNagAt)
        assertNull(result.snoozedUntil)
    }
}
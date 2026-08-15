package com.example.nagme.logic

import com.example.nagme.data.Nag
import com.example.nagme.data.NagOccurrence
import com.example.nagme.data.NagRepository
import com.example.nagme.data.OccurrenceStatus

class NagEngine(
    private val repository: NagRepository
) {

    suspend fun createInitialOccurrence(
        nag: Nag,
        now: Long = System.currentTimeMillis()
    ): Long? {

        if (!nag.enabled) return null

        val nextTime = RecurrenceCalculator.nextOccurrence(
            nag = nag,
            afterTime = now - 1
        ) ?: return null

        val occurrence = NagOccurrence(
            nagId = nag.id,
            scheduledAt = nextTime,
            status = OccurrenceStatus.SCHEDULED,
            nextNagAt = nextTime
        )

        return repository.createOccurrence(occurrence)
    }

    suspend fun startDueOccurrence(
        nag: Nag,
        occurrence: NagOccurrence,
        now: Long = System.currentTimeMillis()
    ): NagOccurrence {

        val updated = NagCycleCalculator.startOccurrence(
            nag = nag,
            occurrence = occurrence,
            now = now
        )

        repository.updateOccurrence(updated)

        return updated
    }

    suspend fun recordNagSpoken(
        nag: Nag,
        occurrence: NagOccurrence,
        spokenAt: Long = System.currentTimeMillis()
    ): NagOccurrence {

        val updated = NagCycleCalculator.afterNagSpoken(
            nag = nag,
            occurrence = occurrence,
            spokenAt = spokenAt
        )

        repository.updateOccurrence(updated)

        if (updated.status == OccurrenceStatus.EXPIRED) {
            createNextRecurringOccurrence(
                nag = nag,
                completedOccurrence = updated,
                referenceTime = spokenAt
            )
        }

        return updated
    }

    suspend fun snoozeOccurrence(
        occurrence: NagOccurrence,
        snoozeMinutes: Int,
        now: Long = System.currentTimeMillis()
    ): NagOccurrence {

        val updated = NagCycleCalculator.snooze(
            occurrence = occurrence,
            now = now,
            snoozeMinutes = snoozeMinutes
        )

        repository.updateOccurrence(updated)

        return updated
    }

    suspend fun completeOccurrence(
        nag: Nag,
        occurrence: NagOccurrence,
        completedAt: Long = System.currentTimeMillis()
    ): NagOccurrence {

        val updated = NagCycleCalculator.complete(
            occurrence = occurrence,
            completedAt = completedAt
        )

        repository.updateOccurrence(updated)

        createNextRecurringOccurrence(
            nag = nag,
            completedOccurrence = updated,
            referenceTime = completedAt
        )

        return updated
    }

    suspend fun createNextRecurringOccurrence(
        nag: Nag,
        completedOccurrence: NagOccurrence,
        referenceTime: Long
    ): Long? {

        if (!nag.enabled) return null

        val nextTime = RecurrenceCalculator.nextOccurrence(
            nag = nag,
            afterTime = referenceTime
        ) ?: return null

        val nextOccurrence = NagOccurrence(
            nagId = nag.id,
            scheduledAt = nextTime,
            status = OccurrenceStatus.SCHEDULED,
            nextNagAt = nextTime
        )

        return repository.createOccurrence(nextOccurrence)
    }
}
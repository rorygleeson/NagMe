package com.example.nagme.logic

import com.example.nagme.data.Nag
import com.example.nagme.data.NagOccurrence
import com.example.nagme.data.OccurrenceStatus

object NagCycleCalculator {

    fun startOccurrence(
        nag: Nag,
        occurrence: NagOccurrence,
        now: Long
    ): NagOccurrence {

        val expiresAt = nag.maxNagDurationMinutes?.let {
            now + it * 60_000L
        }

        return occurrence.copy(
            startedAt = now,
            status = OccurrenceStatus.ACTIVE,
            nagCount = 0,
            lastNagAt = null,
            nextNagAt = now,
            snoozedUntil = null,
            expiresAt = expiresAt,
            completedAt = null
        )
    }

    fun afterNagSpoken(
        nag: Nag,
        occurrence: NagOccurrence,
        spokenAt: Long
    ): NagOccurrence {

        val newCount = occurrence.nagCount + 1

        if (nag.maxNagCount != null && newCount >= nag.maxNagCount) {
            return occurrence.copy(
                status = OccurrenceStatus.EXPIRED,
                nagCount = newCount,
                lastNagAt = spokenAt,
                nextNagAt = null,
                snoozedUntil = null
            )
        }

        if (occurrence.expiresAt != null && spokenAt >= occurrence.expiresAt) {
            return occurrence.copy(
                status = OccurrenceStatus.EXPIRED,
                nagCount = newCount,
                lastNagAt = spokenAt,
                nextNagAt = null,
                snoozedUntil = null
            )
        }

        val interval = nag.nagIntervalMinutes

        if (interval == null) {
            return occurrence.copy(
                status = OccurrenceStatus.EXPIRED,
                nagCount = newCount,
                lastNagAt = spokenAt,
                nextNagAt = null,
                snoozedUntil = null
            )
        }

        val proposedNextNag = spokenAt + interval * 60_000L

        if (
            occurrence.expiresAt != null &&
            proposedNextNag > occurrence.expiresAt
        ) {
            return occurrence.copy(
                status = OccurrenceStatus.EXPIRED,
                nagCount = newCount,
                lastNagAt = spokenAt,
                nextNagAt = null,
                snoozedUntil = null
            )
        }

        return occurrence.copy(
            status = OccurrenceStatus.ACTIVE,
            nagCount = newCount,
            lastNagAt = spokenAt,
            nextNagAt = proposedNextNag,
            snoozedUntil = null
        )
    }

    fun snooze(
        occurrence: NagOccurrence,
        now: Long,
        snoozeMinutes: Int
    ): NagOccurrence {

        require(snoozeMinutes > 0)

        val resumeAt = now + snoozeMinutes * 60_000L

        if (
            occurrence.expiresAt != null &&
            resumeAt > occurrence.expiresAt
        ) {
            return occurrence.copy(
                status = OccurrenceStatus.EXPIRED,
                nextNagAt = null,
                snoozedUntil = null
            )
        }

        return occurrence.copy(
            status = OccurrenceStatus.ACTIVE,
            nextNagAt = resumeAt,
            snoozedUntil = resumeAt
        )
    }

    fun complete(
        occurrence: NagOccurrence,
        completedAt: Long
    ): NagOccurrence {

        return occurrence.copy(
            status = OccurrenceStatus.DONE,
            nextNagAt = null,
            snoozedUntil = null,
            completedAt = completedAt
        )
    }
}
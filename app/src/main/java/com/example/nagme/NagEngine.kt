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

        /*
         * Do not create another occurrence if this Nag
         * already has an ACTIVE or SCHEDULED occurrence.
         */
        val existingOpenOccurrence =
            repository
                .getOpenOccurrencesForNag(nag.id)
                .firstOrNull {
                    it.status == OccurrenceStatus.ACTIVE ||
                            it.status == OccurrenceStatus.SCHEDULED
                }

        if (existingOpenOccurrence != null) {
            return existingOpenOccurrence.id
        }

        val nextTime =
            RecurrenceCalculator.nextOccurrence(
                nag = nag,
                afterTime = now - 1
            ) ?: return null

        val occurrence =
            NagOccurrence(
                nagId = nag.id,
                scheduledAt = nextTime,
                status = OccurrenceStatus.SCHEDULED,
                nextNagAt = nextTime
            )

        return repository.createOccurrence(
            occurrence
        )
    }

    suspend fun startDueOccurrence(
        nag: Nag,
        occurrence: NagOccurrence,
        now: Long = System.currentTimeMillis()
    ): NagOccurrence {

        /*
         * IMPORTANT:
         *
         * Re-read from Room before changing anything.
         *
         * The occurrence passed to us may be stale if the
         * user pressed DONE/SNOOZE while another component
         * still held an older copy.
         */
        val currentOccurrence =
            repository.getOccurrence(
                occurrence.id
            ) ?: return occurrence

        /*
         * Never resurrect a finished occurrence.
         */
        if (
            currentOccurrence.status ==
            OccurrenceStatus.DONE ||
            currentOccurrence.status ==
            OccurrenceStatus.EXPIRED
        ) {
            return currentOccurrence
        }

        /*
         * If another execution already started this
         * occurrence, use the current Room state rather
         * than resetting it.
         */
        if (
            currentOccurrence.status ==
            OccurrenceStatus.ACTIVE
        ) {
            return currentOccurrence
        }

        val updated =
            NagCycleCalculator.startOccurrence(
                nag = nag,
                occurrence = currentOccurrence,
                now = now
            )

        repository.updateOccurrence(
            updated
        )

        return updated
    }

    suspend fun recordNagSpoken(
        nag: Nag,
        occurrence: NagOccurrence,
        spokenAt: Long = System.currentTimeMillis()
    ): NagOccurrence {

        /*
         * CRITICAL STALE-STATE PROTECTION
         *
         * Speech can finish after the user has already
         * pressed DONE.
         *
         * Never use the old occurrence object supplied
         * by the service as the source of truth.
         */
        val currentOccurrence =
            repository.getOccurrence(
                occurrence.id
            ) ?: return occurrence

        /*
         * If DONE or EXPIRED has already been recorded,
         * speech completion must not overwrite it and
         * must not schedule another Nag.
         */
        if (
            currentOccurrence.status ==
            OccurrenceStatus.DONE ||
            currentOccurrence.status ==
            OccurrenceStatus.EXPIRED
        ) {
            return currentOccurrence
        }

        val updated =
            NagCycleCalculator.afterNagSpoken(
                nag = nag,
                occurrence = currentOccurrence,
                spokenAt = spokenAt
            )

        repository.updateOccurrence(
            updated
        )

        if (
            updated.status ==
            OccurrenceStatus.EXPIRED
        ) {

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

        /*
         * Always work from the latest database state.
         */
        val currentOccurrence =
            repository.getOccurrence(
                occurrence.id
            ) ?: return occurrence

        /*
         * A completed occurrence cannot be snoozed.
         */
        if (
            currentOccurrence.status ==
            OccurrenceStatus.DONE ||
            currentOccurrence.status ==
            OccurrenceStatus.EXPIRED
        ) {
            return currentOccurrence
        }

        val updated =
            NagCycleCalculator.snooze(
                occurrence = currentOccurrence,
                now = now,
                snoozeMinutes = snoozeMinutes
            )

        repository.updateOccurrence(
            updated
        )

        return updated
    }

    suspend fun completeOccurrence(
        nag: Nag,
        occurrence: NagOccurrence,
        completedAt: Long = System.currentTimeMillis()
    ): NagOccurrence {

        /*
         * Re-read the occurrence from Room.
         *
         * This makes DONE idempotent at the engine level:
         * if this occurrence has already been completed,
         * do not complete it again and do not create
         * another recurring occurrence.
         */
        val currentOccurrence =
            repository.getOccurrence(
                occurrence.id
            ) ?: return occurrence

        if (
            currentOccurrence.status ==
            OccurrenceStatus.DONE
        ) {
            return currentOccurrence
        }

        /*
         * If the occurrence already expired naturally,
         * its next recurring occurrence may already have
         * been created by recordNagSpoken().
         *
         * Do not create another one here.
         */
        if (
            currentOccurrence.status ==
            OccurrenceStatus.EXPIRED
        ) {
            return currentOccurrence
        }

        val updated =
            NagCycleCalculator.complete(
                occurrence = currentOccurrence,
                completedAt = completedAt
            )

        repository.updateOccurrence(
            updated
        )

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

        /*
         * Before creating anything, check whether this Nag
         * already has another ACTIVE/SCHEDULED occurrence.
         *
         * This prevents the normal duplicate path where
         * multiple lifecycle callbacks try to create the
         * same next occurrence.
         */
        val existingOpenOccurrence =
            repository
                .getOpenOccurrencesForNag(
                    nag.id
                )
                .firstOrNull {
                    it.id != completedOccurrence.id &&
                            (
                                    it.status ==
                                            OccurrenceStatus.ACTIVE ||
                                            it.status ==
                                            OccurrenceStatus.SCHEDULED
                                    )
                }

        if (existingOpenOccurrence != null) {
            return existingOpenOccurrence.id
        }

        val nextTime =
            RecurrenceCalculator.nextOccurrence(
                nag = nag,
                afterTime = referenceTime
            ) ?: return null

        /*
         * Check once more specifically for an occurrence
         * at the same calculated time.
         */
        val existingAtSameTime =
            repository
                .getOpenOccurrencesForNag(
                    nag.id
                )
                .firstOrNull {
                    it.id != completedOccurrence.id &&
                            (
                                    it.status ==
                                            OccurrenceStatus.ACTIVE ||
                                            it.status ==
                                            OccurrenceStatus.SCHEDULED
                                    ) &&
                            it.scheduledAt == nextTime
                }

        if (existingAtSameTime != null) {
            return existingAtSameTime.id
        }

        val nextOccurrence =
            NagOccurrence(
                nagId = nag.id,
                scheduledAt = nextTime,
                status = OccurrenceStatus.SCHEDULED,
                nextNagAt = nextTime
            )

        return repository.createOccurrence(
            nextOccurrence
        )
    }
}
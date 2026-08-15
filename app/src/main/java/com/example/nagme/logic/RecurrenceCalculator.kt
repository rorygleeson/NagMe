package com.example.nagme.logic

import com.example.nagme.data.Nag
import com.example.nagme.data.RecurrenceType
import java.util.Calendar

object RecurrenceCalculator {

    /**
     * Returns the next scheduled occurrence strictly after [afterTime].
     *
     * For ONCE, returns the Nag's startDateTime only if it has not
     * already passed relative to afterTime.
     */
    fun nextOccurrence(
        nag: Nag,
        afterTime: Long
    ): Long? {

        return when (nag.recurrenceType) {

            RecurrenceType.ONCE ->
                nextOnce(nag, afterTime)

            RecurrenceType.DAILY ->
                nextDaily(nag, afterTime)

            RecurrenceType.WEEKDAYS ->
                nextMatchingDay(
                    nag = nag,
                    afterTime = afterTime,
                    allowedDays = setOf(
                        Calendar.MONDAY,
                        Calendar.TUESDAY,
                        Calendar.WEDNESDAY,
                        Calendar.THURSDAY,
                        Calendar.FRIDAY
                    )
                )

            RecurrenceType.WEEKENDS ->
                nextMatchingDay(
                    nag = nag,
                    afterTime = afterTime,
                    allowedDays = setOf(
                        Calendar.SATURDAY,
                        Calendar.SUNDAY
                    )
                )

            RecurrenceType.WEEKLY ->
                nextWeekly(nag, afterTime)

            RecurrenceType.SELECTED_DAYS ->
                nextMatchingDay(
                    nag = nag,
                    afterTime = afterTime,
                    allowedDays = nag.selectedDays
                )
        }
    }

    private fun nextOnce(
        nag: Nag,
        afterTime: Long
    ): Long? {
        return if (nag.startDateTime > afterTime) {
            nag.startDateTime
        } else {
            null
        }
    }

    private fun nextDaily(
        nag: Nag,
        afterTime: Long
    ): Long {

        if (nag.startDateTime > afterTime) {
            return nag.startDateTime
        }

        val candidate = calendarAtNagTime(
            nag = nag,
            dateReference = afterTime
        )

        if (candidate.timeInMillis <= afterTime) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }

        return candidate.timeInMillis
    }

    private fun nextWeekly(
        nag: Nag,
        afterTime: Long
    ): Long? {

        if (nag.startDateTime > afterTime) {
            return nag.startDateTime
        }

        val start = Calendar.getInstance().apply {
            timeInMillis = nag.startDateTime
        }

        return nextMatchingDay(
            nag = nag,
            afterTime = afterTime,
            allowedDays = setOf(start.get(Calendar.DAY_OF_WEEK))
        )
    }

    private fun nextMatchingDay(
        nag: Nag,
        afterTime: Long,
        allowedDays: Set<Int>
    ): Long? {

        if (allowedDays.isEmpty()) {
            return null
        }

        /*
         * The first occurrence can never be before the Nag's
         * configured startDateTime.
         */
        if (nag.startDateTime > afterTime) {

            val start = Calendar.getInstance().apply {
                timeInMillis = nag.startDateTime
            }

            if (start.get(Calendar.DAY_OF_WEEK) in allowedDays) {
                return nag.startDateTime
            }
        }

        val candidate = calendarAtNagTime(
            nag = nag,
            dateReference = maxOf(
                afterTime,
                nag.startDateTime
            )
        )

        /*
         * Search at most the next 8 days.
         * Every supported weekly pattern must match within 7.
         */
        repeat(8) {

            val day = candidate.get(Calendar.DAY_OF_WEEK)

            if (
                day in allowedDays &&
                candidate.timeInMillis > afterTime &&
                candidate.timeInMillis >= nag.startDateTime
            ) {
                return candidate.timeInMillis
            }

            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }

        return null
    }

    /**
     * Creates a Calendar on the date represented by dateReference,
     * but using the hour/minute/second from the Nag's startDateTime.
     */
    private fun calendarAtNagTime(
        nag: Nag,
        dateReference: Long
    ): Calendar {

        val nagTime = Calendar.getInstance().apply {
            timeInMillis = nag.startDateTime
        }

        return Calendar.getInstance().apply {

            timeInMillis = dateReference

            set(
                Calendar.HOUR_OF_DAY,
                nagTime.get(Calendar.HOUR_OF_DAY)
            )

            set(
                Calendar.MINUTE,
                nagTime.get(Calendar.MINUTE)
            )

            set(
                Calendar.SECOND,
                nagTime.get(Calendar.SECOND)
            )

            set(Calendar.MILLISECOND, 0)
        }
    }
}
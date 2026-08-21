package com.example.nagme.logic

import com.example.nagme.data.Nag
import com.example.nagme.data.RecurrenceType
import java.util.Calendar

object RecurrenceCalculator {

    /**
     * Returns the next scheduled occurrence strictly after [afterTime].
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

            RecurrenceType.DURING_TIME_PERIOD ->
                nextDuringTimePeriod(
                    nag = nag,
                    afterTime = afterTime
                )
        }
    }

    private fun nextOnce(
        nag: Nag,
        afterTime: Long
    ): Long? {

        return if (
            nag.startDateTime > afterTime
        ) {
            nag.startDateTime
        } else {
            null
        }
    }

    private fun nextDaily(
        nag: Nag,
        afterTime: Long
    ): Long {

        if (
            nag.startDateTime > afterTime
        ) {
            return nag.startDateTime
        }

        val candidate =
            calendarAtNagTime(
                nag = nag,
                dateReference = afterTime
            )

        if (
            candidate.timeInMillis <= afterTime
        ) {
            candidate.add(
                Calendar.DAY_OF_YEAR,
                1
            )
        }

        return candidate.timeInMillis
    }

    private fun nextWeekly(
        nag: Nag,
        afterTime: Long
    ): Long? {

        if (
            nag.startDateTime > afterTime
        ) {
            return nag.startDateTime
        }

        val start =
            Calendar.getInstance().apply {
                timeInMillis =
                    nag.startDateTime
            }

        return nextMatchingDay(
            nag = nag,
            afterTime = afterTime,
            allowedDays =
                setOf(
                    start.get(
                        Calendar.DAY_OF_WEEK
                    )
                )
        )
    }

    private fun nextMatchingDay(
        nag: Nag,
        afterTime: Long,
        allowedDays: Set<Int>
    ): Long? {

        if (
            allowedDays.isEmpty()
        ) {
            return null
        }

        if (
            nag.startDateTime > afterTime
        ) {

            val start =
                Calendar.getInstance().apply {
                    timeInMillis =
                        nag.startDateTime
                }

            if (
                start.get(
                    Calendar.DAY_OF_WEEK
                ) in allowedDays
            ) {
                return nag.startDateTime
            }
        }

        val candidate =
            calendarAtNagTime(
                nag = nag,
                dateReference =
                    maxOf(
                        afterTime,
                        nag.startDateTime
                    )
            )

        repeat(8) {

            val day =
                candidate.get(
                    Calendar.DAY_OF_WEEK
                )

            if (
                day in allowedDays &&
                candidate.timeInMillis >
                afterTime &&
                candidate.timeInMillis >=
                nag.startDateTime
            ) {
                return candidate.timeInMillis
            }

            candidate.add(
                Calendar.DAY_OF_YEAR,
                1
            )
        }

        return null
    }

    /**
     * Finds the next occurrence within a repeating
     * time period.
     *
     * Example:
     *
     * Every 60 minutes
     * From 09:00
     * Until 17:00
     * Monday-Friday
     *
     * produces:
     *
     * 09:00, 10:00, 11:00 ... 17:00
     *
     * Only the first valid occurrence strictly after
     * [afterTime] is returned.
     */
    private fun nextDuringTimePeriod(
        nag: Nag,
        afterTime: Long
    ): Long? {

        val intervalMinutes =
            nag.timePeriodIntervalMinutes
                ?: return null

        val endMinutesOfDay =
            nag.timePeriodEndMinutesOfDay
                ?: return null

        if (
            intervalMinutes <= 0 ||
            nag.selectedDays.isEmpty()
        ) {
            return null
        }

        val startTime =
            Calendar.getInstance().apply {
                timeInMillis =
                    nag.startDateTime
            }

        val startMinutesOfDay =
            startTime.get(
                Calendar.HOUR_OF_DAY
            ) * 60 +
                    startTime.get(
                        Calendar.MINUTE
                    )

        if (
            endMinutesOfDay <
            startMinutesOfDay
        ) {
            return null
        }

        /*
         * Search forward far enough to guarantee that
         * at least one selected weekday can be found.
         */
        val searchDate =
            Calendar.getInstance().apply {

                timeInMillis =
                    maxOf(
                        afterTime,
                        nag.startDateTime
                    )

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        repeat(8) {

            val dayOfWeek =
                searchDate.get(
                    Calendar.DAY_OF_WEEK
                )

            if (
                dayOfWeek in
                nag.selectedDays
            ) {

                var slotMinutes =
                    startMinutesOfDay

                while (
                    slotMinutes <=
                    endMinutesOfDay
                ) {

                    val candidate =
                        searchDate.clone()
                                as Calendar

                    candidate.set(
                        Calendar.HOUR_OF_DAY,
                        slotMinutes / 60
                    )

                    candidate.set(
                        Calendar.MINUTE,
                        slotMinutes % 60
                    )

                    candidate.set(
                        Calendar.SECOND,
                        0
                    )

                    candidate.set(
                        Calendar.MILLISECOND,
                        0
                    )

                    if (
                        candidate.timeInMillis >
                        afterTime &&
                        candidate.timeInMillis >=
                        nag.startDateTime
                    ) {
                        return candidate.timeInMillis
                    }

                    slotMinutes +=
                        intervalMinutes
                }
            }

            searchDate.add(
                Calendar.DAY_OF_YEAR,
                1
            )
        }

        return null
    }

    /**
     * Creates a Calendar on the date represented by
     * dateReference, but using the hour/minute/second
     * from the Nag's startDateTime.
     */
    private fun calendarAtNagTime(
        nag: Nag,
        dateReference: Long
    ): Calendar {

        val nagTime =
            Calendar.getInstance().apply {
                timeInMillis =
                    nag.startDateTime
            }

        return Calendar
            .getInstance()
            .apply {

                timeInMillis =
                    dateReference

                set(
                    Calendar.HOUR_OF_DAY,
                    nagTime.get(
                        Calendar.HOUR_OF_DAY
                    )
                )

                set(
                    Calendar.MINUTE,
                    nagTime.get(
                        Calendar.MINUTE
                    )
                )

                set(
                    Calendar.SECOND,
                    nagTime.get(
                        Calendar.SECOND
                    )
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }
    }
}
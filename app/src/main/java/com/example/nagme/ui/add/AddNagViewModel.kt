package com.example.nagme.ui.add

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nagme.data.Nag
import com.example.nagme.data.NagDatabase
import com.example.nagme.data.RecurrenceType
import com.example.nagme.data.RoomNagRepository
import com.example.nagme.logic.NagEngine
import com.example.nagme.scheduler.NagScheduler
import kotlinx.coroutines.launch

class AddNagViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        RoomNagRepository(
            NagDatabase.getInstance(application)
        )

    private val engine =
        NagEngine(repository)

    private val scheduler =
        NagScheduler(
            context = application,
            repository = repository
        )

    fun canScheduleExactAlarms(): Boolean {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.S
        ) {
            return true
        }

        val alarmManager =
            getApplication<Application>()
                .getSystemService(
                    Context.ALARM_SERVICE
                ) as AlarmManager

        return alarmManager
            .canScheduleExactAlarms()
    }

    fun saveNag(
        text: String,
        scheduledAt: Long,
        recurrenceType: RecurrenceType,
        selectedDays: Set<Int>,
        timePeriodIntervalMinutes: Int?,
        timePeriodEndMinutesOfDay: Int?,
        nagIntervalMinutes: Int?,
        stopWhenDone: Boolean,
        maxNagCount: Int?,
        maxNagDurationMinutes: Int?,
        onSaved: () -> Unit,
        onError: (String) -> Unit
    ) {

        if (text.isBlank()) {
            onError(
                "Enter something to nag you about."
            )
            return
        }

        if (
            scheduledAt <=
            System.currentTimeMillis()
        ) {
            onError(
                "Choose a time in the future."
            )
            return
        }

        if (
            recurrenceType ==
            RecurrenceType.SELECTED_DAYS &&
            selectedDays.isEmpty()
        ) {
            onError(
                "Choose at least one day."
            )
            return
        }

        if (
            recurrenceType ==
            RecurrenceType.DURING_TIME_PERIOD
        ) {

            if (
                selectedDays.isEmpty()
            ) {
                onError(
                    "Choose at least one day."
                )
                return
            }

            if (
                timePeriodIntervalMinutes == null ||
                timePeriodIntervalMinutes <= 0
            ) {
                onError(
                    "Choose how often this Nag should repeat during the time period."
                )
                return
            }

            if (
                timePeriodEndMinutesOfDay == null
            ) {
                onError(
                    "Choose an end time for the time period."
                )
                return
            }

            val startCalendar =
                java.util.Calendar
                    .getInstance()
                    .apply {
                        timeInMillis =
                            scheduledAt
                    }

            val startMinutesOfDay =
                startCalendar.get(
                    java.util.Calendar.HOUR_OF_DAY
                ) * 60 +
                        startCalendar.get(
                            java.util.Calendar.MINUTE
                        )

            if (
                timePeriodEndMinutesOfDay <=
                startMinutesOfDay
            ) {
                onError(
                    "End time must be after the start time."
                )
                return
            }
        }

        if (
            nagIntervalMinutes != null &&
            nagIntervalMinutes <= 0
        ) {
            onError(
                "Nag interval must be greater than zero."
            )
            return
        }

        if (
            maxNagCount != null &&
            maxNagCount <= 0
        ) {
            onError(
                "Maximum nags must be greater than zero."
            )
            return
        }

        if (
            maxNagDurationMinutes != null &&
            maxNagDurationMinutes <= 0
        ) {
            onError(
                "Maximum duration must be greater than zero."
            )
            return
        }

        viewModelScope.launch {

            try {

                val nag =
                    Nag(
                        text =
                            text.trim(),

                        startDateTime =
                            scheduledAt,

                        recurrenceType =
                            recurrenceType,

                        selectedDays =
                            selectedDays,

                        timePeriodIntervalMinutes =
                            if (
                                recurrenceType ==
                                RecurrenceType.DURING_TIME_PERIOD
                            ) {
                                timePeriodIntervalMinutes
                            } else {
                                null
                            },

                        timePeriodEndMinutesOfDay =
                            if (
                                recurrenceType ==
                                RecurrenceType.DURING_TIME_PERIOD
                            ) {
                                timePeriodEndMinutesOfDay
                            } else {
                                null
                            },

                        nagIntervalMinutes =
                            nagIntervalMinutes,

                        stopWhenDone =
                            stopWhenDone,

                        maxNagCount =
                            maxNagCount,

                        maxNagDurationMinutes =
                            maxNagDurationMinutes
                    )

                val nagId =
                    repository.createNag(
                        nag
                    )

                val savedNag =
                    nag.copy(
                        id = nagId
                    )

                engine
                    .createInitialOccurrence(
                        nag = savedNag,
                        now =
                            System.currentTimeMillis()
                    )

                scheduler.reschedule()

                onSaved()

            } catch (
                e: Exception
            ) {

                onError(
                    e.message
                        ?: "Nag could not be saved."
                )
            }
        }
    }
}
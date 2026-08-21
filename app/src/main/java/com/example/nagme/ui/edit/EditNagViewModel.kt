package com.example.nagme.ui.edit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nagme.data.Nag
import com.example.nagme.data.NagDatabase
import com.example.nagme.data.OccurrenceStatus
import com.example.nagme.data.RecurrenceType
import com.example.nagme.data.RoomNagRepository
import com.example.nagme.logic.NagEngine
import com.example.nagme.scheduler.NagScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class EditNagViewModel(
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

    private val _nag =
        MutableStateFlow<Nag?>(null)

    val nag: StateFlow<Nag?> =
        _nag.asStateFlow()

    private val _nextScheduledAt =
        MutableStateFlow<Long?>(null)

    val nextScheduledAt: StateFlow<Long?> =
        _nextScheduledAt.asStateFlow()

    fun loadNag(
        nagId: Long
    ) {

        viewModelScope.launch {

            _nag.value =
                repository.getNag(
                    nagId
                )

            _nextScheduledAt.value =
                repository
                    .getOpenOccurrencesForNag(
                        nagId
                    )
                    .filter {
                        it.status ==
                                OccurrenceStatus.SCHEDULED
                    }
                    .minByOrNull {
                        it.scheduledAt
                    }
                    ?.scheduledAt
        }
    }

    fun saveChanges(
        text: String,
        scheduledAt: Long,
        recurrenceType: RecurrenceType,
        selectedDays: Set<Int>,
        enabled: Boolean,
        timePeriodIntervalMinutes: Int?,
        timePeriodEndMinutesOfDay: Int?,
        nagIntervalMinutes: Int?,
        maxNagCount: Int?,
        onSaved: () -> Unit,
        onError: (String) -> Unit
    ) {

        val existingNag =
            _nag.value
                ?: return

        if (
            text.isBlank()
        ) {
            onError(
                "Enter something to nag you about."
            )
            return
        }

        if (
            recurrenceType ==
            RecurrenceType.ONCE &&
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
                timePeriodIntervalMinutes < 1
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
                Calendar
                    .getInstance()
                    .apply {
                        timeInMillis =
                            scheduledAt
                    }

            val startMinutesOfDay =
                startCalendar.get(
                    Calendar.HOUR_OF_DAY
                ) * 60 +
                        startCalendar.get(
                            Calendar.MINUTE
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
            nagIntervalMinutes < 1
        ) {
            onError(
                "Nag interval must be at least 1 minute."
            )
            return
        }

        if (
            maxNagCount != null &&
            maxNagCount < 1
        ) {
            onError(
                "Maximum nags must be at least 1."
            )
            return
        }

        viewModelScope.launch {

            try {

                repository
                    .deleteScheduledOccurrencesForNag(
                        existingNag.id
                    )

                val updatedNag =
                    existingNag.copy(
                        text =
                            text.trim(),

                        startDateTime =
                            scheduledAt,

                        recurrenceType =
                            recurrenceType,

                        selectedDays =
                            selectedDays,

                        enabled =
                            enabled,

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

                        maxNagCount =
                            maxNagCount
                    )

                repository.updateNag(
                    updatedNag
                )

                if (
                    enabled
                ) {

                    engine
                        .createInitialOccurrence(
                            nag = updatedNag,
                            now =
                                System
                                    .currentTimeMillis()
                        )
                }

                scheduler.reschedule()

                _nag.value =
                    updatedNag

                onSaved()

            } catch (
                e: Exception
            ) {

                onError(
                    e.message
                        ?: "Nag could not be updated."
                )
            }
        }
    }

    fun deleteNag(
        onDeleted: () -> Unit,
        onError: (String) -> Unit
    ) {

        val existingNag =
            _nag.value
                ?: return

        viewModelScope.launch {

            try {

                repository
                    .deleteScheduledOccurrencesForNag(
                        existingNag.id
                    )

                repository.deleteNag(
                    existingNag
                )

                scheduler.reschedule()

                onDeleted()

            } catch (
                e: Exception
            ) {

                onError(
                    e.message
                        ?: "Nag could not be deleted."
                )
            }
        }
    }
}
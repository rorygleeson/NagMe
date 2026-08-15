package com.example.nagme.ui.edit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nagme.data.Nag
import com.example.nagme.data.NagDatabase
import com.example.nagme.data.RecurrenceType
import com.example.nagme.data.RoomNagRepository
import com.example.nagme.logic.NagEngine
import com.example.nagme.scheduler.NagScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun loadNag(
        nagId: Long
    ) {

        viewModelScope.launch {

            _nag.value =
                repository.getNag(
                    nagId
                )
        }
    }

    fun saveChanges(
        text: String,
        scheduledAt: Long,
        recurrenceType: RecurrenceType,
        selectedDays: Set<Int>,
        enabled: Boolean,
        onSaved: () -> Unit,
        onError: (String) -> Unit
    ) {

        val existingNag =
            _nag.value
                ?: return

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
                            enabled
                    )

                repository.updateNag(
                    updatedNag
                )

                if (enabled) {

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
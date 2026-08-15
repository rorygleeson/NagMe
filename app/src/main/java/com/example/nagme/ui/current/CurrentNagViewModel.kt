package com.example.nagme.ui.current

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nagme.data.Nag
import com.example.nagme.data.NagDatabase
import com.example.nagme.data.NagOccurrence
import com.example.nagme.data.OccurrenceStatus
import com.example.nagme.data.RoomNagRepository
import com.example.nagme.logic.NagEngine
import com.example.nagme.notification.NagNotificationManager
import com.example.nagme.scheduler.NagScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CurrentNagViewModel(
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

    private val nagNotificationManager =
        NagNotificationManager(
            application
        )

    private val _uiState =
        MutableStateFlow(
            CurrentNagUiState()
        )

    val uiState: StateFlow<CurrentNagUiState> =
        _uiState.asStateFlow()

    fun load(
        nagId: Long
    ) {

        viewModelScope.launch {

            val nag =
                repository.getNag(nagId)

            val occurrence =
                repository
                    .getOpenOccurrencesForNag(nagId)
                    .firstOrNull {
                        it.status ==
                                OccurrenceStatus.ACTIVE
                    }

            _uiState.value =
                CurrentNagUiState(
                    nag = nag,
                    occurrence = occurrence,
                    loading = false
                )
        }
    }

    fun updateMaxNagCount(
        newMaxNagCount: Int,
        onStopped: () -> Unit
    ) {

        val nag =
            _uiState.value.nag
                ?: return

        val occurrence =
            _uiState.value.occurrence
                ?: return

        val currentMax =
            nag.maxNagCount
                ?: return

        val minimum =
            maxOf(
                1,
                occurrence.nagCount
            )

        val resolvedMax =
            newMaxNagCount
                .coerceIn(
                    minimum,
                    99
                )

        if (
            resolvedMax ==
            currentMax
        ) {
            return
        }

        viewModelScope.launch {

            val updatedNag =
                nag.copy(
                    maxNagCount =
                        resolvedMax
                )

            repository.updateNag(
                updatedNag
            )

            /*
             * If the user reduces the maximum to the number
             * already spoken, the current occurrence has
             * reached its new limit and must stop immediately.
             */
            if (
                occurrence.nagCount >=
                resolvedMax
            ) {

                val stoppedOccurrence =
                    occurrence.copy(
                        status =
                            OccurrenceStatus.EXPIRED,
                        nextNagAt = null,
                        snoozedUntil = null
                    )

                repository.updateOccurrence(
                    stoppedOccurrence
                )

                nagNotificationManager.cancel(
                    nag.id
                )

                engine.createNextRecurringOccurrence(
                    nag = updatedNag,
                    completedOccurrence =
                        stoppedOccurrence,
                    referenceTime =
                        System.currentTimeMillis()
                )

                scheduler.reschedule()

                _uiState.value =
                    _uiState.value.copy(
                        nag = updatedNag,
                        occurrence = null
                    )

                onStopped()

                return@launch
            }

            _uiState.value =
                _uiState.value.copy(
                    nag = updatedNag
                )

            scheduler.reschedule()
        }
    }

    fun markDone(
        onDone: () -> Unit
    ) {

        val nag =
            _uiState.value.nag
                ?: return

        val occurrence =
            _uiState.value.occurrence
                ?: return

        viewModelScope.launch {

            engine.completeOccurrence(
                nag = nag,
                occurrence = occurrence,
                completedAt =
                    System.currentTimeMillis()
            )

            nagNotificationManager.cancel(
                nag.id
            )

            scheduler.reschedule()

            onDone()
        }
    }

    fun snooze(
        minutes: Int = 10,
        onSnoozed: () -> Unit
    ) {

        val nag =
            _uiState.value.nag
                ?: return

        val occurrence =
            _uiState.value.occurrence
                ?: return

        viewModelScope.launch {

            engine.snoozeOccurrence(
                occurrence = occurrence,
                snoozeMinutes = minutes,
                now = System.currentTimeMillis()
            )

            nagNotificationManager.cancel(
                nag.id
            )

            scheduler.reschedule()

            onSnoozed()
        }
    }
}

data class CurrentNagUiState(
    val nag: Nag? = null,
    val occurrence: NagOccurrence? = null,
    val loading: Boolean = true
)
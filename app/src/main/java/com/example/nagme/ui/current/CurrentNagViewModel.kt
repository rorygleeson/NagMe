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

    fun updateNagIntervalMinutes(
        newIntervalMinutes: Int,
        onStopped: () -> Unit
    ) {

        val nag =
            _uiState.value.nag
                ?: return

        val occurrence =
            _uiState.value.occurrence
                ?: return

        val currentInterval =
            nag.nagIntervalMinutes
                ?: return

        val resolvedInterval =
            newIntervalMinutes
                .coerceIn(
                    1,
                    1440
                )

        if (
            resolvedInterval ==
            currentInterval
        ) {
            return
        }

        viewModelScope.launch {

            val now =
                System.currentTimeMillis()

            val updatedNag =
                nag.copy(
                    nagIntervalMinutes =
                        resolvedInterval
                )

            repository.updateNag(
                updatedNag
            )

            if (
                occurrence.snoozedUntil != null &&
                occurrence.snoozedUntil > now
            ) {

                _uiState.value =
                    _uiState.value.copy(
                        nag = updatedNag
                    )

                scheduler.reschedule()

                return@launch
            }

            val proposedNextNag =
                now +
                        resolvedInterval *
                        60_000L

            if (
                occurrence.expiresAt != null &&
                proposedNextNag >
                occurrence.expiresAt
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
                        now
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

            val updatedOccurrence =
                occurrence.copy(
                    nextNagAt =
                        proposedNextNag,
                    snoozedUntil = null
                )

            repository.updateOccurrence(
                updatedOccurrence
            )

            _uiState.value =
                _uiState.value.copy(
                    nag = updatedNag,
                    occurrence =
                        updatedOccurrence
                )

            scheduler.reschedule()
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

    fun updateAiSettings(
        aiEnabled: Boolean,
        personality: String
    ) {

        val nag =
            _uiState.value.nag
                ?: return

        viewModelScope.launch {

            val updatedNag =
                nag.copy(
                    aiEnabled =
                        aiEnabled,
                    personality =
                        personality
                )

            repository.updateNag(
                updatedNag
            )

            _uiState.value =
                _uiState.value.copy(
                    nag = updatedNag
                )

            /*
             * Do not alter the current active occurrence.
             *
             * The next scheduled spoken Nag will re-read the Nag
             * from Room and use these updated AI settings.
             */
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
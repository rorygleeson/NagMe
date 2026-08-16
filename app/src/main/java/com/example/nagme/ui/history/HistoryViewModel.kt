package com.example.nagme.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nagme.data.Nag
import com.example.nagme.data.NagDatabase
import com.example.nagme.data.NagOccurrence
import com.example.nagme.data.OccurrenceStatus
import com.example.nagme.data.RoomNagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        RoomNagRepository(
            NagDatabase.getInstance(application)
        )

    val history =
        combine(
            repository.observeAllNags(),
            repository.observeHistory()
        ) { nags, occurrences ->

            val nagsById =
                nags.associateBy { it.id }

            occurrences
                .filter { occurrence ->

                    when (occurrence.status) {

                        OccurrenceStatus.ACTIVE ->
                            occurrence.nagCount > 0

                        OccurrenceStatus.DONE,
                        OccurrenceStatus.EXPIRED,
                        OccurrenceStatus.MISSED ->
                            true

                        OccurrenceStatus.SCHEDULED ->
                            false
                    }
                }
                .sortedByDescending { occurrence ->

                    listOfNotNull(
                        occurrence.lastNagAt,
                        occurrence.completedAt,
                        occurrence.startedAt,
                        occurrence.scheduledAt
                    ).maxOrNull()
                        ?: occurrence.scheduledAt
                }
                .mapNotNull { occurrence ->

                    nagsById[occurrence.nagId]?.let { nag ->

                        HistoryItem(
                            nag = nag,
                            occurrence = occurrence
                        )
                    }
                }
        }
            .stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted.WhileSubscribed(
                        5_000
                    ),
                initialValue =
                    emptyList<HistoryItem>()
            )

    fun clearHistory() {

        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}

data class HistoryItem(
    val nag: Nag,
    val occurrence: NagOccurrence
)
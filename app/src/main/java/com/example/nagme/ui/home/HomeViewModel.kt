package com.example.nagme.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nagme.data.Nag
import com.example.nagme.data.NagDatabase
import com.example.nagme.data.NagOccurrence
import com.example.nagme.data.RoomNagRepository
import com.example.nagme.logic.NagEngine
import com.example.nagme.notification.NagNotificationManager
import com.example.nagme.scheduler.NagScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
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

    val uiState =
        combine(
            repository.observeAllNags(),
            repository.observeActiveOccurrences(),
            repository.observeScheduledOccurrences()
        ) { nags, activeOccurrences, scheduledOccurrences ->

            val nagsById =
                nags.associateBy { it.id }

            val activeNagIds =
                activeOccurrences
                    .map { it.nagId }
                    .toSet()

            val scheduledNagIds =
                scheduledOccurrences
                    .map { it.nagId }
                    .toSet()

            HomeUiState(
                active =
                    activeOccurrences.mapNotNull { occurrence ->

                        nagsById[occurrence.nagId]?.let { nag ->

                            HomeNagItem(
                                nag = nag,
                                occurrence = occurrence
                            )
                        }
                    },

                upcoming =
                    scheduledOccurrences.mapNotNull { occurrence ->

                        nagsById[occurrence.nagId]?.let { nag ->

                            HomeNagItem(
                                nag = nag,
                                occurrence = occurrence
                            )
                        }
                    },

                paused =
                    nags.filter { nag ->

                        !nag.enabled &&
                                nag.id !in activeNagIds &&
                                nag.id !in scheduledNagIds
                    }
            )
        }
            .stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted.WhileSubscribed(
                        5_000
                    ),
                initialValue =
                    HomeUiState()
            )

    fun markDone(
        item: HomeNagItem
    ) {

        viewModelScope.launch {

            engine.completeOccurrence(
                nag = item.nag,
                occurrence = item.occurrence,
                completedAt =
                    System.currentTimeMillis()
            )

            nagNotificationManager.cancel(
                item.nag.id
            )

            scheduler.reschedule()
        }
    }
}

data class HomeUiState(
    val active: List<HomeNagItem> = emptyList(),
    val upcoming: List<HomeNagItem> = emptyList(),
    val paused: List<Nag> = emptyList()
)

data class HomeNagItem(
    val nag: Nag,
    val occurrence: NagOccurrence
)
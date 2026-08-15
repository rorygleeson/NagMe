package com.example.nagme

import com.example.nagme.data.Nag
import com.example.nagme.data.NagOccurrence
import com.example.nagme.data.NagRepository
import com.example.nagme.data.OccurrenceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNagRepository : NagRepository {

    private val nags = mutableListOf<Nag>()
    private val occurrences = mutableListOf<NagOccurrence>()

    private val nagsFlow = MutableStateFlow<List<Nag>>(emptyList())
    private val activeFlow = MutableStateFlow<List<NagOccurrence>>(emptyList())
    private val historyFlow = MutableStateFlow<List<NagOccurrence>>(emptyList())

    private var nextNagId = 1L
    private var nextOccurrenceId = 1L

    override fun observeAllNags(): Flow<List<Nag>> = nagsFlow

    override fun observeNag(nagId: Long): Flow<Nag?> {
        return MutableStateFlow(
            nags.firstOrNull { it.id == nagId }
        )
    }

    override suspend fun getNag(nagId: Long): Nag? =
        nags.firstOrNull { it.id == nagId }

    override suspend fun createNag(nag: Nag): Long {
        val id = if (nag.id == 0L) nextNagId++ else nag.id

        nags.add(nag.copy(id = id))
        refreshFlows()

        return id
    }

    override suspend fun updateNag(nag: Nag) {
        val index = nags.indexOfFirst { it.id == nag.id }

        if (index >= 0) {
            nags[index] = nag
            refreshFlows()
        }
    }

    override suspend fun deleteNag(nag: Nag) {
        nags.removeAll { it.id == nag.id }
        refreshFlows()
    }

    override suspend fun setNagEnabled(
        nagId: Long,
        enabled: Boolean
    ) {
        val index = nags.indexOfFirst { it.id == nagId }

        if (index >= 0) {
            nags[index] = nags[index].copy(
                enabled = enabled
            )
            refreshFlows()
        }
    }

    override fun observeActiveOccurrences(): Flow<List<NagOccurrence>> =
        activeFlow

    override fun observeHistory(): Flow<List<NagOccurrence>> =
        historyFlow

    override suspend fun createOccurrence(
        occurrence: NagOccurrence
    ): Long {
        val id =
            if (occurrence.id == 0L) nextOccurrenceId++
            else occurrence.id

        occurrences.add(
            occurrence.copy(id = id)
        )

        refreshFlows()

        return id
    }

    override suspend fun getOccurrence(
        occurrenceId: Long
    ): NagOccurrence? =
        occurrences.firstOrNull {
            it.id == occurrenceId
        }

    override suspend fun updateOccurrence(
        occurrence: NagOccurrence
    ) {
        val index = occurrences.indexOfFirst {
            it.id == occurrence.id
        }

        if (index >= 0) {
            occurrences[index] = occurrence
            refreshFlows()
        }
    }

    override suspend fun getNextDueOccurrence(): NagOccurrence? =
        occurrences
            .filter {
                it.status == OccurrenceStatus.SCHEDULED ||
                        it.status == OccurrenceStatus.ACTIVE
            }
            .filter { it.nextNagAt != null }
            .minByOrNull { it.nextNagAt!! }

    override suspend fun getAllDueOccurrences(
        now: Long
    ): List<NagOccurrence> =
        occurrences
            .filter {
                it.status == OccurrenceStatus.SCHEDULED ||
                        it.status == OccurrenceStatus.ACTIVE
            }
            .filter {
                it.nextNagAt != null &&
                        it.nextNagAt!! <= now
            }
            .sortedBy { it.nextNagAt }

    override suspend fun getOpenOccurrencesForNag(
        nagId: Long
    ): List<NagOccurrence> =
        occurrences.filter {
            it.nagId == nagId &&
                    (
                            it.status == OccurrenceStatus.SCHEDULED ||
                                    it.status == OccurrenceStatus.ACTIVE
                            )
        }

    override suspend fun clearHistory() {
        occurrences.removeAll {
            it.status == OccurrenceStatus.DONE ||
                    it.status == OccurrenceStatus.EXPIRED ||
                    it.status == OccurrenceStatus.MISSED
        }

        refreshFlows()
    }

    fun allOccurrences(): List<NagOccurrence> =
        occurrences.toList()

    private fun refreshFlows() {
        nagsFlow.value = nags.toList()

        activeFlow.value = occurrences.filter {
            it.status == OccurrenceStatus.ACTIVE
        }

        historyFlow.value =
            occurrences.sortedByDescending {
                it.scheduledAt
            }
    }
}

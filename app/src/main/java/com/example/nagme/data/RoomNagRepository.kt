package com.example.nagme.data

import kotlinx.coroutines.flow.Flow

class RoomNagRepository(
    private val database: NagDatabase
) : NagRepository {

    private val nagDao = database.nagDao()
    private val occurrenceDao = database.nagOccurrenceDao()

    // ----- Nags -----

    override fun observeAllNags(): Flow<List<Nag>> =
        nagDao.observeAll()

    override fun observeNag(nagId: Long): Flow<Nag?> =
        nagDao.observeById(nagId)

    override suspend fun getNag(nagId: Long): Nag? =
        nagDao.getById(nagId)

    override suspend fun createNag(nag: Nag): Long =
        nagDao.insert(nag)

    override suspend fun updateNag(nag: Nag) {
        nagDao.update(
            nag.copy(
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteNag(nag: Nag) {
        nagDao.delete(nag)
    }

    override suspend fun setNagEnabled(
        nagId: Long,
        enabled: Boolean
    ) {
        nagDao.setEnabled(
            nagId = nagId,
            enabled = enabled
        )
    }

    // ----- Occurrences -----

    override fun observeActiveOccurrences(): Flow<List<NagOccurrence>> =
        occurrenceDao.observeActive()

    override fun observeScheduledOccurrences(): Flow<List<NagOccurrence>> =
        occurrenceDao.observeScheduled()

    override fun observeHistory(): Flow<List<NagOccurrence>> =
        occurrenceDao.observeHistory()

    override suspend fun createOccurrence(
        occurrence: NagOccurrence
    ): Long =
        occurrenceDao.insert(occurrence)

    override suspend fun getOccurrence(
        occurrenceId: Long
    ): NagOccurrence? =
        occurrenceDao.getById(occurrenceId)

    override suspend fun updateOccurrence(
        occurrence: NagOccurrence
    ) {
        occurrenceDao.update(occurrence)
    }

    override suspend fun getNextDueOccurrence(): NagOccurrence? =
        occurrenceDao.getNextDue()

    override suspend fun getAllDueOccurrences(
        now: Long
    ): List<NagOccurrence> =
        occurrenceDao.getAllDue(now)

    override suspend fun getOpenOccurrencesForNag(
        nagId: Long
    ): List<NagOccurrence> =
        occurrenceDao.getOpenForNag(nagId)

    override suspend fun deleteScheduledOccurrencesForNag(
        nagId: Long
    ) {
        occurrenceDao.deleteScheduledForNag(
            nagId
        )
    }
    override suspend fun clearHistory() {
        occurrenceDao.clearHistory()
    }
}
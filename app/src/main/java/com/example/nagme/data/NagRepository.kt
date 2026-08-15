package com.example.nagme.data

import kotlinx.coroutines.flow.Flow

interface NagRepository {

    // ----- Nags -----

    fun observeAllNags(): Flow<List<Nag>>

    fun observeNag(nagId: Long): Flow<Nag?>

    suspend fun getNag(nagId: Long): Nag?

    suspend fun createNag(nag: Nag): Long

    suspend fun updateNag(nag: Nag)

    suspend fun deleteNag(nag: Nag)

    suspend fun setNagEnabled(
        nagId: Long,
        enabled: Boolean
    )

    // ----- Occurrences -----

    fun observeActiveOccurrences(): Flow<List<NagOccurrence>>

    fun observeHistory(): Flow<List<NagOccurrence>>

    suspend fun createOccurrence(
        occurrence: NagOccurrence
    ): Long

    suspend fun getOccurrence(
        occurrenceId: Long
    ): NagOccurrence?

    suspend fun updateOccurrence(
        occurrence: NagOccurrence
    )

    suspend fun getNextDueOccurrence(): NagOccurrence?

    suspend fun getAllDueOccurrences(
        now: Long
    ): List<NagOccurrence>

    suspend fun getOpenOccurrencesForNag(
        nagId: Long
    ): List<NagOccurrence>

    suspend fun deleteScheduledOccurrencesForNag(
        nagId: Long
    )

    suspend fun clearHistory()

    fun observeScheduledOccurrences(): Flow<List<NagOccurrence>>


}
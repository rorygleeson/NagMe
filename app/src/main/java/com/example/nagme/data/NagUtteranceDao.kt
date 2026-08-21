package com.example.nagme.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NagUtteranceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(utterance: NagUtterance): Long

    @Query(
        """
        SELECT * FROM nag_utterances
        ORDER BY occurrenceId ASC, nagNumber ASC
        """
    )
    fun observeAll(): Flow<List<NagUtterance>>

    @Query(
        """
        SELECT * FROM nag_utterances
        WHERE occurrenceId = :occurrenceId
        ORDER BY nagNumber ASC
        """
    )
    suspend fun getForOccurrence(
        occurrenceId: Long
    ): List<NagUtterance>

    @Query(
        """
        DELETE FROM nag_utterances
        WHERE occurrenceId = :occurrenceId
        """
    )
    suspend fun deleteForOccurrence(
        occurrenceId: Long
    )

    @Query(
        """
        DELETE FROM nag_utterances
        WHERE occurrenceId IN (
            SELECT id FROM nag_occurrences
            WHERE status IN ('DONE', 'EXPIRED', 'MISSED')
        )
        """
    )
    suspend fun clearCompletedHistory()
}
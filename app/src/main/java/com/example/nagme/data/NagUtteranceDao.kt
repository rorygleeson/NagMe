package com.example.nagme.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NagUtteranceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(utterance: NagUtterance): Long

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
}

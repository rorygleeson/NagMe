package com.example.nagme.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NagOccurrenceDao {

    /*
     * Raw Room insert.
     *
     * Do not call this directly outside this DAO.
     * New occurrences should go through insert(),
     * which protects against duplicate open occurrences.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRaw(
        occurrence: NagOccurrence
    ): Long

    /*
     * DATABASE-LEVEL DUPLICATE PROTECTION
     *
     * A Nag must never have more than one open occurrence
     * (SCHEDULED or ACTIVE).
     *
     * Keeping the check and insert in one Room transaction
     * prevents two competing lifecycle paths from both
     * deliberately creating another open occurrence.
     *
     * Existing repository code can continue calling insert().
     */
    @Transaction
    suspend fun insert(
        occurrence: NagOccurrence
    ): Long {

        if (
            occurrence.status ==
            OccurrenceStatus.SCHEDULED ||
            occurrence.status ==
            OccurrenceStatus.ACTIVE
        ) {

            val existingOpen =
                getOpenForNag(
                    occurrence.nagId
                ).firstOrNull()

            if (existingOpen != null) {
                return existingOpen.id
            }
        }

        return insertRaw(
            occurrence
        )
    }

    @Update
    suspend fun update(
        occurrence: NagOccurrence
    )

    @Query(
        """
        SELECT * FROM nag_occurrences
        WHERE id = :occurrenceId
        LIMIT 1
        """
    )
    suspend fun getById(
        occurrenceId: Long
    ): NagOccurrence?

    @Query(
        """
        SELECT * FROM nag_occurrences
        WHERE nagId = :nagId
        ORDER BY scheduledAt DESC
        """
    )
    fun observeForNag(
        nagId: Long
    ): Flow<List<NagOccurrence>>

    @Query(
        """
        SELECT * FROM nag_occurrences
        ORDER BY scheduledAt DESC
        """
    )
    fun observeHistory():
            Flow<List<NagOccurrence>>

    @Query(
        """
        SELECT * FROM nag_occurrences
        WHERE status = 'ACTIVE'
        ORDER BY nextNagAt ASC
        """
    )
    fun observeActive():
            Flow<List<NagOccurrence>>

    @Query(
        """
        SELECT * FROM nag_occurrences
        WHERE status IN ('SCHEDULED', 'ACTIVE')
          AND nextNagAt IS NOT NULL
        ORDER BY nextNagAt ASC
        LIMIT 1
        """
    )
    suspend fun getNextDue():
            NagOccurrence?

    @Query(
        """
        SELECT * FROM nag_occurrences
        WHERE status IN ('SCHEDULED', 'ACTIVE')
          AND nextNagAt IS NOT NULL
          AND nextNagAt <= :now
        ORDER BY nextNagAt ASC
        """
    )
    suspend fun getAllDue(
        now: Long
    ): List<NagOccurrence>

    @Query(
        """
        SELECT * FROM nag_occurrences
        WHERE nagId = :nagId
          AND status IN ('SCHEDULED', 'ACTIVE')
        ORDER BY scheduledAt ASC
        """
    )
    suspend fun getOpenForNag(
        nagId: Long
    ): List<NagOccurrence>

    @Query(
        """
        DELETE FROM nag_occurrences
        WHERE status IN ('DONE', 'EXPIRED', 'MISSED')
        """
    )
    suspend fun clearHistory()

    @Query(
        """
        SELECT * FROM nag_occurrences
        WHERE status = 'SCHEDULED'
          AND nextNagAt IS NOT NULL
        ORDER BY nextNagAt ASC
        """
    )
    fun observeScheduled():
            Flow<List<NagOccurrence>>

    @Query(
        """
        DELETE FROM nag_occurrences
        WHERE nagId = :nagId
          AND status = 'SCHEDULED'
        """
    )
    suspend fun deleteScheduledForNag(
        nagId: Long
    )
}
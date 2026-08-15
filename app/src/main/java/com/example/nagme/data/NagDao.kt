package com.example.nagme.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NagDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(nag: Nag): Long

    @Update
    suspend fun update(nag: Nag)

    @Delete
    suspend fun delete(nag: Nag)

    @Query("SELECT * FROM nags WHERE id = :nagId LIMIT 1")
    suspend fun getById(nagId: Long): Nag?

    @Query("SELECT * FROM nags WHERE id = :nagId LIMIT 1")
    fun observeById(nagId: Long): Flow<Nag?>

    @Query("SELECT * FROM nags ORDER BY startDateTime ASC")
    fun observeAll(): Flow<List<Nag>>

    @Query(
        """
        SELECT * FROM nags
        WHERE enabled = 1
        ORDER BY startDateTime ASC
        """
    )
    suspend fun getEnabled(): List<Nag>

    @Query("UPDATE nags SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :nagId")
    suspend fun setEnabled(
        nagId: Long,
        enabled: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )
}

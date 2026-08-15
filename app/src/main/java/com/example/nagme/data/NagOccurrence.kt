package com.example.nagme.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nag_occurrences",
    indices = [
        Index(value = ["nagId"]),
        Index(value = ["nextNagAt"]),
        Index(value = ["status"])
    ]
)
data class NagOccurrence(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Historical reference to the Nag that created this occurrence.
    // Deliberately NOT a Room foreign key because deleting a Nag
    // must retain its occurrence history.
    val nagId: Long,

    val scheduledAt: Long,

    val startedAt: Long? = null,

    val status: OccurrenceStatus = OccurrenceStatus.SCHEDULED,

    val nagCount: Int = 0,

    val lastNagAt: Long? = null,

    val nextNagAt: Long? = null,

    val snoozedUntil: Long? = null,

    val expiresAt: Long? = null,

    val completedAt: Long? = null,

    val createdAt: Long = System.currentTimeMillis()
)

enum class OccurrenceStatus {
    SCHEDULED,
    ACTIVE,
    DONE,
    EXPIRED,
    MISSED
}
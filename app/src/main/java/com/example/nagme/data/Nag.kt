package com.example.nagme.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nags")
data class Nag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val text: String,

    val enabled: Boolean = true,

    // The actual date/time of the first occurrence.
    // A relative input such as "in 20 minutes" is converted
    // to an absolute timestamp before the Nag is saved.
    val startDateTime: Long,

    val recurrenceType: RecurrenceType = RecurrenceType.ONCE,

    // Only used when recurrenceType is SELECTED_DAYS.
    // 1 = Sunday ... 7 = Saturday.
    val selectedDays: Set<Int> = emptySet(),

    // null = speak only once for this occurrence.
    val nagIntervalMinutes: Int? = 10,

    val stopWhenDone: Boolean = true,

    val maxNagCount: Int? = null,

    val maxNagDurationMinutes: Int? = null,

    // null = use the global default voice.
    val voiceId: String? = null,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)

enum class RecurrenceType {
    ONCE,
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    WEEKLY,
    SELECTED_DAYS
}
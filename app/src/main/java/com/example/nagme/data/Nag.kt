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

    // Used by SELECTED_DAYS and DURING_TIME_PERIOD.
    // 1 = Sunday ... 7 = Saturday.
    val selectedDays: Set<Int> = emptySet(),

    // Only used when recurrenceType is DURING_TIME_PERIOD.
    // Example: 60 = create a new occurrence every 60 minutes
    // within the configured time period.
    val timePeriodIntervalMinutes: Int? = null,

    // Only used when recurrenceType is DURING_TIME_PERIOD.
    // Stored as minutes since midnight.
    // Example: 17:00 = 1020.
    val timePeriodEndMinutesOfDay: Int? = null,

    // null = speak only once for this occurrence.
    val nagIntervalMinutes: Int? = 10,

    val stopWhenDone: Boolean = true,

    val maxNagCount: Int? = null,

    val maxNagDurationMinutes: Int? = null,

    // null = use the global default voice.
    val voiceId: String? = null,

    // AI-generated nag wording is opt-in for each individual Nag.
    // false = use the original NagMe wording and do not use AI.
    val aiEnabled: Boolean = false,

    // Stable personality ID sent to the AI backend.
    // This is only used when aiEnabled is true.
    val personality: String = "irish_mammy",

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)

enum class RecurrenceType {
    ONCE,
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    WEEKLY,
    SELECTED_DAYS,
    DURING_TIME_PERIOD
}
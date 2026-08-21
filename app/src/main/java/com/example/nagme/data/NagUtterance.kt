package com.example.nagme.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nag_utterances",
    indices = [
        Index(value = ["occurrenceId"])
    ]
)
data class NagUtterance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // The occurrence during which this text was spoken.
    val occurrenceId: Long,

    // Which nag within the occurrence this was: 1, 2, 3...
    val nagNumber: Int,

    // The exact text that NagMe actually spoke.
    val spokenText: String,

    // Where the spoken text came from.
    val source: UtteranceSource,

    // When this text was spoken.
    val spokenAt: Long = System.currentTimeMillis()
)

enum class UtteranceSource {
    ORIGINAL,
    AI,
    FALLBACK
}

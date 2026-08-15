package com.example.nagme.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun recurrenceTypeToString(value: RecurrenceType): String =
        value.name

    @TypeConverter
    fun stringToRecurrenceType(value: String): RecurrenceType =
        RecurrenceType.valueOf(value)

    @TypeConverter
    fun occurrenceStatusToString(value: OccurrenceStatus): String =
        value.name

    @TypeConverter
    fun stringToOccurrenceStatus(value: String): OccurrenceStatus =
        OccurrenceStatus.valueOf(value)

    @TypeConverter
    fun intSetToString(value: Set<Int>): String =
        value.sorted().joinToString(",")

    @TypeConverter
    fun stringToIntSet(value: String): Set<Int> {
        if (value.isBlank()) return emptySet()

        return value
            .split(",")
            .mapNotNull { it.toIntOrNull() }
            .toSet()
    }
}
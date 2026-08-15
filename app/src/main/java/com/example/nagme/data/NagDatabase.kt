package com.example.nagme.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Nag::class,
        NagOccurrence::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NagDatabase : RoomDatabase() {

    abstract fun nagDao(): NagDao

    abstract fun nagOccurrenceDao(): NagOccurrenceDao

    companion object {

        @Volatile
        private var INSTANCE: NagDatabase? = null

        fun getInstance(context: Context): NagDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NagDatabase::class.java,
                    "nagme.db"
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
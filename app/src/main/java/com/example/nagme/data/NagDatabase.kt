package com.example.nagme.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Nag::class,
        NagOccurrence::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NagDatabase : RoomDatabase() {

    abstract fun nagDao(): NagDao

    abstract fun nagOccurrenceDao(): NagOccurrenceDao

    companion object {

        @Volatile
        private var INSTANCE: NagDatabase? = null

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {

                    db.execSQL(
                        """
                        ALTER TABLE nags
                        ADD COLUMN timePeriodIntervalMinutes INTEGER
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        ALTER TABLE nags
                        ADD COLUMN timePeriodEndMinutesOfDay INTEGER
                        """.trimIndent()
                    )
                }
            }

        fun getInstance(
            context: Context
        ): NagDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    INSTANCE
                        ?: Room.databaseBuilder(
                            context.applicationContext,
                            NagDatabase::class.java,
                            "nagme.db"
                        )
                            .addMigrations(
                                MIGRATION_1_2
                            )
                            .build()
                            .also {
                                INSTANCE = it
                            }
                }
        }
    }
}
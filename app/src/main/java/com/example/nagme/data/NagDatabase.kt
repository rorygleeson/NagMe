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
        NagOccurrence::class,
        NagUtterance::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NagDatabase : RoomDatabase() {

    abstract fun nagDao(): NagDao

    abstract fun nagOccurrenceDao(): NagOccurrenceDao

    abstract fun nagUtteranceDao(): NagUtteranceDao

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

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS nag_utterances (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            occurrenceId INTEGER NOT NULL,
                            nagNumber INTEGER NOT NULL,
                            spokenText TEXT NOT NULL,
                            source TEXT NOT NULL,
                            spokenAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS index_nag_utterances_occurrenceId
                        ON nag_utterances (occurrenceId)
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
                                MIGRATION_1_2,
                                MIGRATION_2_3
                            )
                            .build()
                            .also {
                                INSTANCE = it
                            }
                }
        }
    }
}
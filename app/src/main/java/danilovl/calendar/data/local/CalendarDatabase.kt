package danilovl.calendar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import danilovl.calendar.data.Converters

/**
 * Schema history is exported to app/schemas (see room.schemaLocation in build.gradle.kts).
 *
 * When changing [CalendarEvent]: bump [version] and add a [Migration] to [ALL_MIGRATIONS].
 * Never rely on destructive fallback — it silently wipes all user events.
 */
@Database(entities = [CalendarEvent::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class CalendarDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: CalendarDatabase? = null

        private val ALL_MIGRATIONS = emptyArray<Migration>()

        fun getDatabase(context: Context): CalendarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CalendarDatabase::class.java,
                    "calendar_database"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    // Version 1 was a pre-release schema that was never exported; only it may be dropped.
                    .fallbackToDestructiveMigrationFrom(1)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

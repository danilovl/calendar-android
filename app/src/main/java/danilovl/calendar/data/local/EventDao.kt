package danilovl.calendar.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

@Dao
abstract class EventDao {
    @Query("SELECT * FROM events WHERE date = :date ORDER BY startTime ASC")
    abstract fun getEventsForDate(date: LocalDate): Flow<List<CalendarEvent>>

    /** Events that start in the range or are multi-day events overlapping it. */
    @Query(
        "SELECT * FROM events WHERE date <= :endDate AND COALESCE(endDate, date) >= :startDate " +
            "ORDER BY date ASC, startTime ASC"
    )
    abstract fun getEventsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<CalendarEvent>>

    @Query("SELECT DISTINCT date FROM events")
    abstract fun getDatesWithEvents(): Flow<List<LocalDate>>

    @Query("SELECT * FROM events")
    abstract fun getAllEvents(): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM events ORDER BY date ASC, startTime ASC")
    abstract suspend fun getAllEventsList(): List<CalendarEvent>

    @Query("SELECT * FROM events WHERE repeat != 'none'")
    abstract suspend fun getRecurringEventsList(): List<CalendarEvent>

    @Query("SELECT * FROM events WHERE reminder = 1")
    abstract suspend fun getEventsWithReminder(): List<CalendarEvent>

    @Query("SELECT * FROM events WHERE id = :id")
    abstract suspend fun getEventById(id: Int): CalendarEvent?

    @Query(
        "SELECT COUNT(*) FROM events WHERE title = :title AND date = :date " +
            "AND startTime IS :startTime AND repeat = :repeat"
    )
    abstract suspend fun countSame(title: String, date: LocalDate, startTime: LocalTime?, repeat: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertEvent(event: CalendarEvent): Long

    @Update
    abstract suspend fun updateEvent(event: CalendarEvent)

    @Delete
    abstract suspend fun deleteEvent(event: CalendarEvent)

    /** Atomically replaces the series row (e.g. with a new exclusion) and inserts a detached instance. */
    @Transaction
    open suspend fun updateAndInsert(updated: CalendarEvent, inserted: CalendarEvent): Long {
        updateEvent(updated)
        return insertEvent(inserted.copy(id = 0))
    }

    /** Inserts events that are not already present; returns the inserted rows with their new ids. */
    @Transaction
    open suspend fun insertIfMissing(events: List<CalendarEvent>): List<CalendarEvent> {
        val inserted = mutableListOf<CalendarEvent>()
        for (event in events) {
            if (countSame(event.title, event.date, event.startTime, event.repeat) == 0) {
                val id = insertEvent(event.copy(id = 0))
                inserted.add(event.copy(id = id.toInt()))
            }
        }
        return inserted
    }
}

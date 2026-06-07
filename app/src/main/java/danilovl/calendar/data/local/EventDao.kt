package danilovl.calendar.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE date = :date ORDER BY startTime ASC")
    fun getEventsForDate(date: LocalDate): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM events WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, startTime ASC")
    fun getEventsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<CalendarEvent>>

    @Query("SELECT DISTINCT date FROM events")
    fun getDatesWithEvents(): Flow<List<LocalDate>>

    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM events ORDER BY date ASC, startTime ASC")
    suspend fun getAllEventsList(): List<CalendarEvent>

    @Query("SELECT * FROM events WHERE repeat != 'none'")
    suspend fun getRecurringEventsList(): List<CalendarEvent>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Int): CalendarEvent?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertEvent(event: CalendarEvent): Long

    @Delete
    suspend fun deleteEvent(event: CalendarEvent)
}

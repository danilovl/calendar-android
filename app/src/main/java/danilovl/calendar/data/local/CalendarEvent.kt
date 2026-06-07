package danilovl.calendar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val date: LocalDate,
    val endDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val isAllDay: Boolean = false,
    val repeat: String = "none",
    val reminder: Boolean = false,
    val reminderMelody: String = "default",
    val reminderOffset: String = "1_day",
    val timeZone: String = "UTC",
    val eventType: String = "default",
    val color: Int? = null,
    val location: String = "",
    val repeatUntil: LocalDate? = null,
    val excludedDates: String = ""
) : Serializable

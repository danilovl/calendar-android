package danilovl.calendar.domain

import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.local.EventDao
import java.time.LocalDate
import java.time.LocalTime

class AddEventUseCase(private val eventDao: EventDao) {
    suspend operator fun invoke(
        title: String,
        description: String,
        date: LocalDate,
        endDate: LocalDate? = null,
        startTime: LocalTime? = null,
        endTime: LocalTime? = null,
        isAllDay: Boolean = false,
        repeat: String = "none",
        reminder: Boolean = false,
        reminderMelody: String = "default",
        reminderOffset: String = "1_day",
        timeZone: String = "UTC",
        eventType: String = "default",
        color: Int? = null,
        location: String = "",
        repeatUntil: LocalDate? = null
    ): Long {
        val event = CalendarEvent(
            title = title,
            description = description,
            date = date,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            repeat = repeat,
            reminder = reminder,
            reminderMelody = reminderMelody,
            reminderOffset = reminderOffset,
            timeZone = timeZone,
            eventType = eventType,
            color = color,
            location = location,
            repeatUntil = repeatUntil
        )
        return eventDao.insertEvent(event)
    }
}

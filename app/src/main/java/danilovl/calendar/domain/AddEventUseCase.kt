package danilovl.calendar.domain

import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.local.EventDao

class AddEventUseCase(private val eventDao: EventDao) {
    /** Inserts [event] as a new row (any id on it is ignored) and returns the new id. */
    suspend operator fun invoke(event: CalendarEvent): Long = eventDao.insertEvent(event.copy(id = 0))
}

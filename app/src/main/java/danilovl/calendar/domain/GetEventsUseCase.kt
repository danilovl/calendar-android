package danilovl.calendar.domain

import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.local.EventDao
import kotlinx.coroutines.flow.Flow

class GetEventsUseCase(private val eventDao: EventDao) {
    operator fun invoke(): Flow<List<CalendarEvent>> = eventDao.getAllEvents()
}

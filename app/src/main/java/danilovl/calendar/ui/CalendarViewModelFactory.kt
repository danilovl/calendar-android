package danilovl.calendar.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import danilovl.calendar.data.local.EventDao
import danilovl.calendar.data.repository.ContactsRepository
import danilovl.calendar.data.repository.ExternalCalendarRepository
import danilovl.calendar.data.repository.HolidayRepository
import danilovl.calendar.data.repository.SettingsRepository
import danilovl.calendar.domain.AddEventUseCase
import danilovl.calendar.domain.GetEventsUseCase
import danilovl.calendar.domain.GetHolidaysUseCase

class CalendarViewModelFactory(
    private val context: android.content.Context,
    private val eventDao: EventDao,
    private val contactsRepository: ContactsRepository,
    private val holidayRepository: HolidayRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            val getEventsUseCase = GetEventsUseCase(eventDao)
            val addEventUseCase = AddEventUseCase(eventDao)
            val getHolidaysUseCase = GetHolidaysUseCase(holidayRepository)
            val externalCalendarRepository = ExternalCalendarRepository(context)
            
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(
                context.applicationContext as Application,
                eventDao, 
                contactsRepository, 
                holidayRepository, 
                settingsRepository,
                externalCalendarRepository,
                getEventsUseCase,
                addEventUseCase,
                getHolidaysUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

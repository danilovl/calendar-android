package danilovl.calendar.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import danilovl.calendar.data.EventType
import danilovl.calendar.data.local.CalendarDatabase
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.local.EventDao
import danilovl.calendar.data.repository.AppSettings
import danilovl.calendar.data.repository.ContactsRepository
import danilovl.calendar.data.repository.ExternalCalendarRepository
import danilovl.calendar.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val externalCalendarRepository: ExternalCalendarRepository,
    private val contactsRepository: ContactsRepository,
    private val eventDao: EventDao,
    private val application: Application
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings

    fun syncBirthdays() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.update { it.copy(lastBirthdaysSync = System.currentTimeMillis()) }
            }
        }
    }

    suspend fun getAvailableCalendars(): List<danilovl.calendar.data.ExternalCalendar> {
        return try {
            externalCalendarRepository.getCalendars()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.update(transform)
            }
        }
    }

    fun importEvents(events: List<CalendarEvent>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                events.forEach {
                    eventDao.insertEvent(it.copy(id = 0))
                }
            }
        }
    }

    suspend fun getFilteredEventsList(types: Set<String>): List<CalendarEvent> {
        return withContext(Dispatchers.IO) {
            val localEvents = eventDao.getAllEventsList().filter {
                val mappedType = if (it.eventType == "default") EventType.EVENT.value else it.eventType
                mappedType in types
            }
            val settingsVal = settings.value
            if (settingsVal.importContactBirthdays && EventType.BIRTHDAY.value in types) {
                val birthdays = try {
                    contactsRepository.getBirthdays().map {
                        CalendarEvent(
                            id = 0,
                            title = it.name,
                            date = it.date,
                            eventType = EventType.BIRTHDAY.value,
                            isAllDay = true,
                            repeat = "yearly",
                            description = application.getString(danilovl.calendar.R.string.birthday_subtitle)
                        )
                    }
                } catch (e: Exception) {
                    emptyList()
                }
                localEvents + birthdays
            } else {
                localEvents
            }
        }
    }
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    private val repository = SettingsRepository.getInstance(context)
    private val externalCalendarRepository = ExternalCalendarRepository(context)
    private val contactsRepository = ContactsRepository(context)
    private val eventDao = CalendarDatabase.getDatabase(context).eventDao()

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, externalCalendarRepository, contactsRepository, eventDao, context.applicationContext as Application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

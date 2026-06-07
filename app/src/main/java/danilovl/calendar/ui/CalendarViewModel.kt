package danilovl.calendar.ui

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import danilovl.calendar.CalendarWidget
import danilovl.calendar.R
import danilovl.calendar.data.EventType
import danilovl.calendar.data.ReminderScheduler
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.local.EventDao
import danilovl.calendar.data.repository.ContactBirthday
import danilovl.calendar.data.repository.ContactsRepository
import danilovl.calendar.data.repository.ExternalCalendarRepository
import danilovl.calendar.data.repository.Holiday
import danilovl.calendar.data.repository.HolidayRepository
import danilovl.calendar.data.repository.SettingsRepository
import danilovl.calendar.domain.AddEventUseCase
import danilovl.calendar.domain.GetEventsUseCase
import danilovl.calendar.domain.GetHolidaysUseCase
import danilovl.calendar.service.isEventOnDate
import danilovl.calendar.util.AppLog
import danilovl.calendar.util.EventUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

enum class CalendarViewMode {
    YEAR, MONTH, WEEK, DAY
}

enum class MonthViewState {
    WEEK,  
    MONTH, 
    FULL   
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val application: Application,
    private val eventDao: EventDao,
    private val contactsRepository: ContactsRepository,
    private val holidayRepository: HolidayRepository,
    private val settingsRepository: SettingsRepository,
    private val externalCalendarRepository: ExternalCalendarRepository,
    private val getEventsUseCase: GetEventsUseCase,
    private val addEventUseCase: AddEventUseCase,
    private val getHolidaysUseCase: GetHolidaysUseCase
) : ViewModel() {

    private val _currentViewMode = MutableStateFlow(CalendarViewMode.MONTH)
    val currentViewMode: StateFlow<CalendarViewMode> = _currentViewMode.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _highlightedDate = MutableStateFlow(LocalDate.now())
    val highlightedDate: StateFlow<LocalDate> = _highlightedDate.asStateFlow()

    private val _contactBirthdays = MutableStateFlow<List<ContactBirthday>>(emptyList())
    val contactBirthdays: StateFlow<List<ContactBirthday>> = _contactBirthdays.asStateFlow()

    private val _holidays = MutableStateFlow<List<Holiday>>(emptyList())
    val holidays: StateFlow<List<Holiday>> = _holidays.asStateFlow()

    private val _externalEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val externalEvents: StateFlow<List<CalendarEvent>> = _externalEvents.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                _currentMonth
            ) { settings, month ->
                settings to month
            }.collect { (settings, month) ->
                loadHolidays(month.year, listOf(settings.holidayCountry) + settings.additionalHolidayCountries)
                loadExternalEvents(month.year, settings.enabledCalendars)
            }
        }
        viewModelScope.launch {
            val settings = settingsRepository.settings.value
            if (settings.importContactBirthdays) {
                val lastSync = settings.lastBirthdaysSync
                val dayInMillis = 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - lastSync > dayInMillis) {
                    syncContactBirthdays()
                } else {
                    loadContactBirthdays()
                }
            }
        }
    }

    fun loadExternalEvents(year: Int, calendarIds: Set<Long>) {
        viewModelScope.launch {
            if (calendarIds.isEmpty()) {
                _externalEvents.value = emptyList()
                return@launch
            }
            try {
                val start = LocalDate.of(year - 1, 1, 1)
                val end = LocalDate.of(year + 1, 12, 31)
                _externalEvents.value = externalCalendarRepository.getEvents(calendarIds, start, end)
            } catch (e: Exception) {
                AppLog.e("CalendarViewModel", "Operation failed", e)
                _externalEvents.value = emptyList()
            }
        }
    }

    fun loadHolidays(year: Int, countries: List<String>) {
        viewModelScope.launch {
            if (!settingsRepository.settings.value.internationalHolidays) {
                _holidays.value = emptyList()
                return@launch
            }
            _holidays.value = try {
                val years = listOf(year - 1, year, year + 1)
                years.flatMap { y ->
                    countries.flatMap { country ->
                        getHolidaysUseCase(y, country)
                    }
                }.distinctBy { it.date to it.name }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private val localEvents: StateFlow<List<CalendarEvent>> = getEventsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEvents: StateFlow<List<CalendarEvent>> = combine(
        localEvents,
        externalEvents,
        holidays,
        contactBirthdays
    ) { local, external, hols, birthdays ->
        val virtualHols = hols.map {
            CalendarEvent(
                id = -1,
                title = it.name,
                date = it.date,
                eventType = EventType.HOLIDAY.value,
                isAllDay = true,
                description = application.getString(R.string.holiday_subtitle)
            )
        }
        val currentYear = LocalDate.now().year
        val virtualBirthdays = listOf(currentYear - 1, currentYear, currentYear + 1).flatMap { year ->
            birthdays.mapNotNull {
                try {
                    CalendarEvent(
                        id = -2,
                        title = it.name,
                        date = it.date.withYear(year),
                        eventType = EventType.BIRTHDAY.value,
                        isAllDay = true,
                        description = "BIRTHDAY_ORIG_DATE:${it.date}|" + application.getString(R.string.birthday_subtitle)
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
        (local + external + virtualHols + virtualBirthdays)
            .distinctBy { (it.title.lowercase().trim()) to it.date }
            .sortedWith(compareBy({ !it.isAllDay }, { it.startTime }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eventsForSelectedDate: StateFlow<List<CalendarEvent>> = combine(
        _selectedDate,
        allEvents
    ) { date, allEvs ->
        allEvs.filter { isEventOnDate(it, date) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val datesWithEvents: StateFlow<List<LocalDate>> = eventDao.getDatesWithEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yearEventDates: StateFlow<Set<LocalDate>> = combine(allEvents, externalEvents, holidays) { events, extEvents, hols ->
        (events.map { it.date } + extEvents.map { it.date } + hols.map { it.date }).toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val yearBirthdayDays: StateFlow<Set<Pair<Int, Int>>> = _contactBirthdays.map { birthdays ->
        birthdays.map { it.date.monthValue to it.date.dayOfMonth }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setViewMode(mode: CalendarViewMode) {
        _currentViewMode.value = mode
    }

    fun syncContactBirthdays() {
        viewModelScope.launch {
            try {
                _contactBirthdays.value = contactsRepository.getBirthdays()
                settingsRepository.update { it.copy(lastBirthdaysSync = System.currentTimeMillis()) }
            } catch (e: Exception) {
                AppLog.e("CalendarViewModel", "Operation failed", e)
            }
        }
    }

    fun loadContactBirthdays() {
        viewModelScope.launch {
            try {
                _contactBirthdays.value = contactsRepository.getBirthdays()
            } catch (e: Exception) {
                AppLog.e("CalendarViewModel", "Operation failed", e)
            }
        }
    }

    fun clearContactBirthdays() {
        _contactBirthdays.value = emptyList()
    }

    fun selectDate(date: LocalDate, updateHighlighted: Boolean = true) {
        val oldYear = _selectedDate.value.year
        _selectedDate.value = date
        if (updateHighlighted) {
            _highlightedDate.value = date
        }
        if (date.year != oldYear) {
            viewModelScope.launch {
                val settings = settingsRepository.settings.value
                loadHolidays(date.year, listOf(settings.holidayCountry) + settings.additionalHolidayCountries)
                loadContactBirthdays()
            }
        }
        if (YearMonth.from(date) != _currentMonth.value) {
            _currentMonth.value = YearMonth.from(date)
        }
    }

    fun selectYear(year: Int) {
        if (_selectedDate.value.year != year) {
            val newDate = _selectedDate.value.withYear(year)
            selectDate(newDate)
        }
    }

    private val _monthViewState = MutableStateFlow(MonthViewState.MONTH)
    val monthViewState: StateFlow<MonthViewState> = _monthViewState.asStateFlow()

    private val _selectedEvent = MutableStateFlow<CalendarEvent?>(null)
    val selectedEvent: StateFlow<CalendarEvent?> = _selectedEvent.asStateFlow()

    private val _showAddEvent = MutableStateFlow(false)
    val showAddEvent: StateFlow<Boolean> = _showAddEvent.asStateFlow()

    fun showEventDetails(eventId: Int) {
        viewModelScope.launch {
            _selectedEvent.value = eventDao.getEventById(eventId)
            _selectedEvent.value?.let { event ->
                selectDate(event.date)
            }
        }
    }

    fun showVirtualEventDetails(type: String, title: String, date: LocalDate) {
        val virtualEvent = CalendarEvent(
            id = if (type == EventType.HOLIDAY.value) -1 else -2,
            title = title,
            date = date,
            eventType = type,
            isAllDay = true,
            description = if (type == EventType.HOLIDAY.value) application.getString(R.string.holiday_subtitle) else application.getString(R.string.birthday_subtitle)
        )
        _selectedEvent.value = virtualEvent
        selectDate(date)
    }

    fun selectEvent(event: CalendarEvent?) {
        _selectedEvent.value = event
    }

    fun requestAddEvent() {
        _showAddEvent.value = true
    }

    fun dismissAddEvent() {
        _showAddEvent.value = false
    }

    fun clearSelectedEvent() {
        _selectedEvent.value = null
    }

    fun setMonthViewState(state: MonthViewState) {
        _monthViewState.value = state
    }

    fun cycleMonthViewState() {
        _monthViewState.value = when (_monthViewState.value) {
            MonthViewState.WEEK -> MonthViewState.MONTH
            MonthViewState.MONTH -> MonthViewState.FULL
            MonthViewState.FULL -> MonthViewState.WEEK
        }
    }

    fun expandMonth() {
        _monthViewState.value = when (_monthViewState.value) {
            MonthViewState.WEEK -> MonthViewState.MONTH
            else -> MonthViewState.FULL
        }
    }

    fun collapseMonth() {
        _monthViewState.value = when (_monthViewState.value) {
            MonthViewState.FULL -> MonthViewState.MONTH
            else -> MonthViewState.WEEK
        }
    }

    fun toggleReminder(event: CalendarEvent) {
        viewModelScope.launch {
            val newReminder = !event.reminder
            var updatedEvent = event.copy(reminder = newReminder)
            
            if (newReminder) {
                val toSave = if (updatedEvent.id <= 0) updatedEvent.copy(id = 0) else updatedEvent
                val newId = eventDao.insertEvent(toSave)
                updatedEvent = toSave.copy(id = newId.toInt())
                
                val settings = settingsRepository.settings.value
                val offset = updatedEvent.reminderOffset.ifBlank { settings.defaultReminder }
                EventUtils.scheduleReminder(
                    application, 
                    updatedEvent.title, 
                    updatedEvent.date, 
                    updatedEvent.startTime, 
                    updatedEvent.isAllDay, 
                    offset, 
                    settings, 
                    updatedEvent.id, 
                    updatedEvent.eventType
                )
            } else {
                if (event.id > 0) {
                    eventDao.insertEvent(updatedEvent)
                }
                ReminderScheduler.cancel(application, event.title, event.date)
            }
            _selectedEvent.value = updatedEvent
            updateWidget()
        }
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun selectToday() {
        selectDate(LocalDate.now())
    }

    fun addEvent(
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
    ) {
        viewModelScope.launch {
            val id = addEventUseCase(
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
            if (reminder) {
                EventUtils.scheduleReminder(
                    context = application,
                    title = title,
                    date = date,
                    startTime = startTime,
                    isAllDay = isAllDay,
                    offsetLabel = reminderOffset,
                    settings = settingsRepository.settings.value,
                    eventId = id.toInt(),
                    eventType = eventType
                )
            }
            updateWidget()
        }
    }

    fun deleteEvent(event: CalendarEvent, onlyThisInstance: Boolean = false) {
        viewModelScope.launch {
            if (onlyThisInstance && event.repeat != "none") {
                val updatedExclusions = if (event.excludedDates.isEmpty()) {
                    event.date.toString()
                } else {
                    "${event.excludedDates},${event.date}"
                }
                eventDao.insertEvent(event.copy(excludedDates = updatedExclusions))
            } else {
                eventDao.deleteEvent(event)
            }
            updateWidget()
        }
    }

    private fun updateWidget() {
        viewModelScope.launch {
            try {
                CalendarWidget().updateAll(application)
            } catch (e: Exception) {
                AppLog.e("CalendarViewModel", "Operation failed", e)
            }
        }
    }
}

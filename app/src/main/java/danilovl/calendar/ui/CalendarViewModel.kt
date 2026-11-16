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
import danilovl.calendar.service.groupEventsByDate
import danilovl.calendar.service.isEventOnDate
import danilovl.calendar.util.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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
            // Data covers year-1..year+1, so reload only when the year changes, not on every month swipe.
            val yearFlow = _currentMonth.map { it.year }.distinctUntilChanged()
            combine(settingsRepository.settings, yearFlow) { settings, year ->
                settings to year
            }.collectLatest { (settings, year) ->
                loadHolidays(year, listOf(settings.holidayCountry) + settings.additionalHolidayCountries)
                loadExternalEvents(year, settings.enabledCalendars)
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

    private var externalJob: Job? = null
    private var holidaysJob: Job? = null

    /** Cancels a previous load so that a slow, stale response cannot overwrite a newer one. */
    fun loadExternalEvents(year: Int, calendarIds: Set<Long>) {
        externalJob?.cancel()
        externalJob = viewModelScope.launch {
            if (calendarIds.isEmpty()) {
                _externalEvents.value = emptyList()
                return@launch
            }
            try {
                val start = LocalDate.of(year - 1, 1, 1)
                val end = LocalDate.of(year + 1, 12, 31)
                _externalEvents.value = externalCalendarRepository.getEvents(calendarIds, start, end)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("CalendarViewModel", "Operation failed", e)
                _externalEvents.value = emptyList()
            }
        }
    }

    fun loadHolidays(year: Int, countries: List<String>) {
        holidaysJob?.cancel()
        holidaysJob = viewModelScope.launch {
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("CalendarViewModel", "Failed to load holidays", e)
                emptyList()
            }
        }
    }

    private val localEvents: StateFlow<List<CalendarEvent>> = getEventsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val displayedYear = _currentMonth.map { it.year }.distinctUntilChanged()

    val allEvents: StateFlow<List<CalendarEvent>> = combine(
        localEvents,
        externalEvents,
        holidays,
        contactBirthdays,
        displayedYear
    ) { local, external, hols, birthdays, year ->
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
        val virtualBirthdays = listOf(year - 1, year, year + 1).flatMap { y ->
            birthdays.mapNotNull {
                try {
                    CalendarEvent(
                        id = -2,
                        title = it.name,
                        date = it.date.withYear(y),
                        eventType = EventType.BIRTHDAY.value,
                        isAllDay = true,
                        description = "BIRTHDAY_ORIG_DATE:${it.date}|" + application.getString(R.string.birthday_subtitle)
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
        // Local events are never merged with each other: two events with the same title on one day
        // are both legitimate. Only other sources are dropped when they duplicate a local event
        // (e.g. a birthday saved locally with a reminder, or a holiday already entered by hand).
        fun key(e: CalendarEvent) = e.title.lowercase().trim() to e.date
        val localKeys = HashSet<Pair<String, LocalDate>>()
        local.forEach { localKeys.add(key(it)) }
        // A birthday saved locally (e.g. to get a reminder) repeats yearly — hide the contact copy in every year.
        val localYearly = HashSet<Pair<String, java.time.MonthDay>>()
        local.filter { it.repeat == "yearly" }.forEach {
            localYearly.add(it.title.lowercase().trim() to java.time.MonthDay.from(it.date))
        }
        val others = (external.distinctBy { Triple(it.id, it.date, it.startTime) } +
            (virtualHols + virtualBirthdays).distinctBy { key(it) })
            .filter { key(it) !in localKeys }
            .filter { it.id >= 0 || (it.title.lowercase().trim() to java.time.MonthDay.from(it.date)) !in localYearly }
        (local + others)
            .sortedWith(compareBy({ !it.isAllDay }, { it.startTime }))
    }
        .flowOn(Dispatchers.Default) // merge/sort must not compete with animation frames on the main thread
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Events per day for the displayed year +-1 (recurring and multi-day events expanded).
     * Computed once per data change on a background thread, so calendar pages only do map lookups
     * while composing — no per-day filtering of all events during swipes and expand/collapse animations.
     */
    val eventsByDate: StateFlow<Map<LocalDate, List<CalendarEvent>>> = combine(allEvents, displayedYear) { events, year ->
        groupEventsByDate(events, LocalDate.of(year - 1, 1, 1), LocalDate.of(year + 1, 12, 31))
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val eventsForSelectedDate: StateFlow<List<CalendarEvent>> = combine(
        _selectedDate,
        allEvents
    ) { date, allEvs ->
        allEvs.filter { isEventOnDate(it, date) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val datesWithEvents: StateFlow<List<LocalDate>> = eventDao.getDatesWithEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Dates (displayed year +-1) that have at least one event. */
    val yearEventDates: StateFlow<Set<LocalDate>> = eventsByDate
        .map { it.keys.toSet() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

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
        _selectedDate.value = date
        if (updateHighlighted) {
            _highlightedDate.value = date
        }
        // Holidays and external events reload through the _currentMonth collector in init.
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

    fun clearAllOverlays() {
        _showAddEvent.value = false
        _selectedEvent.value = null
    }

    fun showEventDetails(eventId: Int, origDateStr: String? = null, eventDateStr: String? = null) {
        clearAllOverlays()
        viewModelScope.launch {
            val event = eventDao.getEventById(eventId)
            if (event != null) {
                val updatedEvent = if (event.eventType == EventType.BIRTHDAY.value && 
                    origDateStr != null && 
                    !event.description.contains("BIRTHDAY_ORIG_DATE:")) {
                    event.copy(description = "BIRTHDAY_ORIG_DATE:$origDateStr|" + event.description)
                } else event
                
                _selectedEvent.value = updatedEvent
                val dateToSelect = eventDateStr?.let {
                    try { LocalDate.parse(it) } catch (e: Exception) { null }
                } ?: updatedEvent.date
                selectDate(dateToSelect)
            }
        }
    }

    fun showVirtualEventDetails(type: String, title: String, date: LocalDate, origDateStr: String? = null) {
        clearAllOverlays()
        val isHoliday = type == EventType.HOLIDAY.value
        val virtualId = if (isHoliday) -1 else -2
        val virtualDescription = if (isHoliday) {
            application.getString(R.string.holiday_subtitle)
        } else {
            "BIRTHDAY_ORIG_DATE:${origDateStr ?: date}|" + application.getString(R.string.birthday_subtitle)
        }
        val virtualEvent = CalendarEvent(
            id = virtualId,
            title = title,
            date = date,
            eventType = type,
            isAllDay = true,
            description = virtualDescription
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
        if (event.eventType == EventType.EXTERNAL.value) return
        viewModelScope.launch {
            val newReminder = !event.reminder
            var updatedEvent = event.copy(reminder = newReminder)

            if (newReminder) {
                if (updatedEvent.id <= 0) {
                    // A virtual birthday/holiday becomes a stored event; birthdays repeat yearly.
                    val repeat = if (event.eventType == EventType.BIRTHDAY.value) "yearly" else event.repeat
                    val toSave = updatedEvent.copy(id = 0, repeat = repeat)
                    val newId = eventDao.insertEvent(toSave)
                    updatedEvent = toSave.copy(id = newId.toInt())
                } else {
                    eventDao.updateEvent(updatedEvent)
                }
                ReminderScheduler.schedule(application, updatedEvent, settingsRepository.settings.value)
            } else {
                if (event.id > 0) {
                    eventDao.updateEvent(updatedEvent)
                }
                ReminderScheduler.cancel(application, event.id)
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
        repeatUntil: LocalDate? = null,
        originalEvent: CalendarEvent? = null,
        onlyThisInstance: Boolean = false,
        instanceDate: LocalDate? = null
    ) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.value
            val finalDescription = if (eventType == EventType.BIRTHDAY.value && !description.contains("BIRTHDAY_ORIG_DATE:")) {
                "BIRTHDAY_ORIG_DATE:$date|$description"
            } else description
            val finalRepeat = if (onlyThisInstance) "none" else repeat
            val finalRepeatUntil = if (onlyThisInstance) null else repeatUntil

            val template = CalendarEvent(
                title = title,
                description = finalDescription,
                date = date,
                endDate = endDate,
                startTime = startTime,
                endTime = endTime,
                isAllDay = isAllDay,
                repeat = finalRepeat,
                reminder = reminder,
                reminderMelody = reminderMelody,
                reminderOffset = reminderOffset,
                timeZone = timeZone,
                eventType = eventType,
                color = color,
                location = location,
                repeatUntil = finalRepeatUntil
            )

            val stored = originalEvent?.takeIf { it.id > 0 && it.eventType != EventType.EXTERNAL.value }

            try {
                when {
                    stored != null && onlyThisInstance && stored.repeat != "none" && instanceDate != null -> {
                        // Detach one occurrence: exclude it from the series and store it separately.
                        val series = stored.withExcluded(instanceDate)
                        val newId = eventDao.updateAndInsert(series, template)
                        ReminderScheduler.schedule(application, series, settings)
                        ReminderScheduler.schedule(application, template.copy(id = newId.toInt()), settings)
                    }
                    stored != null -> {
                        // Edit in place: keeps the id (widget/notification links) and excluded dates.
                        val updated = template.copy(id = stored.id, excludedDates = stored.excludedDates)
                        eventDao.updateEvent(updated)
                        ReminderScheduler.schedule(application, updated, settings)
                    }
                    else -> {
                        val id = addEventUseCase(template)
                        ReminderScheduler.schedule(application, template.copy(id = id.toInt()), settings)
                    }
                }
            } catch (e: Exception) {
                AppLog.e("CalendarViewModel", "Failed to save event", e)
            }
            updateWidget()
        }
    }

    fun deleteEvent(event: CalendarEvent, onlyThisInstance: Boolean = false, instanceDate: LocalDate? = null) {
        if (event.id <= 0 || event.eventType == EventType.EXTERNAL.value) return
        viewModelScope.launch {
            if (onlyThisInstance && event.repeat != "none" && instanceDate != null) {
                val series = event.withExcluded(instanceDate)
                eventDao.updateEvent(series)
                ReminderScheduler.schedule(application, series, settingsRepository.settings.value)
            } else {
                eventDao.deleteEvent(event)
                ReminderScheduler.cancel(application, event.id)
            }
            updateWidget()
        }
    }

    private fun CalendarEvent.withExcluded(day: LocalDate): CalendarEvent {
        val dates = excludedDates.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        dates.add(day.toString())
        return copy(excludedDates = dates.joinToString(","))
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

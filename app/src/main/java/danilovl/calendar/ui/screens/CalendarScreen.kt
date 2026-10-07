package danilovl.calendar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import danilovl.calendar.data.ReminderScheduler
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.repository.SettingsRepository
import danilovl.calendar.ui.CalendarViewMode
import danilovl.calendar.ui.CalendarViewModel
import danilovl.calendar.ui.MonthViewState
import danilovl.calendar.ui.components.BottomNavigationBar
import danilovl.calendar.ui.components.CalendarHeader
import danilovl.calendar.ui.components.DayEventsPopup
import danilovl.calendar.ui.components.EventDetailSheet
import danilovl.calendar.ui.components.GoToDatePicker
import danilovl.calendar.ui.components.SelectedDateDetails
import danilovl.calendar.ui.theme.XiaomiBg
import danilovl.calendar.util.EventUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository.getInstance(context) }
    val appSettings by settingsRepository.settings.collectAsState()
    
    val currentViewMode by viewModel.currentViewMode.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val highlightedDate by viewModel.highlightedDate.collectAsState()
    val events by viewModel.eventsForSelectedDate.collectAsState()
    val allEvents by viewModel.allEvents.collectAsState()
    val monthViewState by viewModel.monthViewState.collectAsState()
    val yearEventDates by viewModel.yearEventDates.collectAsState()
    val yearBirthdayDays by viewModel.yearBirthdayDays.collectAsState()
    val detailEvent by viewModel.selectedEvent.collectAsState()
    val showAddEventScreen by viewModel.showAddEvent.collectAsState()

    UseCalendarPermissions(context, appSettings, viewModel)

    val pagerState = rememberPagerState(pageCount = { 2400 }, initialPage = 1200)
    SyncPagerWithSelectedDate(pagerState, selectedDate, currentViewMode, viewModel)

    val weekStartsSunday = appSettings.weekStart == "sunday"
    
    val isTodayHighlighted = remember(currentViewMode, currentMonth, selectedDate) {
        val today = LocalDate.now()
        when (currentViewMode) {
            CalendarViewMode.YEAR -> selectedDate.year != today.year
            CalendarViewMode.MONTH -> currentMonth != YearMonth.from(today)
            CalendarViewMode.WEEK -> {
                val firstDayOfWeek = if (weekStartsSunday) DayOfWeek.SUNDAY else DayOfWeek.MONDAY
                val adjuster = TemporalAdjusters.previousOrSame(firstDayOfWeek)

                val startOfWeekToday = today.with(adjuster)
                val startOfWeekSelected = selectedDate.with(adjuster)

                !startOfWeekToday.isEqual(startOfWeekSelected)
            }
            CalendarViewMode.DAY -> !selectedDate.isEqual(today)
        }
    }

    var showSettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showSearchScreen by rememberSaveable { mutableStateOf(false) }
    var showDateCalcScreen by rememberSaveable { mutableStateOf(false) }
    var showGoToDate by rememberSaveable { mutableStateOf(false) }
    var editingEvent by rememberSaveable { mutableStateOf<CalendarEvent?>(null) }
    var isEditOnlyThisInstance by rememberSaveable { mutableStateOf(false) }
    var popupDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var newEventTime by rememberSaveable { mutableStateOf<LocalTime?>(null) }

    androidx.compose.runtime.LaunchedEffect(detailEvent) {
        if (detailEvent != null) {
            showSettingsScreen = false
            showSearchScreen = false
            showDateCalcScreen = false
            showGoToDate = false
            editingEvent = null
            popupDate = null
        }
    }

    val anyOverlay = showAddEventScreen || editingEvent != null ||
        showSearchScreen || showSettingsScreen || showDateCalcScreen
    BackHandler(enabled = anyOverlay || currentViewMode != CalendarViewMode.MONTH) {
        when {
            editingEvent != null -> editingEvent = null
            showAddEventScreen -> viewModel.dismissAddEvent()
            showSearchScreen -> showSearchScreen = false
            showSettingsScreen -> showSettingsScreen = false
            showDateCalcScreen -> showDateCalcScreen = false
            currentViewMode != CalendarViewMode.MONTH -> viewModel.setViewMode(CalendarViewMode.MONTH)
        }
    }

    if (showGoToDate) {
        GoToDatePicker(
            initialDate = selectedDate,
            onDismiss = { showGoToDate = false },
            onPick = { date ->
                showGoToDate = false
                viewModel.setViewMode(CalendarViewMode.MONTH)
                viewModel.selectDate(date)
            }
        )
    }

    detailEvent?.let { event ->
        EventDetailSheet(
            event = event,
            onDismiss = { viewModel.clearSelectedEvent() },
            onEdit = { ev, onlyThis ->
                viewModel.clearSelectedEvent()
                editingEvent = ev
                isEditOnlyThisInstance = onlyThis
            },
            onDelete = { event, onlyThis ->
                if (!onlyThis) {
                    ReminderScheduler.cancel(context, event.title, event.date)
                }
                viewModel.deleteEvent(event, onlyThis)
                viewModel.clearSelectedEvent()
            },
            onToggleReminder = {
                viewModel.toggleReminder(event)
            },
            settings = appSettings,
            selectedDate = selectedDate
        )
    }

    popupDate?.let { date ->
        val dayEvents = remember(allEvents, date) { allEvents.filter { it.date == date } }
        DayEventsPopup(
            date = date,
            events = dayEvents,
            onDismiss = { popupDate = null },
            onEventClick = { popupDate = null; viewModel.selectEvent(it) }
        )
    }

    if (showSettingsScreen) {
        SettingsScreen(onBack = { showSettingsScreen = false })
        return
    }

    if (showSearchScreen) {
        SearchScreen(
            allEvents = allEvents,
            onBack = { showSearchScreen = false },
            onEventClick = { viewModel.selectEvent(it) }
        )
        return
    }

    if (showDateCalcScreen) {
        DateCalcScreen(initialDate = selectedDate, onBack = { showDateCalcScreen = false })
        return
    }

    val currentEditingEvent = editingEvent
    if (showAddEventScreen || currentEditingEvent != null) {
        AddEventScreen(
            selectedDate = selectedDate,
            initialEvent = currentEditingEvent,
            initialTime = newEventTime,
            defaultOnlyThis = isEditOnlyThisInstance,
            onDismiss = {
                viewModel.dismissAddEvent()
                editingEvent = null
                newEventTime = null
            },
            onConfirm = { title, desc, startDate, endDate, start, end, allDay,
                          repeat, rem, melody, reminderOffset, tz, type, color, location, repUntil, onlyThis ->
                viewModel.addEvent(
                    title, desc, startDate, endDate, start, end, allDay,
                    repeat, rem, melody, reminderOffset, tz, type, color, location, repUntil,
                    currentEditingEvent, onlyThis
                )
                viewModel.dismissAddEvent()
                editingEvent = null
                newEventTime = null
            }
        )
        return
    }


    Scaffold(
        containerColor = XiaomiBg,
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            CalendarHeader(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                viewMode = currentViewMode,
                onTodayClick = { viewModel.selectToday() },
                onAddEventClick = { newEventTime = null; viewModel.requestAddEvent() },
                onSettingsClick = { showSettingsScreen = true },
                onSearchClick = { showSearchScreen = true },
                onGoToDateClick = { showGoToDate = true },
                onDateCalcClick = { showDateCalcScreen = true },
                onShareClick = { EventUtils.shareEvents(context, allEvents) },
                isTodayHighlighted = isTodayHighlighted
            )
        },
        bottomBar = {
            BottomNavigationBar(currentViewMode) { viewModel.setViewMode(it) }
        },
        floatingActionButton = {}
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AnimatedContent(targetState = currentViewMode, label = "viewTransition") { mode ->
                when (mode) {
                    CalendarViewMode.YEAR -> YearView(
                        selectedDate = selectedDate,
                        datesWithEvents = yearEventDates,
                        birthdayDays = yearBirthdayDays,
                        weekStartsSunday = weekStartsSunday,
                        onYearChange = { viewModel.selectYear(it) },
                        onMonthClick = { date ->
                            viewModel.selectDate(date)
                            viewModel.setViewMode(CalendarViewMode.MONTH)
                            viewModel.setMonthViewState(MonthViewState.MONTH)
                        }
                    )
                    CalendarViewMode.MONTH -> MonthView(
                        selectedDate = selectedDate,
                        highlightedDate = highlightedDate,
                        monthState = monthViewState,
                        pagerState = pagerState,
                        allEvents = allEvents,
                        birthdays = emptyList(),
                        holidays = emptyList(),
                        onDateSelect = { date, manual -> viewModel.selectDate(date, manual) },
                        onSetState = { viewModel.setMonthViewState(it) },
                        onHandleTap = { viewModel.cycleMonthViewState() },
                        onEventClick = { viewModel.selectEvent(it) },
                        onDayDetail = { popupDate = it },
                        weekStartsSunday = weekStartsSunday,
                        settings = appSettings
                    ) {
                        SelectedDateDetails(
                            events = events,
                            onDeleteEvent = { event ->
                                ReminderScheduler.cancel(context, event.title, event.date)
                                viewModel.deleteEvent(event)
                            },
                            onEventClick = { viewModel.selectEvent(it) },
                            settings = appSettings
                        )
                    }
                    CalendarViewMode.WEEK -> WeekView(
                        selectedDate = selectedDate,
                        highlightedDate = highlightedDate,
                        allEvents = allEvents,
                        weekStartsSunday = weekStartsSunday,
                        onDateSelect = { date, manual -> viewModel.selectDate(date, manual) },
                        onAddEvent = { date, time ->
                            viewModel.selectDate(date, true)
                            newEventTime = time
                            viewModel.requestAddEvent()
                        },
                        onEventClick = { viewModel.selectEvent(it) },
                        settings = appSettings
                    )
                    CalendarViewMode.DAY -> DayView(
                        selectedDate = selectedDate,
                        highlightedDate = highlightedDate,
                        allEvents = allEvents,
                        onDateSelect = { date, manual -> viewModel.selectDate(date, manual) },
                        onAddEvent = { date, time ->
                            viewModel.selectDate(date, true)
                            newEventTime = time
                            viewModel.requestAddEvent()
                        },
                        onEventClick = { viewModel.selectEvent(it) },
                        settings = appSettings
                    )
                }
            }
        }
    }
}


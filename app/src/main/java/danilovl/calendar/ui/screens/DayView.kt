package danilovl.calendar.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import danilovl.calendar.R
import danilovl.calendar.data.EventType
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.repository.AppSettings
import danilovl.calendar.ui.appLocale
import danilovl.calendar.ui.components.eventAccentColor
import danilovl.calendar.ui.components.rememberCurrentDateTime
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiEventPinkText
import danilovl.calendar.ui.theme.XiaomiRed
import danilovl.calendar.ui.theme.XiaomiSurface
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.util.DateTimeUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

private val HOUR_HEIGHT = 64.dp
private val TIME_GUTTER = 48.dp

@Composable
fun DayView(
    selectedDate: LocalDate,
    highlightedDate: LocalDate,
    allEvents: List<CalendarEvent>,
    onDateSelect: (LocalDate, Boolean) -> Unit,
    onAddEvent: (LocalDate, LocalTime) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    settings: AppSettings
) {
    val initialPage = 1000
    val pagerState = rememberPagerState(pageCount = { 2000 }, initialPage = initialPage)
    
    val referenceDate = remember { selectedDate }

    val currentSelected by rememberUpdatedState(selectedDate)

    val currentDateTime = rememberCurrentDateTime()
    val today = currentDateTime.toLocalDate()
    val nowMinutes = currentDateTime.hour * 60 + currentDateTime.minute
    val nowText = currentDateTime.format(DateTimeUtils.timeFormatter)

    LaunchedEffect(selectedDate) {
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(referenceDate, selectedDate).toInt()
        val targetPage = initialPage + daysDiff
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress && targetPage in 0 until 2000) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val targetDate = referenceDate.plusDays((page - initialPage).toLong())
            if (targetDate != currentSelected) {
                onDateSelect(targetDate, false)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val date = referenceDate.plusDays((page - initialPage).toLong())
        
        val dayEvents = remember(allEvents, date) {
            val filtered = mutableListOf<CalendarEvent>()
            allEvents.forEach {
                if (it.date == date) filtered.add(it)
            }
            filtered
        }
        val timedEvents = remember(dayEvents) {
            dayEvents.filter { !it.isAllDay && it.startTime != null }
        }
        val dayHolidays = remember(dayEvents) {
            dayEvents.filter { it.eventType == EventType.HOLIDAY.value }
        }
        val dayBirthdays = remember(dayEvents) {
            dayEvents.filter { it.eventType == EventType.BIRTHDAY.value }
        }
        val allDayEvents = remember(dayEvents) {
            dayEvents.filter { (it.isAllDay || it.startTime == null) && it.eventType != EventType.HOLIDAY.value && it.eventType != EventType.BIRTHDAY.value }
        }
        
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        LaunchedEffect(page) {
            val isToday = date == today
            val startHour = if (isToday) (nowMinutes / 60f - 1.5f).coerceIn(0f, 23f) else 7f
            scrollState.scrollTo(with(density) { (HOUR_HEIGHT * startHour).roundToPx() })
        }

        Column(modifier = Modifier.fillMaxSize().background(XiaomiSurface)) {
            Row(
                modifier = Modifier.fillMaxWidth().background(XiaomiSurface).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .then(
                            if (date == highlightedDate) Modifier.background(XiaomiBlue, RoundedCornerShape(16.dp))
                            else Modifier.border(1.5.dp, XiaomiBlue, RoundedCornerShape(16.dp))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (date == highlightedDate) Color.White else XiaomiBlue
                    )
                    if (dayEvents.isNotEmpty() || dayHolidays.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .size(4.dp)
                                .background(if (date == highlightedDate) Color.White else Color.Gray.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, appLocale()).replaceFirstChar { it.uppercase() },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = XiaomiTextPrimary
                    )
                    if (settings.showWeekNumber) {
                        val weekFields = WeekFields.of(if (settings.weekStart == "sunday") DayOfWeek.SUNDAY else DayOfWeek.MONDAY, 1)
                        val weekNum = date.get(weekFields.weekOfYear())
                        Text(
                            text = "${stringResource(R.string.unit_weeks_short)} $weekNum",
                            fontSize = 13.sp,
                            color = XiaomiTextSecondary
                        )
                    }
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("MMMM yyyy", appLocale())),
                        fontSize = 12.sp,
                        color = XiaomiTextSecondary
                    )
                }
            }

            val allDayCount = dayHolidays.size + dayBirthdays.size + allDayEvents.size
            if (allDayCount > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                        .then(
                            if (allDayCount > 3) Modifier
                                .heightIn(max = 140.dp)
                                .verticalScroll(rememberScrollState())
                            else Modifier
                        ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dayHolidays.forEach { h ->
                        AllDayChip(h.title, stringResource(R.string.event_holiday), XiaomiRed) {
                            onEventClick(h)
                        }
                    }
                    dayBirthdays.forEach { b ->
                        AllDayChip("🎂 ${b.title}", stringResource(R.string.event_birthday), XiaomiEventPinkText) {
                            onEventClick(b)
                        }
                    }
                    allDayEvents.forEach { event ->
                        key(event.id) {
                            val chipColor = eventAccentColor(event, settings)
                            AllDayChip(event.title, stringResource(R.string.common_all_day), chipColor) { onEventClick(event) }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                Column {
                    val topPadding = if (date == today && nowMinutes < 15) 8.dp else 0.dp
                    Box(modifier = Modifier.fillMaxWidth().padding(top = topPadding).animateContentSize()) {
                        Row(modifier = Modifier.matchParentSize()) {
                            Box(modifier = Modifier.width(TIME_GUTTER)) {
                                VerticalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.CenterEnd))
                            }
                        }
                        
                        Column {
                            for (hour in 0..23) {
                                key(hour) {
                                    HourRow(
                                        hour = hour,
                                        onClick = { onAddEvent(date, LocalTime.of(hour, 0)) }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.matchParentSize().padding(start = TIME_GUTTER)) {
                            timedEvents.forEach { event ->
                                key(event.id) {
                                    val start = event.startTime!!
                                    val startMin = start.hour * 60 + start.minute
                                    val endMin = event.endTime?.let { it.hour * 60 + it.minute }
                                        ?.takeIf { it > startMin } ?: (startMin + 60)
                                    Box(
                                        modifier = Modifier
                                            .offset(y = HOUR_HEIGHT * (startMin / 60f))
                                            .fillMaxWidth()
                                            .height((HOUR_HEIGHT * ((endMin - startMin) / 60f)).coerceAtLeast(24.dp))
                                            .padding(horizontal = 8.dp, vertical = 1.dp)
                                    ) {
                                        EventCard(event, onEventClick, settings)
                                    }
                                }
                            }
                        }

                        if (date == today) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = HOUR_HEIGHT * (nowMinutes / 60f) - 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(TIME_GUTTER), contentAlignment = Alignment.Center) {
                                    Surface(color = XiaomiRed, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            nowText,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = XiaomiRed, thickness = 2.dp, modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HourRow(hour: Int, onClick: () -> Unit) {
    val hourText = String.format(appLocale(), "%02d:00", hour)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HOUR_HEIGHT)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(TIME_GUTTER)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = hourText,
                fontSize = 11.sp,
                color = XiaomiTextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun EventCard(event: CalendarEvent, onClick: (CalendarEvent) -> Unit, settings: AppSettings) {
    val chipColor = eventAccentColor(event, settings)
    Surface(
        color = chipColor.copy(alpha = 0.9f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick(event) }
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(text = event.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            if (event.startTime != null && event.endTime != null) {
                Text(
                    text = "${event.startTime.format(DateTimeUtils.timeFormatter)} - ${event.endTime.format(DateTimeUtils.timeFormatter)}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun AllDayChip(title: String, label: String, color: Color, onClick: (() -> Unit)?) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 14.sp, color = XiaomiTextPrimary, maxLines = 1)
        }
    }
}

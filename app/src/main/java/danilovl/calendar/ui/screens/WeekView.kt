package danilovl.calendar.ui.screens

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import danilovl.calendar.R
import danilovl.calendar.data.EventType
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.repository.AppSettings
import danilovl.calendar.ui.components.eventAccentColor
import danilovl.calendar.ui.components.rememberCurrentDateTime
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiEventPinkBg
import danilovl.calendar.ui.theme.XiaomiEventPinkText
import danilovl.calendar.ui.theme.XiaomiRed
import danilovl.calendar.ui.theme.XiaomiSurface
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.ui.theme.XiaomiWeekendBlue
import danilovl.calendar.ui.appLocale
import danilovl.calendar.ui.trans
import danilovl.calendar.ui.weekdayLabels
import danilovl.calendar.util.DateTimeUtils
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.WeekFields

private val HOUR_HEIGHT = 48.dp
private val TIME_GUTTER = 48.dp
private val ALL_DAY_HEIGHT = 44.dp


@Composable
fun WeekView(
    selectedDate: LocalDate,
    highlightedDate: LocalDate,
    allEvents: List<CalendarEvent>,
    weekStartsSunday: Boolean = false,
    onDateSelect: (LocalDate, Boolean) -> Unit,
    onAddEvent: (LocalDate, LocalTime) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    settings: AppSettings
) {
    val initialPage = 1000
    val pagerState = rememberPagerState(pageCount = { 2000 }, initialPage = initialPage)

    val referenceStart = remember(weekStartsSunday) { DateTimeUtils.startOfWeek(selectedDate, weekStartsSunday) }
    val currentSelected by rememberUpdatedState(selectedDate)

    val currentDateTime = rememberCurrentDateTime()
    val today = currentDateTime.toLocalDate()
    val nowMinutes = currentDateTime.hour * 60 + currentDateTime.minute
    val nowText = currentDateTime.format(DateTimeUtils.timeFormatter)
    val weekLabels = weekdayLabels(weekStartsSunday)

    var selectedSlot by remember { mutableStateOf<Pair<LocalDate, Int>?>(null) }

    LaunchedEffect(selectedDate, weekStartsSunday) {
        val targetStart = DateTimeUtils.startOfWeek(selectedDate, weekStartsSunday)
        val weeksDiff = java.time.temporal.ChronoUnit.WEEKS.between(referenceStart, targetStart).toInt()
        val targetPage = initialPage + weeksDiff
        if (targetPage in 0 until 2000) {
            snapshotFlow { pagerState.isScrollInProgress }.filter { !it }.first()
            if (pagerState.currentPage != targetPage) {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val start = referenceStart.plusWeeks((page - initialPage).toLong())
            val targetDate = start.plusDays(DateTimeUtils.columnIndex(currentSelected, weekStartsSunday).toLong())
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
        val weekStart = referenceStart.plusWeeks((page - initialPage).toLong())
        val weekEnd = weekStart.plusDays(6)
        val weekNumber = remember(weekStart) { weekStart.get(WeekFields.ISO.weekOfWeekBasedYear()) }

        val weekEvents = remember(allEvents, weekStart) {
            val filtered = mutableListOf<CalendarEvent>()
            allEvents.forEach {
                if (!it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd)) {
                    filtered.add(it)
                }
            }
            filtered
        }
        val eventsByDate = remember(weekEvents) {
            val grouped = mutableMapOf<LocalDate, MutableList<CalendarEvent>>()
            weekEvents.forEach {
                if (!it.isAllDay && it.startTime != null) {
                    grouped.getOrPut(it.date) { mutableListOf() }.add(it)
                }
            }
            grouped
        }
        val allDayByDate = remember(weekEvents) {
            val grouped = mutableMapOf<LocalDate, MutableList<CalendarEvent>>()
            weekEvents.forEach {
                if (it.isAllDay || it.startTime == null) {
                    grouped.getOrPut(it.date) { mutableListOf() }.add(it)
                }
            }
            grouped
        }

        Column(modifier = Modifier
            .fillMaxSize()
            .background(XiaomiSurface)) {
            WeekDayHeader(weekStart, today, highlightedDate, weekNumber, weekLabels) { onDateSelect(it, true) }
            WeekAllDayRow(weekStart, allDayByDate, onEventClick, settings)
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

            val scrollState = rememberScrollState()
            val density = LocalDensity.current
            LaunchedEffect(page, today) {
                val todayInWeek = !today.isBefore(weekStart) && !today.isAfter(weekEnd)
                val startHour = if (todayInWeek) (nowMinutes / 60f - 1.5f).coerceIn(0f, 23f) else 7f
                scrollState.scrollTo(with(density) { (HOUR_HEIGHT * startHour).roundToPx() })
            }

            Box(modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)) {
                Column {
                    WeekTimeGrid(
                        weekStart = weekStart,
                        today = today,
                        nowMinutes = nowMinutes,
                        nowText = nowText,
                        eventsByDate = eventsByDate,
                        selectedSlot = selectedSlot,
                        onSlotSelect = { selectedSlot = it },
                        onAddEvent = { date, time -> selectedSlot = null; onAddEvent(date, time) },
                        onEventClick = onEventClick,
                        settings = settings
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun WeekDayHeader(
    weekStart: LocalDate,
    today: LocalDate,
    selected: LocalDate,
    weekNumber: Int,
    labels: List<String>,
    onDateSelect: (LocalDate) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(XiaomiSurface)) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)) {
            Spacer(Modifier.width(TIME_GUTTER))
            labels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = XiaomiTextSecondary
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(TIME_GUTTER), contentAlignment = Alignment.Center) {
                Text(
                    weekNumber.toString(),
                    fontSize = 10.sp,
                    color = XiaomiWeekendBlue,
                    fontWeight = FontWeight.Normal
                )
            }
            for (i in 0..6) {
                val date = weekStart.plusDays(i.toLong())
                val isSelected = date == selected
                val isToday = date == today
                val isWeekend = date.dayOfWeek.value >= 6
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .width(46.dp)
                            .height(50.dp)
                            .then(
                                when {
                                    isSelected -> Modifier.background(
                                        XiaomiBlue, RoundedCornerShape(12.dp)
                                    )

                                    isToday -> Modifier.border(
                                        1.5.dp, XiaomiBlue, RoundedCornerShape(12.dp)
                                    )

                                    else -> Modifier
                                }
                            )
                            .clickable { onDateSelect(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = when {
                                isSelected -> Color.White
                                isWeekend -> XiaomiWeekendBlue
                                else -> XiaomiTextPrimary
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekAllDayRow(
    weekStart: LocalDate,
    allDayByDate: Map<LocalDate, List<CalendarEvent>>,
    onEventClick: (CalendarEvent) -> Unit,
    settings: AppSettings
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ALL_DAY_HEIGHT)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier
            .width(TIME_GUTTER)
            .fillMaxHeight(), contentAlignment = Alignment.Center) {
            Text(
                trans(R.string.common_all_day).replace(" ", "\n"),
                fontSize = 9.sp,
                color = XiaomiTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )
        }
        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            val dayAllDayEvents = allDayByDate[date] ?: emptyList()
            val dayHols = dayAllDayEvents.filter { it.eventType == EventType.HOLIDAY.value }
            val dayBirthdays = dayAllDayEvents.filter { it.eventType == EventType.BIRTHDAY.value }
            val dayRegular = dayAllDayEvents.filter { it.eventType != EventType.HOLIDAY.value && it.eventType != EventType.BIRTHDAY.value }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 1.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                dayHols.forEach { h ->
                    Surface(
                        color = XiaomiRed.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onEventClick(h)
                            }
                    ) {
                        Text(
                            h.title,
                            fontSize = 8.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                        )
                    }
                }
                dayBirthdays.forEach { b ->
                    Surface(
                        color = XiaomiEventPinkBg,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onEventClick(b)
                            }
                    ) {
                        Text(
                            b.title,
                            fontSize = 8.sp,
                            color = XiaomiEventPinkText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                        )
                    }
                }
                dayRegular.forEach { e ->
                    val chipColor = eventAccentColor(e, settings)
                    Surface(color = chipColor.copy(alpha = 0.85f), shape = RoundedCornerShape(6.dp), modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEventClick(e) }) {
                        Text(
                            e.title,
                            fontSize = 8.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekTimeGrid(
    weekStart: LocalDate,
    today: LocalDate,
    nowMinutes: Int,
    nowText: String,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    selectedSlot: Pair<LocalDate, Int>?,
    onSlotSelect: (Pair<LocalDate, Int>) -> Unit,
    onAddEvent: (LocalDate, LocalTime) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    settings: AppSettings
) {
    val gridColor = Color.LightGray.copy(alpha = 0.5f)
    val density = LocalDensity.current
    val weekEnd = weekStart.plusDays(6)
    val showNow = !today.isBefore(weekStart) && !today.isAfter(weekEnd)

    val topPadding = if (showNow && nowMinutes < 15) 8.dp else 0.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HOUR_HEIGHT * 24 + topPadding)
            .padding(top = topPadding)
            .drawBehind {
                val gutterPx = with(density) { TIME_GUTTER.toPx() }
                val hourPx = with(density) { HOUR_HEIGHT.toPx() }
                val colWidth = (size.width - gutterPx) / 7f
                val strokeWidth = 1.dp.toPx()
                val topPaddingPx = with(density) { topPadding.toPx() }
                for (h in 0..24) {
                    val y = h * hourPx + topPaddingPx
                    drawLine(gridColor, Offset(gutterPx, y), Offset(size.width, y), strokeWidth)
                }
                for (c in 0..7) {
                    val x = gutterPx + c * colWidth
                    drawLine(
                        gridColor, Offset(x, topPaddingPx), Offset(x, size.height), strokeWidth
                    )
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.width(TIME_GUTTER)) {
                for (hour in 0..23) {
                    Box(modifier = Modifier
                        .height(HOUR_HEIGHT)
                        .fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        Text(
                            String.format(appLocale(), "%02d:00", hour),
                            fontSize = 10.sp,
                            color = XiaomiTextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            for (i in 0..6) {
                val date = weekStart.plusDays(i.toLong())
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        for (hour in 0..23) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(HOUR_HEIGHT)
                                    .clickable { onSlotSelect(date to hour) }
                            ) {
                                if (selectedSlot == date to hour) {
                                    Surface(
                                        color = XiaomiBlue,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp)
                                            .clickable { onAddEvent(date, LocalTime.of(hour, 0)) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    (eventsByDate[date] ?: emptyList()).forEach { event ->
                        key(event.id) {
                            val start = event.startTime!!
                            val startMin = start.hour * 60 + start.minute
                            val endMin = event.endTime?.let { it.hour * 60 + it.minute }
                                ?.takeIf { it > startMin } ?: (startMin + 60)
                            val chipColor = eventAccentColor(event, settings)
                            Surface(
                                color = chipColor.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .offset(y = HOUR_HEIGHT * (startMin / 60f))
                                    .padding(horizontal = 1.dp)
                                    .fillMaxWidth()
                                    .height(
                                        (HOUR_HEIGHT * ((endMin - startMin) / 60f)).coerceAtLeast(
                                            20.dp
                                        )
                                    )
                                    .clickable { onEventClick(event) }
                            ) {
                                Text(
                                    event.title,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showNow) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .offset(y = HOUR_HEIGHT * (nowMinutes / 60f) - 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(TIME_GUTTER), contentAlignment = Alignment.Center) {
                        Surface(color = XiaomiRed, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                nowText,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
                            )
                        }
                    }
                    for (i in 0..6) {
                        val date = weekStart.plusDays(i.toLong())
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (date == today) {
                                HorizontalDivider(color = XiaomiRed, thickness = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

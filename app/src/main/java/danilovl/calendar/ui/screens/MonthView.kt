package danilovl.calendar.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import danilovl.calendar.R
import danilovl.calendar.data.CalendarDay
import danilovl.calendar.data.EventType
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.repository.AppSettings
import danilovl.calendar.data.repository.ContactBirthday
import danilovl.calendar.data.repository.Holiday
import danilovl.calendar.service.generateDaysForMonth
import danilovl.calendar.service.isEventOnDate
import danilovl.calendar.ui.MonthViewState
import danilovl.calendar.ui.components.eventAccentColor
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiEventPinkBg
import danilovl.calendar.ui.theme.XiaomiEventPinkText
import danilovl.calendar.ui.theme.XiaomiRed
import danilovl.calendar.ui.theme.XiaomiSurface
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.ui.theme.XiaomiWeekendBlue
import danilovl.calendar.ui.trans
import danilovl.calendar.ui.weekdayLabels
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.lerp as colorLerp

private val HEADER_HEIGHT = 40.dp
private val HANDLE_HEIGHT = 26.dp
private val COLLAPSED_ROW_HEIGHT = 52.dp
private val AGENDA_PEEK = 168.dp
private val DAY_SHAPE = RoundedCornerShape(12.dp)

@Composable
fun MonthView(
    selectedDate: LocalDate,
    highlightedDate: LocalDate,
    monthState: MonthViewState,
    pagerState: PagerState,
    allEvents: List<CalendarEvent>,
    birthdays: List<ContactBirthday>,
    holidays: List<Holiday>,
    onDateSelect: (LocalDate, Boolean) -> Unit,
    onSetState: (MonthViewState) -> Unit,
    onHandleTap: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit = {},
    onDayDetail: (LocalDate) -> Unit = {},
    weekStartsSunday: Boolean = false,
    settings: AppSettings,
    eventContent: @Composable () -> Unit = {}
) {
    val progressAnim = remember { Animatable(monthState.ordinal.toFloat()) }
    var dragProgress by remember { mutableFloatStateOf(monthState.ordinal.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }

    LaunchedEffect(monthState) {
        if (!dragging && abs(progressAnim.value - monthState.ordinal.toFloat()) > 0.001f) {
            isAnimating = true
            progressAnim.animateTo(monthState.ordinal.toFloat(), tween(300, easing = FastOutSlowInEasing))
            isAnimating = false
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { progressAnim.value }
            .collect { v ->
                if (isAnimating && !dragging && abs(v - v.roundToInt()) < 0.03f) {
                    isAnimating = false
                }
            }
    }

    val progress = { if (dragging) dragProgress else progressAnim.value }
    val isStable = !isAnimating && !dragging

    BoxWithConstraints(modifier = Modifier
        .fillMaxSize()
        .background(XiaomiSurface)) {
        val density = LocalDensity.current
        val gridAreaMaxPx = with(density) {
            (maxHeight - HEADER_HEIGHT - HANDLE_HEIGHT).coerceAtLeast(COLLAPSED_ROW_HEIGHT).toPx()
        }
        val collapsedRowPx = with(density) { COLLAPSED_ROW_HEIGHT.toPx() }
        val agendaPeekPx = with(density) { AGENDA_PEEK.toPx() }
        val fullRowPx = gridAreaMaxPx / 6f
        val monthRowPx = ((gridAreaMaxPx - agendaPeekPx) / 6f).coerceIn(collapsedRowPx, fullRowPx)

        val weekProgress = { progress().coerceIn(0f, 1f) }
        val fullProgress = { (progress() - 1f).coerceIn(0f, 1f) }
        val rowHeightPx = {
            val p = progress()
            if (p <= 1f) lerp(collapsedRowPx, monthRowPx, p.coerceIn(0f, 1f))
            else lerp(monthRowPx, fullRowPx, (p - 1f).coerceIn(0f, 1f))
        }
        val gridContentPx = { rowHeightPx() * 6f }
        val windowPx = { lerp(collapsedRowPx, gridContentPx(), weekProgress()) }

        val perStatePx = with(density) { 200.dp.toPx() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .draggable(
                    state = rememberDraggableState { delta ->
                        dragProgress = (dragProgress + delta / perStatePx).coerceIn(0f, 2f)
                    },
                    orientation = Orientation.Vertical,
                    onDragStarted = {
                        dragging = true
                        isAnimating = true
                        progressAnim.stop()
                        dragProgress = progressAnim.value
                    },
                    onDragStopped = { velocity ->
                        val from = dragProgress
                        val velocityInStates = (velocity / perStatePx).coerceIn(-2.2f, 2.2f)
                        val projected = (from + velocityInStates * 0.28f).coerceIn(0f, 2f)
                        val target = when {
                            abs(velocityInStates) > 0.22f -> {
                                if (velocityInStates > 0f) ceil(projected).toInt() else floor(
                                    projected
                                ).toInt()
                            }

                            else -> projected.roundToInt()
                        }.coerceIn(0, 2)
                        progressAnim.snapTo(from)
                        dragging = false
                        progressAnim.animateTo(
                            target.toFloat(),
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialVelocity = velocityInStates
                        )
                        isAnimating = false
                        onSetState(MonthViewState.entries[target])
                    }
                )
        ) {
            WeekDaysRow(
                weekStartsSunday = weekStartsSunday,
                showWeekNumber = settings.showWeekNumber,
                modifier = Modifier.height(HEADER_HEIGHT)
            )

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val h = windowPx().roundToInt().coerceIn(0, constraints.maxHeight)
                        val placeable =
                            measurable.measure(constraints.copy(minHeight = h, maxHeight = h))
                        layout(placeable.width, h) { placeable.place(0, 0) }
                    }
            ) { page ->
                val month = remember(page) { YearMonth.now().plusMonths((page - 1200).toLong()) }
                val days = remember(month, weekStartsSunday) {
                    generateDaysForMonth(month, null, weekStartsSunday)
                }
                val daysWithSelection = remember(days, highlightedDate) {
                    val today = LocalDate.now()
                    days.map { day ->
                        day.copy(
                            isSelected = day.date == highlightedDate,
                            isToday = day.date == today
                        )
                    }
                }
                val targetDay = selectedDate.dayOfMonth.coerceAtMost(month.lengthOfMonth())
                val activeWeekIndex = remember(days, targetDay, month) {
                    val targetDate = month.atDay(targetDay)
                    val index = days.indexOfFirst { it.date == targetDate }
                    if (index >= 0) index / 7 else 0
                }

                MonthGrid(
                    days = daysWithSelection,
                    allEvents = allEvents,
                    birthdays = birthdays,
                    holidays = holidays,
                    activeWeekIndex = activeWeekIndex,
                    rowHeightPx = rowHeightPx,
                    weekProgress = weekProgress,
                    fullProgress = fullProgress,
                    isAnimating = !isStable,
                    onDayClick = { onDateSelect(it.date, true) },
                    onDayDetail = onDayDetail,
                    onEventClick = onEventClick,
                    settings = settings
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HANDLE_HEIGHT)
                    .clickable { onHandleTap() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp),
                        shape = CircleShape,
                        color = XiaomiTextSecondary.copy(alpha = 0.2f)
                    ) {}
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = fullProgress() * 180f },
                        tint = XiaomiTextSecondary.copy(alpha = 0.5f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer { alpha = 1f - fullProgress() }
            ) {
                eventContent()
            }
        }
    }
}

@Composable
fun WeekDaysRow(modifier: Modifier = Modifier, weekStartsSunday: Boolean = false, showWeekNumber: Boolean = false) {
    val weekDays = weekdayLabels(weekStartsSunday)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showWeekNumber) {
            Spacer(modifier = Modifier.width(32.dp))
        }
        weekDays.forEachIndexed { index, day ->
            val isWeekend = if (weekStartsSunday) index == 0 || index == 6 else index >= 5
            val color = if (isWeekend) XiaomiWeekendBlue else XiaomiTextPrimary
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = color,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun WeekNumberItem(date: LocalDate, weekStartsSunday: Boolean, modifier: Modifier = Modifier) {
    val weekFields = WeekFields.of(if (weekStartsSunday) DayOfWeek.SUNDAY else DayOfWeek.MONDAY, 1)
    val weekNumber = date.get(weekFields.weekOfYear())
    Box(
        modifier = modifier.width(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = weekNumber.toString(),
            fontSize = 10.sp,
            color = XiaomiTextSecondary,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
private fun MonthGrid(
    days: List<CalendarDay>,
    allEvents: List<CalendarEvent>,
    birthdays: List<ContactBirthday>,
    holidays: List<Holiday>,
    activeWeekIndex: Int,
    rowHeightPx: () -> Float,
    weekProgress: () -> Float,
    fullProgress: () -> Float,
    isAnimating: Boolean,
    onDayClick: (CalendarDay) -> Unit,
    onDayDetail: (LocalDate) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    settings: AppSettings
) {
    val eventsByDate = remember(allEvents, days) {
        days.associate { day ->
            day.date to allEvents.filter { isEventOnDate(it, day.date) }
        }
    }
    val birthdaysByDate = remember(birthdays) {
        birthdays.groupBy { it.date.month to it.date.dayOfMonth }
    }
    val holidaysByDate = remember(holidays) {
        holidays.groupBy { it.date }
    }

    val density = LocalDensity.current
    val minNumberBoxPx = with(density) { 30.dp.toPx() }
    val maxNumberBoxPx = with(density) { 34.dp.toPx() }
    val maxNumberScale = 16f / 15f

    val sharedColumnModifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(align = Alignment.Top, unbounded = true)
        .offset {
            IntOffset(0, -((activeWeekIndex * rowHeightPx()) * (1f - weekProgress())).roundToInt())
        }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clipToBounds()
    ) {
        if (isAnimating) {
            Column(modifier = sharedColumnModifier) {
                days.chunked(7).forEach { weekDays ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .layout { measurable, constraints ->
                                val h = rowHeightPx().roundToInt().coerceAtLeast(0)
                                val placeable = measurable.measure(
                                    constraints.copy(
                                        minHeight = h,
                                        maxHeight = h
                                    )
                                )
                                layout(placeable.width, h) { placeable.place(0, 0) }
                            }
                    ) {
                        if (settings.showWeekNumber) {
                            WeekNumberItem(weekDays.first().date, settings.weekStart == "sunday", Modifier.fillMaxHeight())
                        }
                        weekDays.forEach { day ->
                            val numAlpha = if (day.isFromCurrentMonth) 1f else 0.3f
                            val dayHolidays = holidaysByDate[day.date] ?: emptyList()
                            val isHoliday = dayHolidays.isNotEmpty()
                            val numColor = when {
                                day.isSelected -> Color.White
                                isHoliday -> XiaomiRed
                                day.date.dayOfWeek.value >= 6 -> XiaomiWeekendBlue
                                else -> XiaomiTextPrimary
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(vertical = 1.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .layout { measurable, constraints ->
                                            val side =
                                                lerp(minNumberBoxPx, maxNumberBoxPx, fullProgress())
                                                    .roundToInt().coerceAtLeast(0)
                                            val placeable = measurable.measure(
                                                constraints.copy(
                                                    minWidth = side,
                                                    maxWidth = side,
                                                    minHeight = side,
                                                    maxHeight = side
                                                )
                                            )
                                            layout(side, side) { placeable.place(0, 0) }
                                        }
                                        .alpha(numAlpha)
                                        .then(
                                            when {
                                                day.isSelected -> Modifier
                                                    .background(XiaomiBlue, DAY_SHAPE)
                                                    .border(1.5.dp, XiaomiBlue, DAY_SHAPE)

                                                day.isToday -> Modifier.border(
                                                    1.5.dp,
                                                    XiaomiBlue,
                                                    DAY_SHAPE
                                                )

                                                else -> Modifier
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.date.dayOfMonth.toString(),
                                        color = numColor,
                                        fontSize = 15.sp,
                                        modifier = Modifier.graphicsLayer {
                                            val s = lerp(1f, maxNumberScale, fullProgress())
                                            scaleX = s
                                            scaleY = s
                                        }
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .layout { measurable, constraints ->
                                            val h =
                                                (12.dp.toPx() * (1f - fullProgress())).roundToInt()
                                                    .coerceAtLeast(0)
                                            val placeable = measurable.measure(
                                                constraints.copy(
                                                    minHeight = h,
                                                    maxHeight = h
                                                )
                                            )
                                            layout(placeable.width, h) { placeable.place(0, 0) }
                                        }
                                        .alpha(numAlpha)
                                        .graphicsLayer { alpha = (1f - fullProgress()) },
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (eventsByDate[day.date]?.isNotEmpty() == true || birthdaysByDate[day.date.month to day.date.dayOfMonth]?.isNotEmpty() == true) {
                                        Box(
                                            modifier = Modifier
                                                .size(3.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (day.isSelected) Color.White else Color.Gray.copy(
                                                        alpha = 0.5f
                                                    )
                                                )
                                        )
                                    }
                                    if (isHoliday) {
                                        if (eventsByDate[day.date]?.isNotEmpty() == true || birthdaysByDate[day.date.month to day.date.dayOfMonth]?.isNotEmpty() == true) {
                                            Spacer(Modifier.width(2.dp))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(width = 8.dp, height = 2.dp)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(
                                                    if (day.isSelected) Color.White else XiaomiRed.copy(
                                                        alpha = 0.8f
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = sharedColumnModifier) {
                days.chunked(7).forEach { weekDays ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .layout { measurable, constraints ->
                                val h = rowHeightPx().roundToInt().coerceAtLeast(0)
                                val placeable = measurable.measure(
                                    constraints.copy(
                                        minHeight = h,
                                        maxHeight = h
                                    )
                                )
                                layout(placeable.width, h) { placeable.place(0, 0) }
                            }
                    ) {
                        if (settings.showWeekNumber) {
                            WeekNumberItem(weekDays.first().date, settings.weekStart == "sunday", Modifier.fillMaxHeight())
                        }
                        weekDays.forEach { day ->
                            key(day.date) {
                                DayItem(
                                    day = day,
                                    events = eventsByDate[day.date] ?: emptyList(),
                                    birthdays = birthdaysByDate[day.date.month to day.date.dayOfMonth] ?: emptyList(),
                                    holidays = holidaysByDate[day.date] ?: emptyList(),
                                    fullProgress = fullProgress,
                                    modifier = Modifier.weight(1f),
                                    onDayClick = onDayClick,
                                    onDayDetail = onDayDetail,
                                    onEventClick = onEventClick,
                                    settings = settings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayItem(
    day: CalendarDay,
    events: List<CalendarEvent>,
    birthdays: List<ContactBirthday>,
    holidays: List<Holiday>,
    fullProgress: () -> Float,
    modifier: Modifier = Modifier,
    onDayClick: (CalendarDay) -> Unit,
    onDayDetail: (LocalDate) -> Unit = {},
    onEventClick: (CalendarEvent) -> Unit,
    settings: AppSettings
) {
    val isSelected = day.isSelected
    val isToday = day.isToday
    val isWeekend = day.date.dayOfWeek.value >= 6
    val context = androidx.compose.ui.platform.LocalContext.current
    val itemAlpha = if (day.isFromCurrentMonth) 1f else 0.3f
    val dayHols = remember(events, holidays) { (events.filter { it.eventType == EventType.HOLIDAY.value }).distinctBy { it.title } }
    val dayBirthdays = remember(events, birthdays) { (events.filter { it.eventType == EventType.BIRTHDAY.value }).distinctBy { it.title } }
    val dayRegularEvents = remember(events) { events.filter { it.eventType != EventType.HOLIDAY.value && it.eventType != EventType.BIRTHDAY.value } }
    
    val isHoliday = dayHols.isNotEmpty() || holidays.isNotEmpty()
    val hasAnything = dayRegularEvents.isNotEmpty() || dayBirthdays.isNotEmpty() || birthdays.isNotEmpty() || isHoliday
    val density = LocalDensity.current
    val minNumberBoxPx = with(density) { 30.dp.toPx() }
    val maxNumberBoxPx = with(density) { 34.dp.toPx() }
    val maxNumberScale = 16f / 15f
    val noRipple = remember { MutableInteractionSource() }
    val weekFillAlpha = if (isSelected) 1f else 0f

    val textColor = when {
        isSelected -> colorLerp(XiaomiBlue, Color.White, weekFillAlpha)
        isHoliday -> XiaomiRed
        isWeekend -> XiaomiWeekendBlue
        else -> XiaomiTextPrimary
    }

    val numberBoxModifier = Modifier
        .layout { measurable, constraints ->
            val side =
                lerp(minNumberBoxPx, maxNumberBoxPx, fullProgress()).roundToInt().coerceAtLeast(0)
            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = side,
                    maxWidth = side,
                    minHeight = side,
                    maxHeight = side
                )
            )
            layout(side, side) { placeable.place(0, 0) }
        }
        .alpha(itemAlpha)
        .then(
            when {
                isSelected -> Modifier
                    .background(XiaomiBlue.copy(alpha = weekFillAlpha), DAY_SHAPE)
                    .border(1.5.dp, XiaomiBlue, DAY_SHAPE)

                isToday -> Modifier.border(1.5.dp, XiaomiBlue, DAY_SHAPE)
                else -> Modifier
            }
        )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(interactionSource = noRipple, indication = null) {
                onDayClick(day)
            }
            .padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = numberBoxModifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.graphicsLayer {
                    val textScale = lerp(1f, maxNumberScale, fullProgress())
                    scaleX = textScale
                    scaleY = textScale
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(interactionSource = noRipple, indication = null) {
                    onDayClick(day)
                    if (fullProgress() > 0.5f && hasAnything) onDayDetail(day.date)
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val h = (12.dp.toPx() * (1f - fullProgress())).roundToInt().coerceAtLeast(0)
                        val placeable =
                            measurable.measure(constraints.copy(minHeight = h, maxHeight = h))
                        layout(placeable.width, h) { placeable.place(0, 0) }
                    }
                    .graphicsLayer { alpha = itemAlpha * (1f - fullProgress()) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isHoliday) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) XiaomiBlue else XiaomiRed)
                    )
                }
                if (dayBirthdays.isNotEmpty() || birthdays.isNotEmpty()) {
                    if (isHoliday) Spacer(Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) XiaomiBlue else XiaomiEventPinkText)
                    )
                }
                if (dayRegularEvents.isNotEmpty()) {
                    if (isHoliday || dayBirthdays.isNotEmpty() || birthdays.isNotEmpty()) Spacer(
                        Modifier.width(2.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) XiaomiBlue else Color.Gray.copy(alpha = 0.5f))
                    )
                }
            }

            val showDetail by remember {
                derivedStateOf { fullProgress() > 0.01f }
            }
            if (showDetail) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer { alpha = itemAlpha * fullProgress() }
                        .padding(horizontal = 2.dp)
                ) {
                    Column {
                        dayHols.forEach { h ->
                            Surface(
                                color = XiaomiRed.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .padding(vertical = 1.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        if (fullProgress() >= 0.999f) onEventClick(h)
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                ) {
                                    Text(
                                        text = "🌟",
                                        fontSize = 7.sp,
                                        modifier = Modifier.padding(end = 1.dp)
                                    )
                                    Text(
                                        text = h.title,
                                        fontSize = 8.sp,
                                        color = XiaomiRed,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        dayBirthdays.take(1).forEach { b ->
                            Surface(
                                color = XiaomiEventPinkBg,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .padding(vertical = 1.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        if (fullProgress() >= 0.999f) onEventClick(b)
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                ) {
                                    Text(
                                        text = "🎂",
                                        fontSize = 7.sp,
                                        modifier = Modifier.padding(end = 1.dp)
                                    )
                                    Text(
                                        text = b.title,
                                        fontSize = 8.sp,
                                        color = XiaomiEventPinkText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        dayRegularEvents.take(if (dayBirthdays.isEmpty()) 3 else 2).forEach { event ->
                            val chipColor = eventAccentColor(event, settings)
                            val iconStr = when(event.eventType) {
                                EventType.REMINDER.value -> "🔔"
                                EventType.ANNIVERSARY.value -> "❤️"
                                else -> null
                            }
                            Surface(
                                color = chipColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .padding(vertical = 1.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        if (fullProgress() >= 0.999f) onEventClick(event)
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                ) {
                                    if (iconStr != null) {
                                        Text(
                                            text = iconStr,
                                            fontSize = 7.sp,
                                            modifier = Modifier.padding(end = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = event.title,
                                        fontSize = 8.sp,
                                        color = chipColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        val totalItems = dayHols.size + dayBirthdays.size + dayRegularEvents.size
                        if (totalItems > 3) {
                            Text(
                                text = "${trans(R.string.label_more)} ${totalItems - 3}",
                                fontSize = 7.sp,
                                color = if (isSelected) XiaomiBlue else XiaomiTextSecondary,
                                modifier = Modifier.padding(horizontal = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

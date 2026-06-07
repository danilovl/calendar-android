package danilovl.calendar.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import danilovl.calendar.ui.appLocale
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.ui.theme.XiaomiWeekendBlue
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class YearDayCell(
    val dayNumber: Int,
    val row: Int,
    val col: Int,
    val hasEvent: Boolean,
    val isWeekend: Boolean,
    val isToday: Boolean
)

class YearTextCache(
    val normal: Map<Int, TextLayoutResult>,
    val weekend: Map<Int, TextLayoutResult>,
    val today: Map<Int, TextLayoutResult>
)

@Composable
fun YearView(
    selectedDate: LocalDate,
    datesWithEvents: Set<LocalDate>,
    birthdayDays: Set<Pair<Int, Int>> = emptySet(),
    weekStartsSunday: Boolean = false,
    onYearChange: (Int) -> Unit,
    onMonthClick: (LocalDate) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val textCache = remember(textMeasurer) {
        val normalStyle = TextStyle(color = XiaomiTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Normal)
        val weekendStyle = TextStyle(color = XiaomiWeekendBlue, fontSize = 9.sp, fontWeight = FontWeight.Normal)
        val todayStyle = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)

        YearTextCache(
            normal = (1..31).associateWith { textMeasurer.measure(it.toString(), normalStyle) },
            weekend = (1..31).associateWith { textMeasurer.measure(it.toString(), weekendStyle) },
            today = (1..31).associateWith { textMeasurer.measure(it.toString(), todayStyle) }
        )
    }

    val initialPage = 1000
    val pagerState = rememberPagerState(pageCount = { 2000 }, initialPage = initialPage)
    
    val referenceYear = remember { selectedDate.year }
    
    LaunchedEffect(selectedDate.year) {
        val targetPage = initialPage + (selectedDate.year - referenceYear)
        if (targetPage in 0 until 2000) {
            snapshotFlow { pagerState.isScrollInProgress }.filter { !it }.first()
            if (pagerState.currentPage != targetPage) {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            onYearChange(referenceYear + (page - initialPage))
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1
    ) { page ->
        val year = referenceYear + (page - initialPage)
        YearPage(year, datesWithEvents, birthdayDays, weekStartsSunday, textCache, onMonthClick)
    }
}

@Composable
fun YearPage(
    year: Int,
    datesWithEvents: Set<LocalDate>,
    birthdayDays: Set<Pair<Int, Int>>,
    weekStartsSunday: Boolean,
    textCache: YearTextCache,
    onMonthClick: (LocalDate) -> Unit
) {
    val yearEvents = remember(datesWithEvents, year) {
        val filtered = mutableMapOf<Int, MutableSet<Int>>()
        datesWithEvents.forEach { date ->
            if (date.year == year) {
                filtered.getOrPut(date.monthValue) { mutableSetOf() }.add(date.dayOfMonth)
            }
        }
        filtered
    }
    val yearBirthdays = remember(birthdayDays) {
        val filtered = mutableMapOf<Int, MutableSet<Int>>()
        birthdayDays.forEach { (month, day) ->
            filtered.getOrPut(month) { mutableSetOf() }.add(day)
        }
        filtered
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        for (row in 0..3) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (col in 0..2) {
                    val monthIndex = row * 3 + col
                    val month = monthIndex + 1
                    val monthDate = remember(year, monthIndex) { LocalDate.of(year, month, 1) }
                    val monthEvents = yearEvents[month] ?: emptySet()
                    val monthBirthdays = yearBirthdays[month] ?: emptySet()

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        YearMonthItem(
                            monthDate,
                            monthEvents,
                            monthBirthdays,
                            weekStartsSunday,
                            textCache,
                            onMonthClick
                        )
                    }
                }
            }
            if (row < 3) Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun YearMonthItem(
    monthDate: LocalDate,
    monthEventDays: Set<Int>,
    monthBirthdayDays: Set<Int>,
    weekStartsSunday: Boolean,
    textCache: YearTextCache,
    onMonthClick: (LocalDate) -> Unit
) {
    val locale = appLocale()
    val monthName = remember(monthDate) {
        monthDate.format(DateTimeFormatter.ofPattern("LLLL", locale))
            .replaceFirstChar { it.uppercase() }
    }
    
    val daysInMonth = remember(monthDate) { monthDate.lengthOfMonth() }
    val firstDayOfMonth = remember(monthDate) { monthDate.dayOfWeek.value }
    val year = monthDate.year
    val month = monthDate.monthValue
    val today = remember { LocalDate.now() }
    val leading = remember(firstDayOfMonth, weekStartsSunday) {
        if (weekStartsSunday) firstDayOfMonth % 7 else firstDayOfMonth - 1
    }

    val todayDay = remember(today, year, month) {
        if (today.year == year && today.monthValue == month) today.dayOfMonth else -1
    }

    val hasEventByDay = remember(daysInMonth, monthEventDays, monthBirthdayDays) {
        BooleanArray(daysInMonth + 1).also { arr ->
            for (day in 1..daysInMonth) {
                arr[day] = day in monthEventDays || day in monthBirthdayDays
            }
        }
    }
    val isWeekendByDay = remember(daysInMonth, firstDayOfMonth) {
        BooleanArray(daysInMonth + 1).also { arr ->
            for (day in 1..daysInMonth) {
                val dayOfWeek = ((firstDayOfMonth + day - 2) % 7) + 1
                arr[day] = dayOfWeek >= 6
            }
        }
    }
    val dayCells = remember(daysInMonth, leading, hasEventByDay, isWeekendByDay, todayDay) {
        List(daysInMonth) { index ->
            val day = index + 1
            val slot = leading + index
            YearDayCell(
                dayNumber = day,
                row = slot / 7,
                col = slot % 7,
                hasEvent = hasEventByDay[day],
                isWeekend = isWeekendByDay[day],
                isToday = day == todayDay
            )
        }
    }

    val density = LocalDensity.current
    val dotRadius = remember(density) { with(density) { 1.2.dp.toPx() } }
    val dotBottomPadding = remember(density) { with(density) { 1.dp.toPx() } }
    
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clickable { onMonthClick(monthDate) }
            .padding(2.dp)
    ) {
        Text(
            text = monthName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = XiaomiTextPrimary,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val cellWidth = this.size.width / 7
            val cellHeight = this.size.height / 6

            for (cell in dayCells) {
                val x = cell.col * cellWidth
                val y = cell.row * cellHeight
                val centerX = x + cellWidth / 2
                val centerY = y + cellHeight / 2

                if (cell.isToday) {
                    val s = minOf(cellWidth, cellHeight) * 0.9f
                    drawRoundRect(
                        color = XiaomiBlue,
                        topLeft = Offset(centerX - s / 2f, centerY - s / 2f),
                        size = androidx.compose.ui.geometry.Size(s, s),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.25f, s * 0.25f)
                    )
                }

                val textLayoutResult = when {
                    cell.isToday -> textCache.today[cell.dayNumber]
                    cell.isWeekend -> textCache.weekend[cell.dayNumber]
                    else -> textCache.normal[cell.dayNumber]
                }

                if (textLayoutResult != null) {
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x + (cellWidth - textLayoutResult.size.width) / 2,
                            y + (cellHeight - textLayoutResult.size.height) / 2
                        )
                    )
                }

                if (cell.hasEvent) {
                    drawCircle(
                        color = if (cell.isToday) Color.White else XiaomiBlue,
                        radius = dotRadius,
                        center = Offset(
                            centerX,
                            y + cellHeight - dotRadius - dotBottomPadding
                        )
                    )
                }
            }
        }
    }
}

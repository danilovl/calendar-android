package danilovl.calendar.service

import danilovl.calendar.data.CalendarDay
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.util.RegexPatterns
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private val INTERVAL_REGEX = RegexPatterns.LEAD_NUMBER

fun generateDaysForMonth(
    yearMonth: YearMonth,
    highlightedDate: LocalDate?,
    weekStartsSunday: Boolean = false
): List<CalendarDay> {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val dayOfWeekOfFirstDay = firstDayOfMonth.dayOfWeek.value
    val leading = if (weekStartsSunday) dayOfWeekOfFirstDay % 7 else dayOfWeekOfFirstDay - 1

    val days = mutableListOf<CalendarDay>()
    val prevMonth = yearMonth.minusMonths(1)
    val daysInPrevMonth = prevMonth.lengthOfMonth()
    val today = LocalDate.now()
    
    for (i in leading downTo 1) {
        val date = prevMonth.atDay(daysInPrevMonth - i + 1)
        days.add(CalendarDay(date, false, isSelected = date == highlightedDate, isToday = date == today))
    }
    
    for (i in 1..daysInMonth) {
        val date = yearMonth.atDay(i)
        days.add(
            CalendarDay(
                date = date,
                isFromCurrentMonth = true,
                isSelected = date == highlightedDate,
                isToday = date == today
            )
        )
    }
    
    val nextMonth = yearMonth.plusMonths(1)
    val remainingDays = 42 - days.size
    for (i in 1..remainingDays) {
        val date = nextMonth.atDay(i)
        days.add(CalendarDay(date, false, isSelected = date == highlightedDate, isToday = date == today))
    }
    
    return days
}

fun isEventOnDate(event: CalendarEvent, date: LocalDate): Boolean {
    if (event.excludedDates.split(",").contains(date.toString())) return false
    if (event.date == date) return true
    if (event.repeat == "none") return false

    if (event.repeatUntil != null && date.isAfter(event.repeatUntil)) return false

    val startDate = event.date
    if (date.isBefore(startDate)) return false

    val repeatStr = event.repeat
    val interval = INTERVAL_REGEX.find(repeatStr)?.value?.toIntOrNull() ?: 1

    return when {
        repeatStr.contains("daily") || repeatStr.contains("day") -> {
            val daysBetween = ChronoUnit.DAYS.between(startDate, date)
            daysBetween % interval == 0L
        }
        repeatStr.contains("weekly") || repeatStr.contains("week") -> {
            val weeksBetween = ChronoUnit.WEEKS.between(startDate, date)
            val isCorrectWeek = weeksBetween % interval == 0L
            isCorrectWeek && startDate.dayOfWeek == date.dayOfWeek
        }
        repeatStr.contains("monthly") || repeatStr.contains("month") -> {
            val monthsBetween = ChronoUnit.MONTHS.between(startDate, date)
            val isCorrectMonth = monthsBetween % interval == 0L
            isCorrectMonth && dayOfMonthMatches(startDate, date)
        }
        repeatStr.contains("yearly") || repeatStr.contains("year") -> {
            val yearsBetween = ChronoUnit.YEARS.between(startDate, date)
            val isCorrectYear = yearsBetween % interval == 0L
            isCorrectYear && startDate.month == date.month && dayOfMonthMatches(startDate, date)
        }
        else -> false
    }
}

private fun dayOfMonthMatches(startDate: LocalDate, date: LocalDate): Boolean {
    if (startDate.dayOfMonth == date.dayOfMonth) {
        return true
    }

    val lastDayOfTargetMonth = date.lengthOfMonth()

    return startDate.dayOfMonth > lastDayOfTargetMonth && date.dayOfMonth == lastDayOfTargetMonth
}

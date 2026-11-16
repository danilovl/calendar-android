package danilovl.calendar.service

import danilovl.calendar.data.CalendarDay
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.util.RegexPatterns
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private val INTERVAL_REGEX = RegexPatterns.LEAD_NUMBER

/** Upper bound for a multi-day event span, protects against corrupted endDate values. */
private const val MAX_SPAN_DAYS = 366L

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

enum class RepeatUnit { DAY, WEEK, MONTH, YEAR }

data class RepeatRule(val unit: RepeatUnit, val interval: Int)

/**
 * Parses the stored repeat string: "none", "daily", "weekly", "monthly", "yearly"
 * or custom "N_day" / "N_week" / "N_month" / "N_year".
 */
fun parseRepeat(repeat: String): RepeatRule? {
    if (repeat.isBlank() || repeat == "none") return null
    val interval = (INTERVAL_REGEX.find(repeat)?.value?.toIntOrNull() ?: 1).coerceAtLeast(1)
    val unit = when {
        repeat.contains("year") -> RepeatUnit.YEAR
        repeat.contains("month") -> RepeatUnit.MONTH
        repeat.contains("week") -> RepeatUnit.WEEK
        repeat.contains("da") -> RepeatUnit.DAY // "daily" / "_day"
        else -> return null
    }
    return RepeatRule(unit, interval)
}

fun formatRepeat(rule: RepeatRule?): String {
    if (rule == null) return "none"
    if (rule.interval == 1) {
        return when (rule.unit) {
            RepeatUnit.DAY -> "daily"
            RepeatUnit.WEEK -> "weekly"
            RepeatUnit.MONTH -> "monthly"
            RepeatUnit.YEAR -> "yearly"
        }
    }
    val suffix = when (rule.unit) {
        RepeatUnit.DAY -> "_day"
        RepeatUnit.WEEK -> "_week"
        RepeatUnit.MONTH -> "_month"
        RepeatUnit.YEAR -> "_year"
    }
    return "${rule.interval}$suffix"
}

fun CalendarEvent.excludedDateSet(): Set<String> =
    excludedDates.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

/** Number of extra days an event lasts after its start day (0 for single-day events). */
fun CalendarEvent.spanDays(): Long {
    val end = endDate ?: return 0
    return ChronoUnit.DAYS.between(date, end).coerceIn(0, MAX_SPAN_DAYS)
}

/** k-th occurrence start of a recurring event (k = 0 is the event date). */
private fun occurrenceAt(start: LocalDate, rule: RepeatRule, k: Long): LocalDate = when (rule.unit) {
    RepeatUnit.DAY -> start.plusDays(k * rule.interval)
    RepeatUnit.WEEK -> start.plusWeeks(k * rule.interval)
    // plusMonths/plusYears clamp the day to the last day of a shorter month (31 -> 30/28/29)
    RepeatUnit.MONTH -> start.plusMonths(k * rule.interval)
    RepeatUnit.YEAR -> start.plusYears(k * rule.interval)
}

/** Index of the first occurrence that could start on or after [from]. */
private fun firstIndexFrom(start: LocalDate, rule: RepeatRule, from: LocalDate): Long {
    if (!from.isAfter(start)) return 0
    val units = when (rule.unit) {
        RepeatUnit.DAY -> ChronoUnit.DAYS.between(start, from)
        RepeatUnit.WEEK -> ChronoUnit.DAYS.between(start, from) / 7
        RepeatUnit.MONTH -> (from.year * 12L + from.monthValue) - (start.year * 12L + start.monthValue)
        RepeatUnit.YEAR -> (from.year - start.year).toLong()
    }
    return (units / rule.interval - 1).coerceAtLeast(0)
}

/** True if an occurrence of [event] *starts* on [date] (ignores multi-day span). */
fun startsOn(event: CalendarEvent, date: LocalDate): Boolean {
    if (date.isBefore(event.date)) return false
    val rule = parseRepeat(event.repeat)
    val matches = if (rule == null) {
        date == event.date
    } else {
        if (event.repeatUntil != null && date.isAfter(event.repeatUntil)) return false
        var k = firstIndexFrom(event.date, rule, date)
        var result = false
        while (true) {
            val occ = occurrenceAt(event.date, rule, k)
            if (occ == date) { result = true; break }
            if (occ.isAfter(date)) break
            k++
        }
        result
    }
    return matches && date.toString() !in event.excludedDateSet()
}

fun isEventOnDate(event: CalendarEvent, date: LocalDate): Boolean {
    val span = event.spanDays()
    for (offset in 0..span) {
        if (startsOn(event, date.minusDays(offset))) return true
    }
    return false
}

/** First occurrence start on or after [from], or null if the series has ended. */
fun nextOccurrence(event: CalendarEvent, from: LocalDate): LocalDate? {
    val rule = parseRepeat(event.repeat)
    if (rule == null) {
        return event.date.takeIf { !it.isBefore(from) && it.toString() !in event.excludedDateSet() }
    }
    val excluded = event.excludedDateSet()
    var k = firstIndexFrom(event.date, rule, from)
    repeat(100_000) {
        val occ = occurrenceAt(event.date, rule, k)
        if (event.repeatUntil != null && occ.isAfter(event.repeatUntil)) return null
        if (!occ.isBefore(from) && occ.toString() !in excluded) return occ
        k++
    }
    return null
}

/** All occurrence start dates of [event] that touch the inclusive range [start]..[end]. */
fun occurrencesBetween(event: CalendarEvent, start: LocalDate, end: LocalDate): List<LocalDate> {
    val from = start.minusDays(event.spanDays())
    val excluded = event.excludedDateSet()
    val rule = parseRepeat(event.repeat)
    if (rule == null) {
        val d = event.date
        val inRange = !d.isBefore(from) && !d.isAfter(end) && d.toString() !in excluded
        return if (inRange) listOf(d) else emptyList()
    }
    // Rule and exclusions are parsed once per event, not once per date.
    val last = event.repeatUntil?.let { minOf(it, end) } ?: end
    val result = mutableListOf<LocalDate>()
    var k = firstIndexFrom(event.date, rule, from)
    while (true) {
        val occ = occurrenceAt(event.date, rule, k)
        if (occ.isAfter(last)) break
        if (!occ.isBefore(from) && occ.toString() !in excluded) result.add(occ)
        k++
    }
    return result
}

/**
 * Groups events by every day they cover in [start]..[end], expanding recurring and multi-day
 * events. Keeps the input order of events inside each day. Cost is proportional to the number
 * of occurrences, not days x events — intended to be run off the main thread.
 */
fun groupEventsByDate(events: List<CalendarEvent>, start: LocalDate, end: LocalDate): Map<LocalDate, List<CalendarEvent>> {
    val result = HashMap<LocalDate, MutableList<CalendarEvent>>()
    for (event in events) {
        val span = event.spanDays()
        for (occ in occurrencesBetween(event, start, end)) {
            for (d in 0..span) {
                val day = occ.plusDays(d)
                if (day.isBefore(start) || day.isAfter(end)) continue
                val dayEvents = result.getOrPut(day) { mutableListOf() }
                // Overlapping occurrences of one series (span longer than the interval) count once.
                if (dayEvents.lastOrNull() !== event) dayEvents.add(event)
            }
        }
    }
    return result
}

/** Reminder lead time in minutes for labels like "15_min", "1_hour", "1_day", "1_week", "none". */
fun leadMinutes(offset: String): Long {
    val num = INTERVAL_REGEX.find(offset)?.value?.toLongOrNull() ?: 1L

    return when {
        offset.contains("week") -> num * 7 * 24 * 60
        offset.contains("day") -> num * 24 * 60
        offset.contains("hour") -> num * 60
        offset.contains("min") -> num
        else -> 0L
    }
}

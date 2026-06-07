package danilovl.calendar.data

import java.time.LocalDate

data class CalendarDay(
    val date: LocalDate,
    val isFromCurrentMonth: Boolean,
    val isSelected: Boolean = false,
    val isToday: Boolean = false
)

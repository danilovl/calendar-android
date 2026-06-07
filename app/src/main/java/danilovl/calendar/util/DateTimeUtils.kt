package danilovl.calendar.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    private fun dateFormatter() = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private fun longDateFormatter() = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun formatDateLong(date: LocalDate): String {
        return date.format(longDateFormatter())
    }

    fun formatDateTimeLabel(date: LocalDate, time: LocalTime, allDay: Boolean): String {
        val datePart = date.format(dateFormatter())
        return if (allDay) datePart else "$datePart  ${time.format(timeFormatter)}"
    }

    fun startOfWeek(date: LocalDate, sunday: Boolean): LocalDate {
        val dow = date.dayOfWeek.value
        val back = if (sunday) dow % 7 else dow - 1
        return date.minusDays(back.toLong())
    }

    fun columnIndex(date: LocalDate, sunday: Boolean): Int {
        val dow = date.dayOfWeek.value
        return if (sunday) dow % 7 else dow - 1
    }
}

package danilovl.calendar.data.remote

import danilovl.calendar.data.repository.Holiday
import java.time.LocalDate

internal data class HolidayDto(val date: String?, val localName: String?, val name: String?) {
    fun toHoliday(year: Int): Holiday? {
        val parsedDate = date?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        } ?: return null

        val displayName = localName?.takeIf { it.isNotBlank() }
            ?: name?.takeIf { it.isNotBlank() }
            ?: return null

        return Holiday(
            date = LocalDate.of(year, parsedDate.monthValue, parsedDate.dayOfMonth),
            name = displayName
        )
    }
}

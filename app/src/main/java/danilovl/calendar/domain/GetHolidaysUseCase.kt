package danilovl.calendar.domain

import danilovl.calendar.data.repository.Holiday
import danilovl.calendar.data.repository.HolidayRepository

class GetHolidaysUseCase(private val holidayRepository: HolidayRepository) {
    suspend operator fun invoke(year: Int, country: String): List<Holiday> {
        return holidayRepository.getHolidays(year, country)
    }
}

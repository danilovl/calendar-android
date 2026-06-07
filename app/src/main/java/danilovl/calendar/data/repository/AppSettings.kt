package danilovl.calendar.data.repository

data class AppSettings(
    val weekStart: String = "monday",
    val expandedMonth: Boolean = false,
    val showWeekNumber: Boolean = false,
    val chineseAlmanac: Boolean = false,
    val internationalHolidays: Boolean = true,
    val holidayCountry: String = "RU",
    val additionalHolidayCountries: List<String> = emptyList(),
    val otherCalendar: String = "none",
    val defaultReminder: String = "1_day",
    val allDayReminderTime: String = "08:00",
    val reminderMelody: String = "default",
    val importContactBirthdays: Boolean = false,
    val eventTimeZone: String = "GMT+03:00",
    val widgetRangeDays: Int = 14,
    val language: String = "system",
    val eventColor: Int = 0xFF1677FF.toInt(),
    val birthdayColor: Int = 0xFFFF4D4D.toInt(),
    val anniversaryColor: Int = 0xFF9C27B0.toInt(),
    val reminderColor: Int = 0xFFFF9800.toInt(),
    val enabledCalendars: Set<Long> = emptySet(),
    val lastBirthdaysSync: Long = 0L
)


package danilovl.calendar.data.repository

import android.content.Context
import android.content.SharedPreferences
import danilovl.calendar.data.remote.HolidayCountries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("calendar_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readFromPrefs())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun getSettings(): AppSettings = readFromPrefs()

    private fun readFromPrefs(): AppSettings {
        val defaults = AppSettings()

        val additionalCountries = prefs.getString(SettingsKeys.KEY_ADDITIONAL_HOLIDAY_COUNTRIES, "")!!
            .split(",")
            .filter { it.isNotEmpty() }
            .map { HolidayCountries.normalizeCode(it) }

        val enabledCalendars = prefs.getString(SettingsKeys.KEY_ENABLED_CALENDARS, "")!!
            .split(",")
            .filter { it.isNotEmpty() }
            .map { it.toLong() }
            .toSet()

        return AppSettings(
            weekStart = prefs.getString(SettingsKeys.KEY_WEEK_START, defaults.weekStart)!!,
            expandedMonth = prefs.getBoolean(SettingsKeys.KEY_EXPANDED_MONTH, defaults.expandedMonth),
            showWeekNumber = prefs.getBoolean(SettingsKeys.KEY_SHOW_WEEK_NUMBER, defaults.showWeekNumber),
            chineseAlmanac = prefs.getBoolean(SettingsKeys.KEY_CHINESE_ALMANAC, defaults.chineseAlmanac),
            internationalHolidays = prefs.getBoolean(SettingsKeys.KEY_INTL_HOLIDAYS, defaults.internationalHolidays),
            holidayCountry = HolidayCountries.normalizeCode(prefs.getString(SettingsKeys.KEY_HOLIDAY_COUNTRY, defaults.holidayCountry)!!),
            additionalHolidayCountries = additionalCountries,
            otherCalendar = prefs.getString(SettingsKeys.KEY_OTHER_CALENDAR, defaults.otherCalendar)!!,
            defaultReminder = prefs.getString(SettingsKeys.KEY_DEFAULT_REMINDER, defaults.defaultReminder)!!,
            allDayReminderTime = prefs.getString(SettingsKeys.KEY_ALL_DAY_REMINDER, defaults.allDayReminderTime)!!,
            reminderMelody = prefs.getString(SettingsKeys.KEY_REMINDER_MELODY, defaults.reminderMelody)!!,
            importContactBirthdays = prefs.getBoolean(SettingsKeys.KEY_IMPORT_BIRTHDAYS, defaults.importContactBirthdays),
            eventTimeZone = prefs.getString(SettingsKeys.KEY_EVENT_TIMEZONE, defaults.eventTimeZone)!!,
            widgetRangeDays = prefs.getInt(SettingsKeys.KEY_WIDGET_RANGE, defaults.widgetRangeDays),
            language = prefs.getString(SettingsKeys.KEY_LANGUAGE, defaults.language)!!,
            eventColor = prefs.getInt(SettingsKeys.KEY_COLOR_EVENT, defaults.eventColor),
            birthdayColor = prefs.getInt(SettingsKeys.KEY_COLOR_BIRTHDAY, defaults.birthdayColor),
            anniversaryColor = prefs.getInt(SettingsKeys.KEY_COLOR_ANNIVERSARY, defaults.anniversaryColor),
            reminderColor = prefs.getInt(SettingsKeys.KEY_COLOR_REMINDER, defaults.reminderColor),
            enabledCalendars = enabledCalendars,
            lastBirthdaysSync = prefs.getLong(SettingsKeys.KEY_LAST_BIRTHDAYS_SYNC, defaults.lastBirthdaysSync)
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        val editor = prefs.edit()
        editor.putString(SettingsKeys.KEY_WEEK_START, updated.weekStart)
        editor.putBoolean(SettingsKeys.KEY_EXPANDED_MONTH, updated.expandedMonth)
        editor.putBoolean(SettingsKeys.KEY_SHOW_WEEK_NUMBER, updated.showWeekNumber)
        editor.putBoolean(SettingsKeys.KEY_CHINESE_ALMANAC, updated.chineseAlmanac)
        editor.putBoolean(SettingsKeys.KEY_INTL_HOLIDAYS, updated.internationalHolidays)
        editor.putString(SettingsKeys.KEY_HOLIDAY_COUNTRY, updated.holidayCountry)
        editor.putString(SettingsKeys.KEY_ADDITIONAL_HOLIDAY_COUNTRIES, updated.additionalHolidayCountries.joinToString(","))
        editor.putString(SettingsKeys.KEY_OTHER_CALENDAR, updated.otherCalendar)
        editor.putString(SettingsKeys.KEY_DEFAULT_REMINDER, updated.defaultReminder)
        editor.putString(SettingsKeys.KEY_ALL_DAY_REMINDER, updated.allDayReminderTime)
        editor.putString(SettingsKeys.KEY_REMINDER_MELODY, updated.reminderMelody)
        editor.putBoolean(SettingsKeys.KEY_IMPORT_BIRTHDAYS, updated.importContactBirthdays)
        editor.putString(SettingsKeys.KEY_EVENT_TIMEZONE, updated.eventTimeZone)
        editor.putInt(SettingsKeys.KEY_WIDGET_RANGE, updated.widgetRangeDays)
        editor.putString(SettingsKeys.KEY_LANGUAGE, updated.language)
        editor.putInt(SettingsKeys.KEY_COLOR_EVENT, updated.eventColor)
        editor.putInt(SettingsKeys.KEY_COLOR_BIRTHDAY, updated.birthdayColor)
        editor.putInt(SettingsKeys.KEY_COLOR_ANNIVERSARY, updated.anniversaryColor)
        editor.putInt(SettingsKeys.KEY_COLOR_REMINDER, updated.reminderColor)
        editor.putString(SettingsKeys.KEY_ENABLED_CALENDARS, updated.enabledCalendars.joinToString(","))
        editor.putLong(SettingsKeys.KEY_LAST_BIRTHDAYS_SYNC, updated.lastBirthdaysSync)
        editor.commit()
        _settings.value = updated
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context).also { INSTANCE = it }
            }
        }
    }
}

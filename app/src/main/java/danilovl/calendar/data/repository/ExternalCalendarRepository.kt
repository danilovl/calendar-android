package danilovl.calendar.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import danilovl.calendar.data.EventType
import danilovl.calendar.data.ExternalCalendar
import danilovl.calendar.data.local.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ExternalCalendarRepository(private val context: Context) {

    suspend fun getCalendars(): List<ExternalCalendar> = withContext(Dispatchers.IO) {
        val calendars = mutableListOf<ExternalCalendar>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )

        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(CalendarContract.Calendars._ID)
            val nameIndex = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIndex = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
            val colorIndex = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)

            while (it.moveToNext()) {
                calendars.add(
                    ExternalCalendar(
                        id = it.getLong(idIndex),
                        name = it.getString(nameIndex) ?: "Unknown",
                        accountName = it.getString(accountIndex) ?: "Unknown",
                        color = it.getInt(colorIndex)
                    )
                )
            }
        }
        calendars
    }

    suspend fun getEvents(calendarIds: Set<Long>, start: LocalDate, end: LocalDate): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (calendarIds.isEmpty()) return@withContext emptyList()

        val events = mutableListOf<CalendarEvent>()
        val startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_COLOR,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.EVENT_LOCATION
        )

        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${calendarIds.joinToString(",")})"

        val cursor = context.contentResolver.query(
            builder.build(),
            projection,
            selection,
            null,
            null
        )

        cursor?.use {
            val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
            val descIndex = it.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
            val beginIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
            val allDayIndex = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            val eventColorIndex = it.getColumnIndex(CalendarContract.Instances.EVENT_COLOR)
            val calColorIndex = it.getColumnIndex(CalendarContract.Instances.CALENDAR_COLOR)
            val locationIndex = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

            val eventIdIndex = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            while (it.moveToNext()) {
                val begin = it.getLong(beginIndex)
                val date = Instant.ofEpochMilli(begin).atZone(ZoneId.systemDefault()).toLocalDate()
                val isAllDay = it.getInt(allDayIndex) != 0
                val color = if (it.getInt(eventColorIndex) != 0) it.getInt(eventColorIndex) else it.getInt(calColorIndex)

                events.add(
                    CalendarEvent(
                        id = it.getLong(eventIdIndex)
                            .toInt() + 1000000,
                        title = it.getString(titleIndex) ?: "(No title)",
                        description = it.getString(descIndex) ?: "",
                        date = date,
                        isAllDay = isAllDay,
                        eventType = EventType.EXTERNAL.value,
                        color = color,
                        location = it.getString(locationIndex) ?: ""
                    )
                )
            }
        }
        events
    }
}

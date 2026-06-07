package danilovl.calendar.util

import android.content.Context
import android.content.Intent
import danilovl.calendar.R
import danilovl.calendar.data.ReminderScheduler
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.repository.AppSettings
import java.time.LocalDate
import java.time.LocalTime

object EventUtils {
    fun scheduleReminder(
        context: Context,
        title: String,
        date: LocalDate,
        startTime: LocalTime?,
        isAllDay: Boolean,
        offsetLabel: String,
        settings: AppSettings,
        eventId: Int? = null,
        eventType: String = "default"
    ) {
        val allDayTime = try {
            LocalTime.parse(settings.allDayReminderTime)
        } catch (e: Exception) {
            LocalTime.of(8, 0)
        }
        ReminderScheduler.scheduleEvent(
            context = context,
            title = title,
            date = date,
            startTime = startTime,
            isAllDay = isAllDay,
            offsetLabel = offsetLabel,
            allDayTime = allDayTime,
            eventId = eventId,
            eventType = eventType
        )
    }

    fun shareEvents(context: Context, events: List<CalendarEvent>) {
        if (events.isEmpty()) return
        val text = events.joinToString("\n\n") { e ->
            "${e.date}: ${e.title}${if (e.description.isNotEmpty()) "\n${e.description}" else ""}"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
    }
}

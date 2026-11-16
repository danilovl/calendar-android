package danilovl.calendar.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import danilovl.calendar.MainActivity
import danilovl.calendar.R
import danilovl.calendar.data.local.CalendarDatabase
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.repository.AppSettings
import danilovl.calendar.data.repository.SettingsRepository
import danilovl.calendar.service.leadMinutes
import danilovl.calendar.service.nextOccurrence
import danilovl.calendar.util.AppLog
import danilovl.calendar.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

const val REMINDER_CHANNEL_ID = "event_reminders"

private const val TAG = "ReminderScheduler"
private const val EXTRA_EVENT_ID = "eventId"
private const val EXTRA_TITLE = "title"
private const val EXTRA_TEXT = "text"
private const val EXTRA_TYPE = "eventType"
private const val EXTRA_DATE = "date"

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getIntExtra(EXTRA_EVENT_ID, -1)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: context.getString(R.string.type_event)
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        val eventType = intent.getStringExtra(EXTRA_TYPE)
        val dateStr = intent.getStringExtra(EXTRA_DATE)

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (eventId > 0) {
                putExtra("EVENT_ID", eventId)
                putExtra("EVENT_DATE", dateStr)
            } else {
                putExtra("EVENT_TITLE", title)
                putExtra("EVENT_TYPE", eventType)
                putExtra("EVENT_DATE", dateStr)
            }
        }

        val notificationId = if (eventId > 0) eventId else (title + dateStr).hashCode()
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        ReminderScheduler.ensureChannel(context)
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            AppLog.w(TAG, "Notification permission not granted, reminder '$title' not shown", e)
        }

        // Plan the next occurrence of a recurring event.
        if (eventId > 0) {
            val pending = goAsync()
            val appContext = context.applicationContext
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val event = CalendarDatabase.getDatabase(appContext).eventDao().getEventById(eventId)
                    if (event != null && event.reminder) {
                        val settings = SettingsRepository.getInstance(appContext).settings.value
                        // Start after the occurrence that just fired.
                        val fired = dateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        ReminderScheduler.schedule(appContext, event, settings, fired?.plusDays(1))
                    }
                } catch (e: Exception) {
                    AppLog.e(TAG, "Failed to reschedule reminder for event $eventId", e)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}

object ReminderScheduler {

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(REMINDER_CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    context.getString(R.string.settings_reminder_item),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    private fun allDayTime(settings: AppSettings): LocalTime =
        runCatching { LocalTime.parse(settings.allDayReminderTime) }.getOrDefault(LocalTime.of(8, 0))

    /**
     * Computes the next reminder trigger for [event] (first occurrence on/after [from] whose
     * trigger time is still in the future). Returns the occurrence date and trigger time.
     */
    fun nextTrigger(
        event: CalendarEvent,
        settings: AppSettings,
        from: LocalDate? = null,
        now: LocalDateTime = LocalDateTime.now()
    ): Pair<LocalDate, LocalDateTime>? {
        val offset = event.reminderOffset.ifBlank { settings.defaultReminder }
        val lead = leadMinutes(offset)
        val time = if (event.isAllDay || event.startTime == null) allDayTime(settings) else event.startTime
        var searchFrom = maxOf(from ?: now.toLocalDate(), now.toLocalDate())
        repeat(1000) {
            val occ = nextOccurrence(event, searchFrom) ?: return null
            val trigger = occ.atTime(time).minusMinutes(lead)
            if (trigger.isAfter(now)) return occ to trigger
            searchFrom = occ.plusDays(1)
        }
        return null
    }

    /** Schedules (or replaces) the alarm for the next occurrence of [event]. Returns trigger millis. */
    fun schedule(context: Context, event: CalendarEvent, settings: AppSettings, from: LocalDate? = null): Long? {
        if (event.id <= 0) return null
        cancel(context, event.id)
        if (!event.reminder) return null

        val (occurrence, trigger) = nextTrigger(event, settings, from) ?: return null
        val millis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        ensureChannel(context)
        val text = if (event.isAllDay || event.startTime == null) {
            context.getString(R.string.common_all_day)
        } else {
            event.startTime.format(DateTimeUtils.timeFormatter)
        }
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_EVENT_ID, event.id)
            putExtra(EXTRA_TITLE, event.title)
            putExtra(EXTRA_TEXT, text)
            putExtra(EXTRA_TYPE, event.eventType)
            putExtra(EXTRA_DATE, occurrence.toString())
        }
        val pi = PendingIntent.getBroadcast(
            context,
            event.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(AlarmManager::class.java) ?: return null
        try {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            }
        } catch (e: SecurityException) {
            AppLog.w(TAG, "Exact alarm denied, falling back to inexact", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
        return millis
    }

    fun cancel(context: Context, eventId: Int) {
        if (eventId <= 0) return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            eventId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            context.getSystemService(AlarmManager::class.java)?.cancel(pi)
            pi.cancel()
        }
    }

    /** Alarms created by older app versions used (title|date).hashCode() as request code. */
    private fun cancelLegacy(context: Context, event: CalendarEvent) {
        val code = (event.title + "|" + event.date).hashCode()
        val pi = PendingIntent.getBroadcast(
            context,
            code,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            context.getSystemService(AlarmManager::class.java)?.cancel(pi)
            pi.cancel()
        }
    }

    /**
     * Re-plans alarms for every event with a reminder. Alarms do not survive reboot,
     * so this runs on boot, app update, time/zone change, app start and data import.
     */
    suspend fun rescheduleAll(context: Context) {
        val appContext = context.applicationContext
        try {
            val settings = SettingsRepository.getInstance(appContext).settings.value
            val events = CalendarDatabase.getDatabase(appContext).eventDao().getEventsWithReminder()
            events.forEach {
                cancelLegacy(appContext, it)
                schedule(appContext, it, settings)
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to reschedule reminders", e)
        }
    }
}

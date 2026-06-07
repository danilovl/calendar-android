package danilovl.calendar.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import danilovl.calendar.MainActivity
import danilovl.calendar.R
import danilovl.calendar.util.DateTimeUtils
import danilovl.calendar.util.RegexPatterns
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

const val REMINDER_CHANNEL_ID = "event_reminders"

private val LEAD_NUMBER_REGEX = RegexPatterns.LEAD_NUMBER
private val NOTIFICATION_TIME_FORMAT = DateTimeUtils.timeFormatter

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", 0)
        val eventId = intent.getIntExtra("eventId", -1)
        val title = intent.getStringExtra("title") ?: context.getString(R.string.type_event)
        val text = intent.getStringExtra("text").orEmpty()
        val eventType = intent.getStringExtra("eventType")
        val dateStr = intent.getStringExtra("date")

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (eventId != -1) {
                putExtra("EVENT_ID", eventId)
            } else {
                putExtra("EVENT_TITLE", title)
                putExtra("EVENT_TYPE", eventType)
                putExtra("EVENT_DATE", dateStr)
            }
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            id,
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
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
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

    fun requestCode(title: String, date: LocalDate): Int = (title + "|" + date).hashCode()

    fun leadMinutes(offset: String): Long {
        val num = LEAD_NUMBER_REGEX.find(offset)?.value?.toLongOrNull() ?: 1L

        return when {
            offset.contains("week") -> num * 7 * 24 * 60
            offset.contains("day") -> num * 24 * 60
            offset.contains("hour") -> num * 60
            offset.contains("min") -> num
            else -> 0L
        }
    }

    fun scheduleEvent(
        context: Context,
        title: String,
        date: LocalDate,
        startTime: LocalTime?,
        isAllDay: Boolean,
        offsetLabel: String,
        allDayTime: LocalTime,
        eventId: Int? = null,
        eventType: String = "default"
    ): Long? {
        val base = if (isAllDay || startTime == null) date.atTime(allDayTime) else date.atTime(startTime)
        val trigger = base.minusMinutes(leadMinutes(offsetLabel))
        val millis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (millis <= System.currentTimeMillis()) return null

        ensureChannel(context)
        val text = if (isAllDay || startTime == null) context.getString(R.string.common_all_day) else startTime.format(NOTIFICATION_TIME_FORMAT)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", requestCode(title, date))
            putExtra("title", title)
            putExtra("text", text)
            putExtra("eventId", eventId ?: -1)
            putExtra("eventType", eventType)
            putExtra("date", date.toString())
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(title, date),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(AlarmManager::class.java) ?: return null
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)

        return millis
    }

    fun cancel(context: Context, title: String, date: LocalDate) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(title, date),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            context.getSystemService(AlarmManager::class.java)?.cancel(pi)
        }
    }
}

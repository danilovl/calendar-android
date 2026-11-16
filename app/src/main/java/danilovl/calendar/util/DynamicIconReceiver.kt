package danilovl.calendar.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import danilovl.calendar.CalendarWidget
import danilovl.calendar.data.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles midnight ticks (own alarm) and system broadcasts: boot, app update, time/zone change.
 * Alarms are wiped on reboot, so reminders are re-planned here; the widget is refreshed so that
 * "today" changes at midnight instead of on the next 24h widget update.
 */
class DynamicIconReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        IconUtils.applyIconForToday(appContext, retireBootstrap = true)
        IconAlarmScheduler.scheduleNextMidnight(appContext)

        val rescheduleReminders = intent.action != IconAlarmScheduler.ACTION_UPDATE_ICON &&
            intent.action != Intent.ACTION_DATE_CHANGED

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (rescheduleReminders) {
                    ReminderScheduler.rescheduleAll(appContext)
                }
                CalendarWidget().updateAll(appContext)
            } catch (e: Exception) {
                AppLog.e("DynamicIconReceiver", "Failed to process ${intent.action}", e)
            } finally {
                pending.finish()
            }
        }
    }
}

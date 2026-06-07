package danilovl.calendar.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DayChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext

        IconUtils.applyIconForToday(appContext)
        IconAlarmScheduler.scheduleNextMidnight(appContext)
    }
}

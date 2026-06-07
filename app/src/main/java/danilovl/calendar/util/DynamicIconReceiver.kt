package danilovl.calendar.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DynamicIconReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        IconUtils.applyIconForToday(appContext, retireBootstrap = true)
        IconAlarmScheduler.scheduleNextMidnight(appContext)
    }
}

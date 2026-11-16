package danilovl.calendar

import android.app.Application
import androidx.glance.appwidget.GlanceAppWidgetManager
import danilovl.calendar.data.ReminderScheduler
import danilovl.calendar.data.repository.SettingsRepository
import danilovl.calendar.util.AppLog
import danilovl.calendar.util.IconAlarmScheduler
import danilovl.calendar.util.IconUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class CalendarApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        IconUtils.applyIconForToday(this)
        IconAlarmScheduler.scheduleNextMidnight(this)
        observeSettingsForWidget()
        appScope.launch(Dispatchers.IO) {
            // Covers cases where a system broadcast was missed (e.g. force-stop, first launch after update).
            ReminderScheduler.rescheduleAll(this@CalendarApp)
        }
    }

    private fun observeSettingsForWidget() {
        val repository = SettingsRepository.getInstance(this)
        var lastAllDayTime = repository.settings.value.allDayReminderTime
        var lastDefaultReminder = repository.settings.value.defaultReminder
        appScope.launch {
            repository.settings
                .drop(1)
                .collect { settings ->
                    if (settings.allDayReminderTime != lastAllDayTime || settings.defaultReminder != lastDefaultReminder) {
                        lastAllDayTime = settings.allDayReminderTime
                        lastDefaultReminder = settings.defaultReminder
                        launch(Dispatchers.IO) { ReminderScheduler.rescheduleAll(this@CalendarApp) }
                    }
                    try {
                        val widget = CalendarWidget()
                        val manager = GlanceAppWidgetManager(this@CalendarApp)
                        val ids = manager.getGlanceIds(CalendarWidget::class.java)
                        ids.forEach { glanceId ->
                            widget.update(this@CalendarApp, glanceId)
                        }
                    } catch (e: Exception) {
                        AppLog.e("CalendarApp", "Failed to update widget on settings change", e)
                    }
                }
        }
    }
}

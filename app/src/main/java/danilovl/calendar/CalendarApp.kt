package danilovl.calendar

import android.app.Application
import androidx.glance.appwidget.GlanceAppWidgetManager
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
    }

    private fun observeSettingsForWidget() {
        val repository = SettingsRepository.getInstance(this)
        appScope.launch {
            repository.settings
                .drop(1)
                .collect {
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

package danilovl.calendar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import danilovl.calendar.data.LocaleHelper
import danilovl.calendar.data.ReminderScheduler
import danilovl.calendar.data.local.CalendarDatabase
import danilovl.calendar.data.repository.ContactsRepository
import danilovl.calendar.data.repository.HolidayRepository
import danilovl.calendar.data.repository.SettingsRepository
import danilovl.calendar.ui.CalendarViewModel
import danilovl.calendar.ui.CalendarViewModelFactory
import danilovl.calendar.ui.screens.CalendarScreen
import danilovl.calendar.ui.theme.CalendarTheme
import danilovl.calendar.util.AppLog
import danilovl.calendar.util.IconAlarmScheduler
import danilovl.calendar.util.IconUtils
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: CalendarViewModel

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IconUtils.applyIconForToday(this)
        IconAlarmScheduler.scheduleNextMidnight(this)

        ReminderScheduler.ensureChannel(this)

        val database = CalendarDatabase.getDatabase(this)
        val contactsRepository = ContactsRepository(this)
        val holidayRepository = HolidayRepository(this)
        val settingsRepository = SettingsRepository.getInstance(this)

        val factory = CalendarViewModelFactory(
            this,
            database.eventDao(),
            contactsRepository,
            holidayRepository,
            settingsRepository
        )
        viewModel = ViewModelProvider(this, factory)[CalendarViewModel::class.java]

        handleIntent(intent)

        enableEdgeToEdge()
        setContent {
            CalendarTheme {
                CalendarScreen(viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        IconUtils.applyIconForToday(this, retireBootstrap = true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra("ADD_EVENT", false)) {
            viewModel.selectToday()
            viewModel.requestAddEvent()
            return
        }
        val eventId = intent.getIntExtra("EVENT_ID", -1)
        val origDateStr = intent.getStringExtra("EVENT_ORIG_DATE")
        val eventDateStr = intent.getStringExtra("EVENT_DATE")
        if (eventId != -1) {
            viewModel.showEventDetails(eventId, origDateStr, eventDateStr)
        } else {
            val type = intent.getStringExtra("EVENT_TYPE")
            val title = intent.getStringExtra("EVENT_TITLE")
            val dateStr = intent.getStringExtra("EVENT_DATE")
            if (type != null && title != null && dateStr != null) {
                try {
                    val date = LocalDate.parse(dateStr)
                    viewModel.showVirtualEventDetails(type, title, date, origDateStr)
                } catch (e: Exception) {
                    AppLog.e("MainActivity", "Failed to parse date from intent: $dateStr", e)
                }
            }
        }
    }

}
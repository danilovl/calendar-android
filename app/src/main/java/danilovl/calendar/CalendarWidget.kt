package danilovl.calendar

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import danilovl.calendar.data.EventType
import danilovl.calendar.data.local.CalendarDatabase
import danilovl.calendar.data.repository.ContactsRepository
import danilovl.calendar.data.repository.HolidayRepository
import danilovl.calendar.data.repository.SettingsRepository
import danilovl.calendar.service.isEventOnDate
import danilovl.calendar.util.AppLog
import danilovl.calendar.util.DateTimeUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarWidget : GlanceAppWidget() {

    companion object {
        val TealHeader = Color(0xFF26A69A)
        val BirthdayBg = Color(0xFFF0F7FF)
        val BirthdayText = Color(0xFFFB8C00)
        val EventOrange = Color(0xFFFB8C00)
        val SeparatorBg = Color(0xFFF5F5F5)
        val TextSecondary = Color(0xFF757575)
    }

    data class WidgetEvent(
        val id: Int? = null,
        val title: String,
        val startTime: LocalTime?,
        val endTime: LocalTime?,
        val type: String,
        val origDate: String? = null
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) = coroutineScope {
        val today = LocalDate.now()
        val db = CalendarDatabase.getDatabase(context)
        val settings = SettingsRepository.getInstance(context).getSettings()
        val holidayRepository = HolidayRepository(context)
        val contactsRepository = ContactsRepository(context)
        
        val widgetRangeDays = settings.widgetRangeDays
        
        val rangeEventsDeferred = async {
            try {
                withTimeoutOrNull(2000) {
                    db.eventDao().getEventsInRange(today, today.plusDays(widgetRangeDays.toLong())).first()
                } ?: emptyList()
            } catch (e: Exception) {
                AppLog.e("CalendarWidget", "Error fetching range events", e)
                emptyList()
            }
        }
        
        val recurringEventsDeferred = async {
            try {
                withTimeoutOrNull(1000) { db.eventDao().getRecurringEventsList() } ?: emptyList()
            } catch (e: Exception) {
                AppLog.e("CalendarWidget", "Error fetching recurring events", e)
                emptyList()
            }
        }
        
        val holidaysDeferred = async {
            if (settings.internationalHolidays) {
                try {
                    withTimeoutOrNull(3000) { holidayRepository.getHolidays(today.year, settings.holidayCountry) } ?: emptyList()
                } catch (e: Exception) {
                    AppLog.e("CalendarWidget", "Error fetching holidays", e)
                    emptyList()
                }
            } else emptyList()
        }
        
        val birthdaysDeferred = async {
            if (settings.importContactBirthdays) {
                try {
                    withTimeoutOrNull(2000) { contactsRepository.getBirthdays() } ?: emptyList()
                } catch (e: Exception) {
                    AppLog.e("CalendarWidget", "Error fetching birthdays", e)
                    emptyList()
                }
            } else emptyList()
        }

        val rangeEvents = rangeEventsDeferred.await()
        val recurringEvents = recurringEventsDeferred.await()
        val holidays = holidaysDeferred.await()
        val birthdays = birthdaysDeferred.await()
        
        val dbEvents = (rangeEvents + recurringEvents).distinctBy { it.id }

        val appLocale = if (settings.language.isNotEmpty() && settings.language != "system")
            Locale.forLanguageTag(settings.language) else Locale.getDefault()
        val localizedConfig = Configuration(context.resources.configuration).also { it.setLocale(appLocale) }
        val localizedContext = context.createConfigurationContext(localizedConfig)
        
        val allEventsInRange = mutableListOf<Pair<LocalDate, List<WidgetEvent>>>()
        try {
            for (i in 0..widgetRangeDays) {
                val date = today.plusDays(i.toLong())
                val dayEvents = mutableListOf<WidgetEvent>()
                
                dbEvents.filter { isEventOnDate(it, date) }.forEach {
                    val origDate = if (it.eventType == EventType.BIRTHDAY.value && it.description.contains("BIRTHDAY_ORIG_DATE:")) {
                        it.description.substringAfter("BIRTHDAY_ORIG_DATE:").substringBefore("|")
                    } else null
                    dayEvents.add(WidgetEvent(it.id, it.title, it.startTime, it.endTime, it.eventType, origDate))
                }
                
                holidays.filter { it.date == date }.forEach {
                    dayEvents.add(WidgetEvent(null, it.name, null, null, EventType.HOLIDAY.value))
                }
                
                birthdays.filter { it.date.month == date.month && it.date.dayOfMonth == date.dayOfMonth }.forEach { birthday ->
                    val existing = dayEvents.find { it.title == birthday.name && it.type == EventType.BIRTHDAY.value }
                    if (existing != null) {
                        // If already exists (likely from DB), update its origDate if it's missing
                        if (existing.origDate == null) {
                            val idx = dayEvents.indexOf(existing)
                            dayEvents[idx] = existing.copy(origDate = birthday.date.toString())
                        }
                    } else {
                        dayEvents.add(WidgetEvent(null, birthday.name, null, null, EventType.BIRTHDAY.value, birthday.date.toString()))
                    }
                }
                
                if (dayEvents.isNotEmpty()) {
                    allEventsInRange.add(date to dayEvents.sortedBy { it.startTime ?: LocalTime.MIN })
                }
            }
        } catch (e: Exception) {
            AppLog.e("CalendarWidget", "Error processing events", e)
        }

        provideContent {
            val context = LocalContext.current
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color.White)
                    .cornerRadius(16.dp)
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(TealHeader)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = today.dayOfMonth.toString(),
                        style = TextStyle(
                            color = ColorProvider(day = Color.White, night = Color.White),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = today.format(DateTimeFormatter.ofPattern("EEE")).uppercase(),
                            style = TextStyle(color = ColorProvider(day = Color.White, night = Color.White), fontSize = 12.sp)
                        )
                        Text(
                            text = today.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = TextStyle(color = ColorProvider(day = Color.White, night = Color.White), fontSize = 12.sp)
                        )
                    }
                    
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .background(ColorProvider(day = Color.White.copy(alpha = 0.2f), night = Color.White.copy(alpha = 0.2f)))
                            .cornerRadius(16.dp)
                            .clickable(actionStartActivity(
                                android.content.Intent(context, MainActivity::class.java).apply {
                                    putExtra("ADD_EVENT", true)
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            style = TextStyle(color = ColorProvider(day = Color.White, night = Color.White), fontSize = 24.sp)
                        )
                    }
                }

                if (allEventsInRange.isEmpty()) {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = context.getString(R.string.common_no_events), style = TextStyle(color = ColorProvider(day = TextSecondary, night = TextSecondary)))
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(allEventsInRange) { (date, events) ->
                            Column(modifier = GlanceModifier.fillMaxWidth()) {
                                if (date != today) {
                                    Box(
                                        modifier = GlanceModifier
                                            .fillMaxWidth()
                                            .background(SeparatorBg)
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = date.format(DateTimeFormatter.ofPattern("EEE, d MMMM")).uppercase(),
                                            style = TextStyle(
                                                color = ColorProvider(day = TextSecondary, night = TextSecondary),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                                
                                events.forEachIndexed { index, event ->
                                    EventItem(event, date)
                                    if (index < events.size - 1) {
                                        Spacer(modifier = GlanceModifier.height(1.dp).fillMaxWidth().background(SeparatorBg))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EventItem(event: WidgetEvent, date: LocalDate) {
        val isBirthday = event.type == EventType.BIRTHDAY.value
        val isHoliday = event.type == EventType.HOLIDAY.value
        val context = LocalContext.current
        
        val intent = android.content.Intent(context, MainActivity::class.java).apply {
            if (event.id != null) {
                putExtra("EVENT_ID", event.id)
                putExtra("EVENT_DATE", date.toString())
            } else {
                putExtra("EVENT_TYPE", event.type)
                putExtra("EVENT_TITLE", event.title)
                putExtra("EVENT_DATE", date.toString())
                if (event.origDate != null) {
                    putExtra("EVENT_ORIG_DATE", event.origDate)
                }
            }
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(if (isBirthday) BirthdayBg else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(actionStartActivity(intent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = event.title,
                    style = TextStyle(
                        color = ColorProvider(day = if (isBirthday) BirthdayText else Color.Black, night = if (isBirthday) BirthdayText else Color.White),
                        fontSize = 14.sp,
                        fontWeight = if (isBirthday || isHoliday) FontWeight.Medium else FontWeight.Normal
                    ),
                    maxLines = 2
                )
                if (event.startTime != null) {
                    val timeText = if (event.endTime != null) {
                        "${event.startTime.format(DateTimeUtils.timeFormatter)}–${event.endTime.format(DateTimeUtils.timeFormatter)}"
                    } else {
                        event.startTime.format(DateTimeUtils.timeFormatter)
                    }
                    Text(
                        text = timeText,
                        style = TextStyle(color = ColorProvider(day = TextSecondary, night = TextSecondary), fontSize = 12.sp)
                    )
                }
            }
            
            if (!isBirthday && !isHoliday) {
                Box(
                    modifier = GlanceModifier
                        .size(8.dp)
                        .background(EventOrange)
                        .cornerRadius(4.dp)
                ) {}
            }
        }
    }
}

class CalendarWidgetReceiver : androidx.glance.appwidget.GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()
}

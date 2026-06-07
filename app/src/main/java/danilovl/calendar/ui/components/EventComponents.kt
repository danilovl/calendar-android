package danilovl.calendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import danilovl.calendar.R
import danilovl.calendar.data.EventType
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.repository.AppSettings
import danilovl.calendar.ui.appLocale
import danilovl.calendar.ui.theme.XiaomiBg
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiRed
import danilovl.calendar.ui.theme.XiaomiSurface
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.ui.trans
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun isCelebrationEvent(event: CalendarEvent): Boolean =
    event.eventType == EventType.BIRTHDAY.value || event.eventType == EventType.ANNIVERSARY.value

fun eventIcon(event: CalendarEvent): ImageVector {
    return when (event.eventType) {
        EventType.BIRTHDAY.value -> Icons.Default.Cake
        EventType.ANNIVERSARY.value -> Icons.Default.Favorite
        EventType.REMINDER.value -> Icons.Default.Notifications
        EventType.HOLIDAY.value -> Icons.Default.Star
        else -> Icons.Default.Event
    }
}

fun eventAccentColor(event: CalendarEvent, settings: AppSettings? = null): Color {
    event.color?.let { return Color(it) }
    if (event.eventType == EventType.HOLIDAY.value) return XiaomiRed
    if (settings != null) {
        return when (event.eventType) {
            EventType.BIRTHDAY.value -> Color(settings.birthdayColor)
            EventType.ANNIVERSARY.value -> Color(settings.anniversaryColor)
            EventType.REMINDER.value -> Color(settings.reminderColor)
            else -> Color(settings.eventColor)
        }
    }
    return if (isCelebrationEvent(event)) XiaomiRed else XiaomiBlue
}

@Composable
fun XiaomiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: androidx.compose.foundation.BorderStroke? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = XiaomiSurface,
        border = border,
        content = content
    )
}

@Composable
fun SelectedDateDetails(
    events: List<CalendarEvent>,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onEventClick: (CalendarEvent) -> Unit = {},
    settings: AppSettings? = null
) {
    val allDetails = events

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XiaomiBg)
            .padding(16.dp)
    ) {
        if (allDetails.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = trans(R.string.common_no_events), color = XiaomiTextSecondary, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(allDetails) { event ->
                    EventItem(event, onDelete = onDeleteEvent, onClick = onEventClick, settings = settings)
                }
            }
        }
    }
}

@Composable
fun EventItem(
    event: CalendarEvent,
    onDelete: ((CalendarEvent) -> Unit)? = null,
    onClick: ((CalendarEvent) -> Unit)? = null,
    settings: AppSettings? = null
) {
    val color = eventAccentColor(event, settings)
    XiaomiCard(onClick = { onClick?.invoke(event) }) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(eventIcon(event), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (event.eventType == EventType.HOLIDAY.value) XiaomiRed else XiaomiTextPrimary
                )

                if (event.description.contains("BIRTHDAY_ORIG_DATE:")) {
                    val dateStr = event.description.substringAfter("BIRTHDAY_ORIG_DATE:").substringBefore("|")
                    val subtitle = event.description.substringAfter("|")
                    val locale = appLocale()

                    val formattedDate = try {
                        LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
                    } catch (e: Exception) { dateStr }

                    if (subtitle.isNotBlank()) {
                        Text(text = subtitle, fontSize = 13.sp, color = XiaomiTextSecondary)
                    }

                    if (formattedDate.isNotBlank()) {
                        Text(text = formattedDate, fontSize = 13.sp, color = XiaomiTextSecondary)
                    }
                } else if (event.description.isNotBlank()) {
                    Text(text = event.description, fontSize = 13.sp, color = XiaomiTextSecondary)
                }
                
                val timeText = if (event.isAllDay) {
                    if (event.eventType == EventType.HOLIDAY.value || event.eventType == EventType.BIRTHDAY.value) null
                    else trans(R.string.common_all_day)
                } else if (event.startTime != null) {
                    "${event.startTime}${if (event.endTime != null) " - ${event.endTime}" else ""}"
                } else null
                
                if (timeText != null || event.location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        if (timeText != null) {
                            Text(text = timeText, fontSize = 13.sp, color = XiaomiTextSecondary)
                        }
                        if (timeText != null && event.location.isNotBlank()) {
                            Text(text = " • ", fontSize = 13.sp, color = XiaomiTextSecondary)
                        }
                        if (event.location.isNotBlank()) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = XiaomiBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = event.location, fontSize = 13.sp, color = XiaomiBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (onDelete != null && event.id >= 0 && event.id < 1000000000L) {
                IconButton(onClick = { onDelete(event) }) {
                    Icon(Icons.Default.Delete, contentDescription = trans(R.string.action_delete), tint = XiaomiTextSecondary.copy(alpha = 0.3f))
                }
            }
        }
    }
}

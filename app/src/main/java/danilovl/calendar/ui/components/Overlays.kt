package danilovl.calendar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import danilovl.calendar.R
import danilovl.calendar.data.EventType
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.ui.appLocale
import danilovl.calendar.ui.theme.XiaomiBg
import danilovl.calendar.ui.theme.XiaomiSurface
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.util.DateTimeUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle

@Composable
fun GoToDatePicker(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit
) {
    WheelDateTimePickerDialog(
        initialDate = initialDate,
        initialTime = LocalTime.MIN,
        showTime = false,
        onConfirm = { date, _ -> onPick(date) },
        onDismiss = onDismiss
    )
}

@Composable
fun DayEventsPopup(
    date: LocalDate,
    events: List<CalendarEvent>,
    onDismiss: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit
) {
    val locale = appLocale()
    val timeFmt = DateTimeUtils.timeFormatter
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    
    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.85f)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = XiaomiSurface,
                modifier = Modifier.fillMaxWidth(0.86f).heightIn(max = 440.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${date.monthValue}/${date.dayOfMonth}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = XiaomiTextPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                            fontSize = 14.sp,
                            color = XiaomiTextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = XiaomiBg)

                    val isEmpty = events.isEmpty()
                    if (isEmpty) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.common_no_events), color = XiaomiTextSecondary)
                        }
                    } else {
                        val allDayText = stringResource(R.string.common_all_day)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(events.size) { i ->
                                val e = events[i]
                                val sub = when {
                                    e.eventType == EventType.HOLIDAY.value -> stringResource(R.string.event_holiday)
                                    e.eventType == EventType.BIRTHDAY.value -> stringResource(R.string.event_birthday)
                                    e.isAllDay -> allDayText
                                    e.startTime != null -> e.startTime.format(timeFmt) +
                                        (e.endTime?.let { "-${it.format(timeFmt)}" } ?: "")
                                    else -> ""
                                }
                                val displayTitle = if (e.eventType == EventType.BIRTHDAY.value) "🎂 ${e.title}" else e.title
                                PopupRow(eventAccentColor(e), displayTitle, sub) { onEventClick(e) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupRow(dotColor: Color, title: String, subtitle: String, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = XiaomiTextPrimary, maxLines = 2)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 12.sp, color = XiaomiTextSecondary)
            }
        }
    }
}

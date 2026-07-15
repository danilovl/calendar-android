package danilovl.calendar.ui.components

import android.content.Intent
import android.icu.text.RelativeDateTimeFormatter
import android.icu.text.RelativeDateTimeFormatter.AbsoluteUnit
import android.icu.text.RelativeDateTimeFormatter.Direction
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit
import android.icu.util.ULocale
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import danilovl.calendar.R
import danilovl.calendar.data.EventType
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.repository.AppSettings
import danilovl.calendar.ui.appLocale
import danilovl.calendar.ui.screens.LocationPickerScreen
import danilovl.calendar.ui.theme.XiaomiBg
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiSurface
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.ui.trans
import danilovl.calendar.util.AppLog
import danilovl.calendar.util.DateTimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun EventDetailSheet(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onEdit: (CalendarEvent) -> Unit,
    onDelete: (CalendarEvent, Boolean) -> Unit,
    onToggleReminder: () -> Unit = {},
    settings: AppSettings? = null,
    selectedDate: LocalDate? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val locale = appLocale()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMapChoice by remember { mutableStateOf(false) }
    var showInAppMap by remember { mutableStateOf(false) }

    if (showInAppMap) {
        Dialog(
            onDismissRequest = { showInAppMap = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LocationPickerScreen(
                    initialLocation = event.location,
                    onBack = { showInAppMap = false },
                    readOnly = true
                )
            }
        }
        return
    }

    if (showMapChoice) {
        val options = listOf(
            trans(R.string.action_open_in_app_map),
            trans(R.string.action_open_in_maps)
        )
        ChoiceDialog(
            title = trans(R.string.map_view_title),
            options = options,
            selected = "",
            onSelect = { choice ->
                showMapChoice = false
                if (choice == options[0]) {
                    showInAppMap = true
                } else {
                    try {
                        val uriString = if (event.location.startsWith("geo:")) {
                            val parts = event.location.substring(4).split("|")
                            "geo:${parts[0]}?q=${Uri.encode(parts.getOrElse(1) { parts[0] })}"
                        } else {
                            "geo:0,0?q=" + Uri.encode(event.location)
                        }
                        val gmmIntentUri = Uri.parse(uriString)
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        context.startActivity(mapIntent)
                    } catch (e: Exception) {
                        AppLog.w("EventDetailSheet", "No app to open location: ${event.location}", e)
                    }
                }
            },
            onDismiss = { showMapChoice = false }
        )
    }

    if (showDeleteConfirm) {
        Dialog(onDismissRequest = { showDeleteConfirm = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = XiaomiSurface,
                modifier = Modifier.padding(24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.delete_series_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = XiaomiTextPrimary
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = { onDelete(event, true); showDeleteConfirm = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.delete_this_occurrence), color = XiaomiBlue)
                    }
                    TextButton(
                        onClick = { onDelete(event, false); showDeleteConfirm = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.delete_all_occurrences), color = XiaomiBlue)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text(stringResource(R.string.common_cancel), color = XiaomiTextSecondary)
                        }
                    }
                }
            }
        }
    }
    val isBirthday = event.eventType == EventType.BIRTHDAY.value
    val isCelebration = isBirthday || event.eventType == EventType.ANNIVERSARY.value
    val headerColor = eventAccentColor(event, settings)

    val birthDate = remember(event) {
        if (isBirthday) {
            if (event.description.contains("BIRTHDAY_ORIG_DATE:")) {
                val dateStr = event.description.substringAfter("BIRTHDAY_ORIG_DATE:").substringBefore("|")
                try { LocalDate.parse(dateStr) } catch (e: Exception) { null }
            } else if (event.repeat.contains("yearly")) {
                event.date
            } else if (event.id < 0) { // Virtual event fallback
                event.date
            } else {
                null
            }
        } else null
    }

    val age = remember(birthDate, selectedDate) {
        birthDate?.let {
            val targetDate = selectedDate ?: event.date
            ChronoUnit.YEARS.between(it, targetDate).toInt()
        }
    }

    val timeFmt = DateTimeUtils.timeFormatter
    val subtitle = remember(event, selectedDate) {
        val displayDate = selectedDate ?: event.date
        val date = displayDate
            .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
        val time = when {
            event.isAllDay -> null
            event.startTime != null -> event.startTime.format(timeFmt) +
                (event.endTime?.let { ", ${it.format(timeFmt)}" } ?: "")
            else -> null
        }
        if (time != null) "$date, $time" else date
    }
    val relative = remember(event, selectedDate, locale) { relativeDay(selectedDate ?: event.date, locale) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(XiaomiBg)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isCelebration) {
                        CelebrationHeader(event, headerColor, age, birthDate)
                        ReminderPill(event, headerColor, onToggleReminder)
                        Spacer(Modifier.height(16.dp))
                    } else {
                        EventHeader(event, subtitle, headerColor)
                        Column(modifier = Modifier.padding(20.dp)) {
                            if (event.location.isNotBlank()) {
                                XiaomiCard(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    onClick = { showMapChoice = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = XiaomiBlue)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            val displayLocation = if (event.location.startsWith("geo:")) event.location.substringAfter("|") else event.location
                                            Text(displayLocation, fontSize = 15.sp, color = XiaomiTextPrimary)
                                            Text(trans(R.string.action_open_in_maps), fontSize = 12.sp, color = XiaomiBlue)
                                        }
                                    }
                                }
                            }

                            if (event.repeat != "none") {
                                DetailRow(trans(R.string.detail_repeat), repeatLabel(event.repeat))
                                if (selectedDate != null && selectedDate != event.date) {
                                    DetailRow(
                                        trans(R.string.label_start),
                                        event.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
                                    )
                                }
                                DetailRow(
                                    trans(R.string.detail_repeat_end),
                                    event.repeatUntil?.let { DateTimeUtils.formatDateLong(it) } ?: trans(R.string.detail_repeat_never)
                                )
                            }
                            DetailRow(trans(R.string.detail_account), trans(R.string.detail_local_calendar) + " >")
                            DetailRow(
                                trans(R.string.settings_reminder_item),
                                if (event.reminder) "${settingLabel(event.reminderOffset)} · ${settingLabel(event.reminderMelody)}" else trans(R.string.reminder_off)
                            )
                        }
                    }
                }

                if (event.id >= 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(vertical = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally)
                    ) {
                        RoundActionButton(Icons.Default.Edit, trans(R.string.action_edit)) { onEdit(event) }
                        RoundActionButton(Icons.Default.Delete, trans(R.string.action_delete)) {
                            if (event.repeat != "none") {
                                showDeleteConfirm = true
                            } else {
                                onDelete(event, false)
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.18f))
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = trans(R.string.action_close),
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CelebrationHeader(event: CalendarEvent, color: Color, age: Int?, birthDate: LocalDate?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Cake, 
                contentDescription = null, 
                tint = Color.White, 
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                event.title,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (age != null && age > 0) {
                Text(
                    text = trans(R.string.birthday_age, age),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (birthDate != null) {
                val locale = appLocale()
                val dateText = birthDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
                Text(
                    text = dateText,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EventHeader(event: CalendarEvent, subtitle: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    eventIcon(event),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(event.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)
        }
    }
}

@Composable
private fun ReminderPill(event: CalendarEvent, color: Color, onClick: () -> Unit) {
    val isActive = event.reminder
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (isActive) color.copy(alpha = 0.2f) else color.copy(alpha = 0.1f),
            onClick = onClick,
            border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, color) else null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isActive) trans(R.string.reminder_on) else trans(R.string.action_set_reminder),
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RoundActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = XiaomiSurface,
        shadowElevation = 4.dp,
        modifier = Modifier.size(60.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = XiaomiTextPrimary, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = XiaomiTextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, color = XiaomiTextSecondary, textAlign = TextAlign.End)
    }
}

private fun relativeDay(date: LocalDate, locale: Locale): String {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
    val rdtf = RelativeDateTimeFormatter.getInstance(ULocale.forLocale(locale))
    val text = when (days) {
        0L -> rdtf.format(Direction.THIS, AbsoluteUnit.DAY)
        1L -> rdtf.format(Direction.NEXT, AbsoluteUnit.DAY)
        -1L -> rdtf.format(Direction.LAST, AbsoluteUnit.DAY)
        else -> if (days > 0) rdtf.format(days.toDouble(), Direction.NEXT, RelativeUnit.DAYS)
                else rdtf.format((-days).toDouble(), Direction.LAST, RelativeUnit.DAYS)
    }
    return text.replaceFirstChar { it.titlecase(locale) }
}


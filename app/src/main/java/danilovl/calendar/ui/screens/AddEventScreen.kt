package danilovl.calendar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import danilovl.calendar.R
import danilovl.calendar.data.EventType
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.data.repository.SettingsOptions
import danilovl.calendar.data.repository.SettingsRepository
import danilovl.calendar.ui.components.CategoryItem
import danilovl.calendar.ui.components.ChoiceDialog
import danilovl.calendar.ui.components.ColorPickerDialog
import danilovl.calendar.ui.components.ReminderMelodyRow
import danilovl.calendar.ui.components.ReminderWheelPickerDialog
import danilovl.calendar.ui.components.RepeatDialog
import danilovl.calendar.ui.components.WheelDateTimePickerDialog
import danilovl.calendar.ui.components.XiaomiCard
import danilovl.calendar.ui.components.repeatLabel
import danilovl.calendar.ui.components.screenTitle
import danilovl.calendar.ui.components.settingLabel
import danilovl.calendar.ui.components.transparentTextFieldColors
import danilovl.calendar.ui.theme.XiaomiBg
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.ui.trans
import danilovl.calendar.util.DateTimeUtils
import java.time.LocalDate
import java.time.LocalTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    selectedDate: LocalDate,
    initialEvent: CalendarEvent? = null,
    instanceDate: LocalDate? = null,
    initialTime: LocalTime? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, LocalDate, LocalDate?, LocalTime?, LocalTime?, Boolean, String, Boolean, String, String, String, String, Int?, String, LocalDate?, Boolean) -> Unit
) {
    val context = LocalContext.current
    val defaults = remember { SettingsRepository.getInstance(context).settings.value }

    var eventType by remember { mutableStateOf(EventType.fromString(initialEvent?.eventType).value) }
    val isFullEvent = eventType == EventType.EVENT.value
    val isReminder = eventType == EventType.REMINDER.value
    val isCelebration = eventType == EventType.BIRTHDAY.value || eventType == EventType.ANNIVERSARY.value

    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var isAllDay by remember { mutableStateOf(initialEvent?.isAllDay ?: false) }
    var startTime by remember { mutableStateOf(initialEvent?.startTime ?: initialTime ?: LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(initialEvent?.endTime ?: initialTime?.plusHours(1) ?: LocalTime.of(10, 0)) }
    var startDate by remember { mutableStateOf(instanceDate ?: initialEvent?.date ?: selectedDate) }
    var endDate by remember { mutableStateOf(instanceDate ?: initialEvent?.endDate ?: initialEvent?.date ?: selectedDate) }
    var repeat by remember { mutableStateOf(initialEvent?.repeat ?: "none") }
    var repeatUntil by remember { mutableStateOf(initialEvent?.repeatUntil) }
    var hasReminder by remember {
        mutableStateOf(
            initialEvent?.reminder ?: (eventType == EventType.REMINDER.value)
        )
    }
    var reminderMelody by remember { mutableStateOf(initialEvent?.reminderMelody ?: defaults.reminderMelody) }
    var reminderOffset by remember { mutableStateOf(initialEvent?.reminderOffset ?: defaults.defaultReminder) }
    var timezone by remember { mutableStateOf(initialEvent?.timeZone ?: defaults.eventTimeZone) }
    var eventColor by remember { mutableStateOf(initialEvent?.color) }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }

    val scrollState = rememberScrollState()
    var titleError by remember { mutableStateOf(false) }
    var showMelodyDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showWhenPicker by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var showRepeatUntilPicker by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }

    if (showLocationPicker) {
        LocationPickerScreen(
            initialLocation = location,
            onLocationSelected = {
                location = it
                showLocationPicker = false
            },
            onBack = { showLocationPicker = false }
        )
        return
    }

    if (showMelodyDialog) {
        ChoiceDialog(trans(R.string.select_melody), SettingsOptions.melodies, reminderMelody, { reminderMelody = it; showMelodyDialog = false }, { showMelodyDialog = false })
    }
    if (showReminderDialog) {
        ReminderWheelPickerDialog(
            initialOffset = reminderOffset,
            onSelect = {
                reminderOffset = it
                hasReminder = true
                showReminderDialog = false
            },
            onDismiss = { showReminderDialog = false },
            onNone = {
                hasReminder = false
                showReminderDialog = false
            }
        )
    }
    if (showStartPicker) {
        WheelDateTimePickerDialog(
            initialDate = startDate,
            initialTime = startTime,
            showTime = !isAllDay,
            onConfirm = { date, time ->
                startDate = date
                startTime = time
                val newEnd = time.plusHours(1)
                endTime = newEnd
                endDate = if (newEnd.isBefore(time)) date.plusDays(1) else date
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        WheelDateTimePickerDialog(
            initialDate = endDate,
            initialTime = endTime,
            showTime = !isAllDay,
            onConfirm = { date, time ->
                endDate = date
                endTime = time
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
    if (showWhenPicker) {
        WheelDateTimePickerDialog(
            initialDate = startDate,
            initialTime = LocalTime.of(0, 0),
            showTime = false,
            onConfirm = { date, _ ->
                startDate = date
                showWhenPicker = false
            },
            onDismiss = { showWhenPicker = false }
        )
    }

    if (showRepeatDialog) {
        RepeatDialog(
            currentRepeat = repeat,
            onSelect = { repeat = it; showRepeatDialog = false },
            onDismiss = { showRepeatDialog = false }
        )
    }

    if (showRepeatUntilPicker) {
        WheelDateTimePickerDialog(
            initialDate = repeatUntil ?: endDate.plusMonths(1),
            initialTime = LocalTime.of(0, 0),
            showTime = false,
            onConfirm = { date, _ ->
                repeatUntil = date
                showRepeatUntilPicker = false
            },
            onDismiss = { showRepeatUntilPicker = false },
            onNone = {
                repeatUntil = null
                showRepeatUntilPicker = false
            }
        )
    }

    if (showColorDialog) {
        ColorPickerDialog(
            currentColor = eventColor,
            onSelect = { eventColor = it; showColorDialog = false },
            onDismiss = { showColorDialog = false }
        )
    }

    fun confirm(onlyThis: Boolean = false) {
        if (title.isBlank()) {
            titleError = true
            return
        }
        if (isFullEvent) {
            onConfirm(
                title, description,
                startDate,
                endDate,
                if (isAllDay) null else startTime,
                if (isAllDay) null else endTime,
                isAllDay, repeat, hasReminder, reminderMelody, reminderOffset, timezone, EventType.EVENT.value,
                eventColor, location, repeatUntil, onlyThis
            )
        } else {
            val rep = if (isCelebration) "yearly" else repeat
            val desc = if (eventType == EventType.ANNIVERSARY.value) "" else description
            onConfirm(title, desc, startDate, null, null, null, true, rep, hasReminder, reminderMelody, reminderOffset, timezone, eventType, eventColor, "", repeatUntil, onlyThis)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        screenTitle(eventType, initialEvent != null),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = trans(R.string.action_close),
                                modifier = Modifier.size(18.dp),
                                tint = Color.DarkGray
                            )
                        }
                    }
                },
                actions = {
                    if (initialEvent == null || initialEvent.repeat == "none") {
                        IconButton(onClick = { confirm(false) }) {
                            Icon(Icons.Default.Check, contentDescription = trans(R.string.action_done), tint = Color.Black, modifier = Modifier.size(28.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = XiaomiBg)
            )
        },
        containerColor = XiaomiBg
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CategoryItem(trans(R.string.type_event), Icons.Default.Event, eventType == EventType.EVENT.value) {
                    if (eventType != EventType.EVENT.value) {
                        eventType = EventType.EVENT.value
                        hasReminder = false
                    }
                }
                CategoryItem(trans(R.string.type_birthday).take(10) + "...", Icons.Default.Cake, eventType == EventType.BIRTHDAY.value) {
                    if (eventType != EventType.BIRTHDAY.value) {
                        eventType = EventType.BIRTHDAY.value
                        hasReminder = false
                    }
                }
                CategoryItem(trans(R.string.type_anniversary), Icons.Default.Favorite, eventType == EventType.ANNIVERSARY.value) {
                    if (eventType != EventType.ANNIVERSARY.value) {
                        eventType = EventType.ANNIVERSARY.value
                        hasReminder = false
                    }
                }
                CategoryItem(trans(R.string.type_reminder), Icons.Default.Notifications, eventType == EventType.REMINDER.value) {
                    if (eventType != EventType.REMINDER.value) {
                        eventType = EventType.REMINDER.value
                        hasReminder = true
                    }
                }
            }

            XiaomiCard(
                border = if (titleError) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error) else null
            ) {
                Column {
                    TextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (titleError && it.isNotBlank()) titleError = false
                        },
                        placeholder = { Text(if (eventType == EventType.BIRTHDAY.value) trans(R.string.enter_name) else if (isFullEvent) trans(R.string.enter_title) else trans(R.string.type_reminder)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = transparentTextFieldColors(),
                        isError = titleError
                    )
                    if (titleError) {
                        Text(
                            text = if (eventType == EventType.BIRTHDAY.value) trans(R.string.enter_name) else trans(R.string.enter_title),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    }
                }
            }

            if (isFullEvent) {
                XiaomiCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                            Text(trans(R.string.common_all_day), modifier = Modifier.weight(1f), fontSize = 16.sp)
                            Switch(
                                checked = isAllDay, 
                                onCheckedChange = { isAllDay = it }, 
                                colors = SwitchDefaults.colors(checkedThumbColor = XiaomiBlue),
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = XiaomiBg)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { showStartPicker = true }.padding(10.dp)
                        ) {
                            Text(trans(R.string.label_start), modifier = Modifier.weight(1f), fontSize = 16.sp)
                            Text(
                                DateTimeUtils.formatDateTimeLabel(startDate, startTime, isAllDay),
                                color = XiaomiTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = XiaomiBg)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { showEndPicker = true }.padding(10.dp)
                        ) {
                            Text(trans(R.string.label_end), modifier = Modifier.weight(1f), fontSize = 16.sp)
                            Text(
                                DateTimeUtils.formatDateTimeLabel(endDate, endTime, isAllDay),
                                color = XiaomiTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (isFullEvent || isReminder) {
                    XiaomiCard {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { showRepeatDialog = true }.padding(10.dp)
                            ) {
                                Text(trans(R.string.detail_repeat), modifier = Modifier.weight(1f), fontSize = 16.sp)
                                Text("${repeatLabel(repeat)} >", color = XiaomiTextSecondary, fontSize = 14.sp)
                            }
                            if (repeat != "none") {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = XiaomiBg)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { showRepeatUntilPicker = true }.padding(10.dp)
                                ) {
                                    Text(trans(R.string.detail_repeat_end), modifier = Modifier.weight(1f), fontSize = 16.sp)
                                    Text(
                                        if (repeatUntil != null) DateTimeUtils.formatDateLong(repeatUntil!!) + " >" else trans(R.string.detail_repeat_never) + " >",
                                        color = XiaomiTextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                XiaomiCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { showColorDialog = true }.padding(10.dp)
                    ) {
                        Text(trans(R.string.select_color), modifier = Modifier.weight(1f), fontSize = 16.sp)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (eventColor != null) Color(eventColor!!) else Color.Transparent)
                                .border(1.dp, Color.LightGray, CircleShape)
                        )
                    }
                }

                XiaomiCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = if (location.startsWith("geo:")) location.substringAfter("|") else location,
                            onValueChange = { location = it },
                            placeholder = { Text(trans(R.string.label_location)) },
                            modifier = Modifier.weight(1f),
                            colors = transparentTextFieldColors(),
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = XiaomiBlue) }
                        )
                        IconButton(onClick = { showLocationPicker = true }) {
                            Icon(Icons.Default.Map, contentDescription = trans(R.string.select_location), tint = XiaomiBlue)
                        }
                    }
                }

                XiaomiCard {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { showReminderDialog = true }.padding(10.dp)
                        ) {
                            Text(trans(R.string.settings_reminder_item), modifier = Modifier.weight(1f), fontSize = 16.sp)
                            Text(
                                if (hasReminder) "${settingLabel(reminderOffset)} >" else "${trans(R.string.reminder_off)} >",
                                color = XiaomiTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = XiaomiBg)
                        ReminderMelodyRow(reminderMelody, hasReminder, { showMelodyDialog = true }, { hasReminder = it })
                    }
                }

                XiaomiCard {
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text(trans(R.string.label_description)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        colors = transparentTextFieldColors()
                    )
                }

                XiaomiCard {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(10.dp)
                        ) {
                            Text(trans(R.string.detail_account), modifier = Modifier.weight(1f), fontSize = 16.sp)
                            Text("${trans(R.string.detail_local_calendar)} >", color = XiaomiTextSecondary, fontSize = 14.sp)
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = XiaomiBg)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(10.dp)
                        ) {
                            Text(trans(R.string.label_timezone), modifier = Modifier.weight(1f), fontSize = 16.sp)
                            Text("($timezone) >", color = XiaomiTextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                XiaomiCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { showWhenPicker = true }.padding(10.dp)
                    ) {
                        Text(trans(R.string.label_when), modifier = Modifier.weight(1f), fontSize = 16.sp)
                        Text(
                            DateTimeUtils.formatDateLong(startDate),
                            color = XiaomiTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }

                if (isReminder) {
                    XiaomiCard {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { showRepeatDialog = true }.padding(10.dp)
                            ) {
                                Text(trans(R.string.detail_repeat), modifier = Modifier.weight(1f), fontSize = 16.sp)
                                Text("${repeatLabel(repeat)} >", color = XiaomiTextSecondary, fontSize = 14.sp)
                            }
                            if (repeat != "none") {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = XiaomiBg)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { showRepeatUntilPicker = true }.padding(10.dp)
                                ) {
                                    Text(trans(R.string.detail_repeat_end), modifier = Modifier.weight(1f), fontSize = 16.sp)
                                    Text(
                                        if (repeatUntil != null) DateTimeUtils.formatDateLong(repeatUntil!!) + " >" else trans(R.string.detail_repeat_never) + " >",
                                        color = XiaomiTextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                XiaomiCard {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { showReminderDialog = true }.padding(10.dp)
                        ) {
                            Text(trans(R.string.settings_reminder_item), modifier = Modifier.weight(1f), fontSize = 16.sp)
                            Text(
                                if (hasReminder) "${settingLabel(reminderOffset)} >" else "${trans(R.string.reminder_off)} >",
                                color = XiaomiTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = XiaomiBg)
                        ReminderMelodyRow(reminderMelody, hasReminder, { showMelodyDialog = true }, { hasReminder = it })
                    }
                }

                if (eventType != EventType.ANNIVERSARY.value) {
                    XiaomiCard {
                        TextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text(trans(R.string.label_description)) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                            colors = transparentTextFieldColors()
                        )
                    }
                }
            }

            if (initialEvent != null && initialEvent.repeat != "none") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Button(
                        onClick = { confirm(onlyThis = true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = XiaomiBlue)
                    ) {
                        Text(trans(R.string.save_this_occurrence), color = Color.White)
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { confirm(onlyThis = false) },
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, XiaomiBlue)
                    ) {
                        Text(trans(R.string.save_all_occurrences), color = XiaomiBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}




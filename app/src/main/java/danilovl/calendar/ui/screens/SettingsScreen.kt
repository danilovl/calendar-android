package danilovl.calendar.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import danilovl.calendar.R
import danilovl.calendar.data.AppLanguages
import danilovl.calendar.data.ExternalCalendar
import danilovl.calendar.data.remote.HolidayCountries
import danilovl.calendar.data.repository.AppSettings
import danilovl.calendar.data.repository.HolidayRepository
import danilovl.calendar.data.repository.SettingsOptions
import danilovl.calendar.ui.SettingsViewModel
import danilovl.calendar.ui.SettingsViewModelFactory
import danilovl.calendar.ui.components.ChoiceScreenSpec
import danilovl.calendar.ui.components.ColorItem
import danilovl.calendar.ui.components.ColorPickerDialog
import danilovl.calendar.ui.components.CountryPickerScreen
import danilovl.calendar.ui.components.ItemDivider
import danilovl.calendar.ui.components.LanguagePickerScreen
import danilovl.calendar.ui.components.MultiCountryPickerScreen
import danilovl.calendar.ui.components.NavigationItem
import danilovl.calendar.ui.components.SelectorItem
import danilovl.calendar.ui.components.SettingsCategory
import danilovl.calendar.ui.components.SettingsGroup
import danilovl.calendar.ui.components.SingleChoiceScreen
import danilovl.calendar.ui.components.SwitchItem
import danilovl.calendar.ui.components.WheelDateTimePickerDialog
import danilovl.calendar.ui.components.WidgetRangeDialog
import danilovl.calendar.ui.components.settingLabel
import danilovl.calendar.ui.formatDays
import danilovl.calendar.ui.theme.XiaomiBg
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date
import java.util.Locale

@Composable
private fun formatLastSync(timestamp: Long): String {
    if (timestamp == 0L) return stringResource(R.string.settings_last_sync_never)
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return stringResource(R.string.settings_last_sync, formatter.format(date))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationManager = remember { NotificationManagerCompat.from(context) }
    var notificationsEnabled by remember { mutableStateOf(notificationManager.areNotificationsEnabled()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = notificationManager.areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val noEventsToExport = stringResource(R.string.no_events_to_export)
    val exportSuccess = stringResource(R.string.settings_export_success)
    val importSuccess = stringResource(R.string.settings_import_success)
    val importError = stringResource(R.string.settings_import_error)

    var showExportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf<String?>(null) }
    var selectedTypes by remember { mutableStateOf(setOf("event", "birthday", "anniversary", "reminder")) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val events = viewModel.getFilteredEventsList(selectedTypes)
                    if (events.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(noEventsToExport)
                        }
                        return@launch
                    }
                    val json = danilovl.calendar.util.EventBackupUtils.exportToJson(events)
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(json.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, exportSuccess, android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    AppLog.e("SettingsScreen", "Operation failed", e)
                }
            }
        }
    }

    val exportIcsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val events = viewModel.getFilteredEventsList(selectedTypes)
                    if (events.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(noEventsToExport)
                        }
                        return@launch
                    }
                    val ics = danilovl.calendar.util.IcsGenerator.generate(events)
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(ics.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, exportSuccess, android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    AppLog.e("SettingsScreen", "Operation failed", e)
                }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
                    if (json != null) {
                        val events = danilovl.calendar.util.EventBackupUtils.importFromJson(json)
                        viewModel.importEvents(events)
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, importSuccess, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    AppLog.e("SettingsScreen", "Operation failed", e)
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, importError, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val importIcsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
                    if (content != null) {
                        val events = danilovl.calendar.util.IcsParser.parse(content)
                        viewModel.importEvents(events)
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, importSuccess, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    AppLog.e("SettingsScreen", "Operation failed", e)
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, importError, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    var choiceScreen by remember { mutableStateOf<ChoiceScreenSpec?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showAccounts by rememberSaveable { mutableStateOf(false) }
    var showWidgetRangeDialog by rememberSaveable { mutableStateOf(false) }
    var showCountryPicker by rememberSaveable { mutableStateOf(false) }
    var showAdditionalCountryPicker by rememberSaveable { mutableStateOf(false) }
    var showLanguagePicker by rememberSaveable { mutableStateOf(false) }
    var showEventColors by rememberSaveable { mutableStateOf(false) }
    var showExternalCalendars by rememberSaveable { mutableStateOf(false) }
    var showAllDayTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDefaultReminderPicker by rememberSaveable { mutableStateOf(false) }

    if (showDefaultReminderPicker) {
        danilovl.calendar.ui.components.ReminderWheelPickerDialog(
            initialOffset = settings.defaultReminder,
            onSelect = { v ->
                viewModel.update { it.copy(defaultReminder = v) }
                showDefaultReminderPicker = false
            },
            onDismiss = { showDefaultReminderPicker = false },
            onNone = {
                viewModel.update { it.copy(defaultReminder = "none") }
                showDefaultReminderPicker = false
            }
        )
    }

    if (showEventColors) {
        EventColorsScreen(
            settings = settings,
            onUpdate = { viewModel.update(it) },
            onBack = { showEventColors = false }
        )
        return
    }

    if (showExternalCalendars) {
        ExternalCalendarsScreen(
            viewModel = viewModel,
            onBack = { showExternalCalendars = false }
        )
        return
    }

    if (showLanguagePicker) {
        LanguagePickerScreen(
            selectedCode = settings.language,
            onSelect = { code ->
                showLanguagePicker = false
                if (code != settings.language) {
                    viewModel.update { it.copy(language = code) }
                    (context as? Activity)?.recreate()
                }
            },
            onBack = { showLanguagePicker = false }
        )
        return
    }

    val holidayRepo = remember { HolidayRepository(context) }
    var countries by remember { mutableStateOf(HolidayCountries.staticCountries) }
    LaunchedEffect(Unit) {
        runCatching { holidayRepo.getCountries() }.getOrNull()?.let { if (it.isNotEmpty()) countries = it }
    }
    val holidayCountryName = countries.find { it.code == settings.holidayCountry }?.name
        ?: HolidayCountries.displayName(context, settings.holidayCountry)

    if (showCountryPicker) {
        CountryPickerScreen(
            countries = countries,
            selectedCode = settings.holidayCountry,
            onSelect = { code ->
                viewModel.update { s ->
                    s.copy(
                        holidayCountry = code,
                        additionalHolidayCountries = s.additionalHolidayCountries.filter { it != code }
                    )
                }
                showCountryPicker = false
            },
            onBack = { showCountryPicker = false }
        )
        return
    }

    if (showAdditionalCountryPicker) {
        MultiCountryPickerScreen(
            countries = countries,
            selectedCodes = settings.additionalHolidayCountries,
            excludedCode = settings.holidayCountry,
            onSelectionChange = { codes ->
                viewModel.update { s -> s.copy(additionalHolidayCountries = codes) }
            },
            onBack = { showAdditionalCountryPicker = false }
        )
        return
    }

    choiceScreen?.let { spec ->
        SingleChoiceScreen(
            title = spec.title,
            options = spec.options,
            selected = spec.selected,
            onSelect = { spec.onSelect(it); choiceScreen = null },
            onBack = { choiceScreen = null }
        )
        return
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.settings_about)) },
            text = { Text(stringResource(R.string.about_text)) },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.common_ok)) } }
        )
    }

    if (showAccounts) {
        showExternalCalendars = true
        showAccounts = false
    }

    if (showAllDayTimePicker) {
        val parts = settings.allDayReminderTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        WheelDateTimePickerDialog(
            initialDate = LocalDate.now(),
            initialTime = LocalTime.of(hour, minute),
            showDate = false,
            showTime = true,
            onConfirm = { _, time ->
                viewModel.update { it.copy(allDayReminderTime = String.format(Locale.getDefault(), "%02d:%02d", time.hour, time.minute)) }
                showAllDayTimePicker = false
            },
            onDismiss = { showAllDayTimePicker = false }
        )
    }

    if (showWidgetRangeDialog) {
        WidgetRangeDialog(
            currentDays = settings.widgetRangeDays,
            onConfirm = { days ->
                viewModel.update { it.copy(widgetRangeDays = days) }
                showWidgetRangeDialog = false
            },
            onDismiss = { showWidgetRangeDialog = false }
        )
    }

    fun showTimePicker() {
        showAllDayTimePicker = true
    }

    val weekStartTitle = stringResource(R.string.settings_week_start)
    val otherCalendarsTitle = stringResource(R.string.settings_other_calendars)
    val reminderMelodyTitle = stringResource(R.string.settings_reminder_melody_title)
    val eventTzTitle = stringResource(R.string.settings_event_tz_title)
    val languageDisplay = if (settings.language == AppLanguages.SYSTEM)
        stringResource(R.string.language_system) else AppLanguages.nativeName(settings.language)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = XiaomiBg)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = XiaomiBg
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
        ) {
            SettingsGroup {
                NavigationItem(stringResource(R.string.settings_accounts_manage)) { showAccounts = true }
                ItemDivider()
                SelectorItem(stringResource(R.string.settings_language), languageDisplay) {
                    showLanguagePicker = true
                }
            }

            SettingsCategory(stringResource(R.string.settings_cat_month_views))
            SettingsGroup {
                SelectorItem(stringResource(R.string.settings_week_start), settingLabel(settings.weekStart)) {
                    choiceScreen = ChoiceScreenSpec(weekStartTitle, SettingsOptions.weekStarts, settings.weekStart) { v ->
                        viewModel.update { it.copy(weekStart = v) }
                    }
                }
                ItemDivider()
                SwitchItem(stringResource(R.string.settings_expanded_month), settings.expandedMonth) { v ->
                    viewModel.update { it.copy(expandedMonth = v) }
                }
                ItemDivider()
                SwitchItem(stringResource(R.string.settings_show_week_number), settings.showWeekNumber) { v ->
                    viewModel.update { it.copy(showWeekNumber = v) }
                }
            }

            SettingsCategory(stringResource(R.string.settings_cat_features))
            SettingsGroup {
                SwitchItem(stringResource(R.string.settings_chinese_almanac), settings.chineseAlmanac) { v ->
                    viewModel.update { it.copy(chineseAlmanac = v) }
                }
                ItemDivider()
                SwitchItem(stringResource(R.string.settings_intl_holidays), settings.internationalHolidays) { v ->
                    viewModel.update { it.copy(internationalHolidays = v) }
                }
                ItemDivider()
                SelectorItem(stringResource(R.string.settings_holiday_country), holidayCountryName) {
                    showCountryPicker = true
                }
                settings.additionalHolidayCountries.forEach { code ->
                    val name = countries.find { it.code == code }?.name ?: HolidayCountries.displayName(context, code)
                    ItemDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, modifier = Modifier.weight(1f), fontSize = 15.sp, color = XiaomiTextPrimary)
                        IconButton(onClick = {
                            viewModel.update { s -> s.copy(additionalHolidayCountries = s.additionalHolidayCountries - code) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                ItemDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdditionalCountryPicker = true }
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = XiaomiBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.settings_add_holiday_country), color = XiaomiBlue, fontSize = 15.sp)
                }
                ItemDivider()
                SelectorItem(stringResource(R.string.settings_other_calendars), settingLabel(settings.otherCalendar)) {
                    choiceScreen = ChoiceScreenSpec(otherCalendarsTitle, SettingsOptions.otherCalendars, settings.otherCalendar) { v ->
                        viewModel.update { it.copy(otherCalendar = v) }
                    }
                }
            }

            SettingsCategory(stringResource(R.string.settings_cat_reminders))
            SettingsGroup {
                SwitchItem(
                    title = stringResource(R.string.settings_notifications_enabled),
                    checked = notificationsEnabled,
                    onCheckedChange = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                )
                if (!notificationsEnabled) {
                    Text(
                        text = stringResource(R.string.settings_notifications_disabled_desc),
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 28.dp, bottom = 8.dp)
                    )
                }
                ItemDivider()
                NavigationItem(
                    title = stringResource(R.string.settings_reminder_item),
                    value = settingLabel(settings.reminderMelody),
                    enabled = notificationsEnabled
                ) {
                    choiceScreen = ChoiceScreenSpec(reminderMelodyTitle, SettingsOptions.melodies, settings.reminderMelody) { v ->
                        viewModel.update { it.copy(reminderMelody = v) }
                    }
                }
                ItemDivider()
                SelectorItem(
                    title = stringResource(R.string.settings_reminder_time),
                    value = settingLabel(settings.defaultReminder),
                    enabled = notificationsEnabled
                ) {
                    showDefaultReminderPicker = true
                }
                ItemDivider()
                NavigationItem(
                    title = stringResource(R.string.settings_reminder_time_allday),
                    value = settings.allDayReminderTime,
                    enabled = notificationsEnabled
                ) { showTimePicker() }
            }

            SettingsCategory(stringResource(R.string.settings_colors_title))
            SettingsGroup {
                NavigationItem(stringResource(R.string.settings_colors_title)) { showEventColors = true }
            }

            SettingsCategory(stringResource(R.string.settings_import_export))
            SettingsGroup {
                NavigationItem(stringResource(R.string.settings_export_json)) {
                    exportFormat = "json"
                    showExportDialog = true
                }
                ItemDivider()
                NavigationItem(stringResource(R.string.settings_export_ics)) {
                    exportFormat = "ics"
                    showExportDialog = true
                }
                ItemDivider()
                NavigationItem(stringResource(R.string.settings_import_json)) {
                    importJsonLauncher.launch(arrayOf("application/json"))
                }
                ItemDivider()
                NavigationItem(stringResource(R.string.action_import_ics)) {
                    importIcsLauncher.launch(arrayOf("text/calendar"))
                }
            }

            SettingsCategory(stringResource(R.string.settings_cat_other))
            SettingsGroup {
                SwitchItem(stringResource(R.string.settings_import_birthdays), settings.importContactBirthdays) { v ->
                    viewModel.update { it.copy(importContactBirthdays = v) }
                }
                if (settings.importContactBirthdays) {
                    ItemDivider()
                    NavigationItem(
                        title = stringResource(R.string.settings_sync_birthdays_now),
                        subtitle = formatLastSync(settings.lastBirthdaysSync)
                    ) {
                        viewModel.syncBirthdays()
                    }
                }
                ItemDivider()
                SelectorItem(stringResource(R.string.settings_widget_range), formatDays(settings.widgetRangeDays)) {
                    showWidgetRangeDialog = true
                }
                ItemDivider()
                NavigationItem(stringResource(R.string.settings_event_tz), settings.eventTimeZone) {
                    choiceScreen = ChoiceScreenSpec(eventTzTitle, SettingsOptions.timeZones, settings.eventTimeZone) { v ->
                        viewModel.update { it.copy(eventTimeZone = v) }
                    }
                }
                ItemDivider()
                NavigationItem(stringResource(R.string.settings_about)) { showAbout = true }
            }
        }
    }

    if (showExportDialog) {
        ExportTypeDialog(
            selectedTypes = selectedTypes,
            onSelectionChange = { selectedTypes = it },
            onConfirm = {
                showExportDialog = false
                if (exportFormat == "json") {
                    exportJsonLauncher.launch("calendar_backup_${System.currentTimeMillis()}.json")
                } else if (exportFormat == "ics") {
                    exportIcsLauncher.launch("calendar_export_${System.currentTimeMillis()}.ics")
                }
            },
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun ExportTypeDialog(
    selectedTypes: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val types = listOf(
        "event" to stringResource(R.string.type_event),
        "birthday" to stringResource(R.string.type_birthday),
        "anniversary" to stringResource(R.string.type_anniversary),
        "reminder" to stringResource(R.string.type_reminder)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_export_type_selection), fontWeight = FontWeight.Medium) },
        text = {
            Column {
                types.forEach { (type, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (type in selectedTypes) {
                                    onSelectionChange(selectedTypes - type)
                                } else {
                                    onSelectionChange(selectedTypes + type)
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = type in selectedTypes,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = XiaomiBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedTypes.isNotEmpty()
            ) {
                Text(stringResource(R.string.settings_export_action), color = XiaomiBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = XiaomiBlue)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventColorsScreen(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var showColorPickerType by remember { mutableStateOf<String?>(null) }

    showColorPickerType?.let { type ->
        val currentColor = when (type) {
            "event" -> settings.eventColor
            "birthday" -> settings.birthdayColor
            "anniversary" -> settings.anniversaryColor
            "reminder" -> settings.reminderColor
            else -> settings.eventColor
        }
        ColorPickerDialog(
            currentColor = currentColor,
            onSelect = { color ->
                if (color != null) {
                    onUpdate {
                        when (type) {
                            "event" -> it.copy(eventColor = color)
                            "birthday" -> it.copy(birthdayColor = color)
                            "anniversary" -> it.copy(anniversaryColor = color)
                            "reminder" -> it.copy(reminderColor = color)
                            else -> it
                        }
                    }
                }
                showColorPickerType = null
            },
            onDismiss = { showColorPickerType = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_colors_title), fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
        ) {
            SettingsGroup {
                ColorItem(stringResource(R.string.type_event), settings.eventColor) { showColorPickerType = "event" }
                ItemDivider()
                ColorItem(stringResource(R.string.type_birthday), settings.birthdayColor) { showColorPickerType = "birthday" }
                ItemDivider()
                ColorItem(stringResource(R.string.type_anniversary), settings.anniversaryColor) { showColorPickerType = "anniversary" }
                ItemDivider()
                ColorItem(stringResource(R.string.type_reminder), settings.reminderColor) { showColorPickerType = "reminder" }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalCalendarsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    var calendars by remember { mutableStateOf<List<ExternalCalendar>>(emptyList()) }
    var hasPermission by remember { 
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.READ_CALENDAR
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    val icsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                if (content != null) {
                    val events = danilovl.calendar.util.IcsParser.parse(content)
                    viewModel.importEvents(events)
                    onBack()
                }
            } catch (e: Exception) {
                AppLog.e("SettingsScreen", "Operation failed", e)
            }
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            calendars = viewModel.getAvailableCalendars()
        } else {
            permissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.external_calendars_title), fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { icsLauncher.launch("text/calendar") }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_import_ics))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = XiaomiBg)
            )
        },
        containerColor = XiaomiBg
    ) { padding ->
        if (!hasPermission) {
            Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.permission_calendar_denied), 
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = XiaomiTextSecondary
                )
            }
        } else if (calendars.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.no_external_calendars),
                    color = XiaomiTextSecondary
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                val grouped = calendars.groupBy { it.accountName }
                grouped.forEach { (account, accountCalendars) ->
                    item {
                        Text(
                            text = account,
                            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = XiaomiTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(accountCalendars) { cal ->
                        val isEnabled = settings.enabledCalendars.contains(cal.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.update {
                                        val newSet = it.enabledCalendars.toMutableSet()
                                        if (isEnabled) newSet.remove(cal.id) else newSet.add(cal.id)
                                        it.copy(enabledCalendars = newSet)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(cal.color))
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                cal.name, 
                                modifier = Modifier.weight(1f),
                                color = XiaomiTextPrimary
                            )
                            Checkbox(
                                checked = isEnabled,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = XiaomiBlue)
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

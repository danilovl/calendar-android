package danilovl.calendar.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import danilovl.calendar.R
import danilovl.calendar.data.AppLanguages
import danilovl.calendar.data.remote.Country
import danilovl.calendar.ui.formatDays
import danilovl.calendar.ui.theme.XiaomiBg
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiSurface
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.util.RegexPatterns

data class ChoiceScreenSpec(
    val title: String,
    val options: List<String>,
    val selected: String,
    val onSelect: (String) -> Unit
)

@Composable
fun settingLabel(key: String): String {
    val resId = when (key) {
        "monday" -> R.string.option_monday
        "sunday" -> R.string.option_sunday
        "none" -> R.string.option_none
        "chinese" -> R.string.option_chinese
        "islamic" -> R.string.option_islamic
        "hebrew" -> R.string.option_hebrew
        "10_min" -> R.string.remind_10_min
        "15_min" -> R.string.remind_15_min
        "30_min" -> R.string.remind_30_min
        "1_hour" -> R.string.remind_1_hour
        "1_day" -> R.string.remind_1_day
        "2_days" -> R.string.remind_2_days
        "1_week" -> R.string.remind_1_week
        "day_before_20" -> R.string.remind_day_before_20
        "default" -> R.string.melody_default
        "quiet" -> R.string.melody_quiet
        "energetic" -> R.string.melody_energetic
        "melodic" -> R.string.melody_melodic
        "daily" -> R.string.repeat_daily
        "weekly" -> R.string.repeat_weekly
        "monthly" -> R.string.repeat_monthly
        "yearly" -> R.string.repeat_yearly
        "custom" -> R.string.repeat_custom
        else -> null
    }
    if (resId != null) return stringResource(resId)

    val num = RegexPatterns.LEAD_NUMBER.find(key)?.value
    if (num != null) {
        val before = stringResource(R.string.remind_before_prefix)
        val unit = when {
            key.contains("_min") -> stringResource(R.string.unit_min_short)
            key.contains("_hour") -> stringResource(R.string.unit_hours_short)
            key.contains("_day") -> stringResource(R.string.unit_days_short)
            key.contains("_week") -> stringResource(R.string.unit_weeks_short)
            else -> ""
        }
        return if (before.isNotEmpty()) "$before $num $unit" else "$num $unit"
    }

    return key
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = XiaomiSurface
    ) {
        Column(content = content)
    }
}

@Composable
fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = XiaomiBg
    )
}

@Composable
fun SwitchItem(title: String, checked: Boolean, enabled: Boolean = true, verticalPadding: androidx.compose.ui.unit.Dp = 8.dp, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(start = 16.dp, end = 8.dp, top = verticalPadding, bottom = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val textColor = if (enabled) XiaomiTextPrimary else XiaomiTextSecondary
        Text(
            text = title, 
            fontSize = 16.sp, 
            color = textColor, 
            modifier = Modifier.weight(1f),
            maxLines = 3
        )
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedThumbColor = XiaomiSurface, checkedTrackColor = XiaomiBlue),
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
fun SelectorItem(title: String, value: String, enabled: Boolean = true, verticalPadding: androidx.compose.ui.unit.Dp = 12.dp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val textColor = if (enabled) XiaomiTextPrimary else XiaomiTextSecondary
        Text(
            text = title, 
            fontSize = 16.sp, 
            color = textColor, 
            modifier = Modifier.weight(1f),
            maxLines = 3
        )
        Text(
            text = value, 
            fontSize = 15.sp, 
            color = XiaomiTextSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.UnfoldMore, contentDescription = null, tint = XiaomiTextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun NavigationItem(title: String, value: String = "", subtitle: String? = null, enabled: Boolean = true, verticalPadding: androidx.compose.ui.unit.Dp = 12.dp, onClick: () -> Unit) {
    val actualPadding = if (subtitle != null) 4.dp else verticalPadding
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = actualPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val textColor = if (enabled) XiaomiTextPrimary else XiaomiTextSecondary
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, 
                fontSize = 16.sp, 
                color = textColor, 
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = XiaomiTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
        if (value.isNotEmpty()) {
            Text(
                text = value, 
                fontSize = 15.sp, 
                color = XiaomiTextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(4.dp))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = XiaomiTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ColorItem(title: String, colorInt: Int, verticalPadding: androidx.compose.ui.unit.Dp = 12.dp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title, 
            fontSize = 16.sp, 
            color = XiaomiTextPrimary, 
            modifier = Modifier.weight(1f),
            maxLines = 3
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(androidx.compose.ui.graphics.Color(colorInt))
                .border(1.dp, androidx.compose.ui.graphics.Color.LightGray, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = XiaomiTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = XiaomiTextSecondary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, top = 10.dp, bottom = 4.dp, end = 16.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleChoiceScreen(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Medium) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsGroup {
                options.forEachIndexed { index, option ->
                    if (index > 0) ItemDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, onClick = { onSelect(option) })
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(settingLabel(option), fontSize = 16.sp, color = XiaomiTextPrimary, modifier = Modifier.weight(1f))
                        if (option == selected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = XiaomiBlue, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerScreen(
    countries: List<Country>,
    selectedCode: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(countries, query) {
        if (query.isBlank()) countries
        else countries.filter { it.name.contains(query.trim(), ignoreCase = true) || it.code.contains(query.trim(), ignoreCase = true) }
    }

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_holiday_country), fontWeight = FontWeight.Medium) },
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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.country_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = XiaomiTextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = XiaomiSurface,
                    unfocusedContainerColor = XiaomiSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.common_not_found), color = XiaomiTextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    items(filtered, key = { it.code }) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = country.code == selectedCode, onClick = { onSelect(country.code) })
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(country.name, fontSize = 16.sp, color = XiaomiTextPrimary, modifier = Modifier.weight(1f))
                            if (country.code == selectedCode) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = XiaomiBlue, modifier = Modifier.size(22.dp))
                            }
                        }
                        HorizontalDivider(color = XiaomiBg, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiCountryPickerScreen(
    countries: List<Country>,
    selectedCodes: List<String>,
    excludedCode: String,
    onSelectionChange: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(countries, query, excludedCode) {
        val base = countries.filter { it.code != excludedCode }
        if (query.isBlank()) base
        else base.filter { it.name.contains(query.trim(), ignoreCase = true) || it.code.contains(query.trim(), ignoreCase = true) }
    }

    var currentSelection by remember { mutableStateOf(selectedCodes.toSet()) }

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_additional_countries), fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onSelectionChange(currentSelection.toList())
                        onBack()
                    }) {
                        Text(stringResource(R.string.common_ok), color = XiaomiBlue, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = XiaomiBg)
            )
        },
        containerColor = XiaomiBg
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.country_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = XiaomiTextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = XiaomiSurface,
                    unfocusedContainerColor = XiaomiSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.common_not_found), color = XiaomiTextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    items(filtered, key = { it.code }) { country ->
                        val isSelected = currentSelection.contains(country.code)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentSelection = if (isSelected) {
                                        currentSelection - country.code
                                    } else {
                                        currentSelection + country.code
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(country.name, fontSize = 16.sp, color = XiaomiTextPrimary, modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = XiaomiBlue)
                            )
                        }
                        HorizontalDivider(color = XiaomiBg, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerScreen(
    selectedCode: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    data class LangRow(val code: String, val label: String)
    val rows = listOf(LangRow(AppLanguages.SYSTEM, stringResource(R.string.language_system))) +
        AppLanguages.supported.map { LangRow(it.code, it.nativeName) }

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_language), fontWeight = FontWeight.Medium) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsGroup {
                rows.forEachIndexed { index, row ->
                    if (index > 0) ItemDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = row.code == selectedCode, onClick = { onSelect(row.code) })
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(row.label, fontSize = 16.sp, color = XiaomiTextPrimary, modifier = Modifier.weight(1f))
                        if (row.code == selectedCode) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = XiaomiBlue, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetRangeDialog(
    currentDays: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val maxDays = 90
    val items = (1..maxDays).map { formatDays(it) }
    var selectedIndex by remember { mutableIntStateOf((currentDays.coerceIn(1, maxDays) - 1)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = XiaomiSurface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.settings_widget_range),
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = XiaomiTextPrimary
                )
                Spacer(Modifier.height(16.dp))
                WheelPicker(
                    items = items,
                    initialIndex = selectedIndex,
                    onItemSelected = { selectedIndex = items.indexOf(it) }
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(selectedIndex + 1) }) {
                        Text(stringResource(R.string.common_ok), color = XiaomiBlue)
                    }
                }
            }
        }
    }
}

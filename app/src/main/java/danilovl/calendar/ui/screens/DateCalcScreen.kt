package danilovl.calendar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import danilovl.calendar.R
import danilovl.calendar.ui.appLocale
import danilovl.calendar.ui.components.WheelDateTimePickerDialog
import danilovl.calendar.ui.theme.XiaomiBg
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiSurface
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateCalcScreen(initialDate: LocalDate, onBack: () -> Unit) {
    val locale = appLocale()
    val dateFormatPattern = stringResource(R.string.datecalc_date_format)
    val fmt = remember(dateFormatPattern, locale) { DateTimeFormatter.ofPattern(dateFormatPattern, locale) }

    var startDate by remember { mutableStateOf(initialDate) }
    var endDate by remember { mutableStateOf(initialDate.plusDays(7)) }
    var baseDate by remember { mutableStateOf(initialDate) }
    var deltaText by remember { mutableStateOf("30") }
    var add by remember { mutableStateOf(true) }
    var picker by remember { mutableStateOf<Int?>(null) }

    picker?.let { idx ->
        val current = when (idx) { 0 -> startDate; 1 -> endDate; else -> baseDate }
        WheelDateTimePickerDialog(
            initialDate = current,
            initialTime = LocalTime.MIN,
            showTime = false,
            onConfirm = { date, _ ->
                when (idx) { 0 -> startDate = date; 1 -> endDate = date; else -> baseDate = date }
                picker = null
            },
            onDismiss = { picker = null }
        )
    }

    val daysBetween = ChronoUnit.DAYS.between(startDate, endDate)
    val delta = deltaText.toLongOrNull() ?: 0L
    val resultDate = baseDate.plusDays(if (add) delta else -delta)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.datecalc_title), fontWeight = FontWeight.Medium) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(stringResource(R.string.datecalc_section_diff)) {
                DateRow(stringResource(R.string.label_start), startDate.format(fmt)) { picker = 0 }
                HorizontalDivider(color = XiaomiBg)
                DateRow(stringResource(R.string.label_end), endDate.format(fmt)) { picker = 1 }
                HorizontalDivider(color = XiaomiBg)
                ResultRow(stringResource(R.string.datecalc_label_diff), "${abs(daysBetween)} ${stringResource(R.string.unit_days_short)}")
            }

            SectionCard(stringResource(R.string.datecalc_section_add_sub)) {
                DateRow(stringResource(R.string.datecalc_label_date), baseDate.format(fmt)) { picker = 2 }
                HorizontalDivider(color = XiaomiBg)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = add,
                        onClick = { add = true },
                        label = { Text(stringResource(R.string.datecalc_add)) },
                        leadingIcon = if (add) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) } } else null
                    )
                    FilterChip(
                        selected = !add,
                        onClick = { add = false },
                        label = { Text(stringResource(R.string.datecalc_subtract)) },
                        leadingIcon = if (!add) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) } } else null
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = {
                            val v = deltaText.toLongOrNull() ?: 0L
                            if (v > 0) deltaText = (v - 1).toString()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = XiaomiBg,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.datecalc_decrease), tint = XiaomiBlue)
                        }
                    }

                    OutlinedTextField(
                        value = deltaText,
                        onValueChange = { s -> deltaText = s.filter { it.isDigit() }.take(5) },
                        label = { Text(stringResource(R.string.datecalc_days_count)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        onClick = {
                            val v = deltaText.toLongOrNull() ?: 0L
                            deltaText = (v + 1).toString()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = XiaomiBg,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.datecalc_increase), tint = XiaomiBlue)
                        }
                    }
                }
                HorizontalDivider(color = XiaomiBg)
                ResultRow(stringResource(R.string.datecalc_result), resultDate.format(fmt))
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = XiaomiBlue, modifier = Modifier.padding(start = 4.dp))
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = XiaomiSurface
    ) {
        Column(content = content)
    }
}

@Composable
private fun DateRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, color = XiaomiTextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = XiaomiTextSecondary)
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = XiaomiTextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = XiaomiBlue)
    }
}


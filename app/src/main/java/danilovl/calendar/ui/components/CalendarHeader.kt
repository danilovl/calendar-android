package danilovl.calendar.ui.components

import android.icu.text.RelativeDateTimeFormatter
import android.icu.text.RelativeDateTimeFormatter.AbsoluteUnit
import android.icu.text.RelativeDateTimeFormatter.Direction
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit
import android.icu.util.ULocale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import danilovl.calendar.R
import danilovl.calendar.ui.CalendarViewMode
import danilovl.calendar.ui.appLocale
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.ui.trans
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    viewMode: CalendarViewMode,
    onTodayClick: () -> Unit,
    onAddEventClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onGoToDateClick: () -> Unit = {},
    onDateCalcClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    isTodayHighlighted: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val title = buildAnnotatedString {
        if (viewMode == CalendarViewMode.YEAR) {
            withStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Normal)) {
                append(selectedDate.year.toString())
            }
        } else {
            val month = if (viewMode == CalendarViewMode.MONTH) currentMonth.monthValue else selectedDate.monthValue
            val year = if (viewMode == CalendarViewMode.MONTH) currentMonth.year else selectedDate.year
            withStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Light)) {
                append("$month . ")
            }
            withStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Light)) {
                append(year.toString())
            }
        }
    }

    val locale = appLocale()
    val subtitle = if (viewMode == CalendarViewMode.MONTH) {
        val daysDiff = ChronoUnit.DAYS.between(LocalDate.now(), selectedDate)
        val rdtf = RelativeDateTimeFormatter.getInstance(ULocale.forLocale(locale))
        when {
            daysDiff > 0 -> rdtf.format(
                daysDiff.toDouble(),
                Direction.NEXT,
                RelativeUnit.DAYS
            )
            daysDiff < 0 -> rdtf.format(
                (-daysDiff).toDouble(),
                Direction.LAST,
                RelativeUnit.DAYS
            )
            else -> rdtf.format(
                Direction.THIS,
                AbsoluteUnit.DAY
            )
        }.replaceFirstChar { it.titlecase(locale) }
    } else null

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = XiaomiTextPrimary,
                    lineHeight = 36.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = XiaomiTextSecondary,
                        modifier = Modifier.padding(top = 12.dp),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onTodayClick) {
                Icon(
                    Icons.Default.Today,
                    contentDescription = trans(R.string.action_today),
                    tint = if (isTodayHighlighted) XiaomiBlue else XiaomiTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onAddEventClick) {
                Icon(Icons.Default.Add, contentDescription = trans(R.string.action_add), tint = XiaomiTextPrimary, modifier = Modifier.size(24.dp))
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = trans(R.string.action_menu), tint = XiaomiTextPrimary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(trans(R.string.action_search)) },
                        onClick = { onSearchClick(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(trans(R.string.action_go_to_date)) },
                        onClick = { onGoToDateClick(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(trans(R.string.action_calc_date)) },
                        onClick = { onDateCalcClick(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(trans(R.string.action_share)) },
                        onClick = { onShareClick(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(trans(R.string.action_settings)) },
                        onClick = { onSettingsClick(); showMenu = false }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

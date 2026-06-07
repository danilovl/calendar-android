package danilovl.calendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import danilovl.calendar.R
import danilovl.calendar.ui.appLocale
import danilovl.calendar.ui.theme.XiaomiBg
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiSurface
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.ui.trans
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle

@Composable
fun WheelDateTimePickerDialog(
    initialDate: LocalDate,
    initialTime: LocalTime,
    showDate: Boolean = true,
    showTime: Boolean = true,
    onConfirm: (LocalDate, LocalTime) -> Unit,
    onDismiss: () -> Unit,
    onNone: (() -> Unit)? = null
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var selectedTime by remember { mutableStateOf(initialTime) }
    val locale = appLocale()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = XiaomiSurface,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val title = when {
                    showDate && showTime -> trans(R.string.select_date_time)
                    showDate -> trans(R.string.select_date)
                    else -> trans(R.string.select_time)
                }
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = XiaomiTextPrimary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showDate) {
                        val days = remember(selectedDate.year, selectedDate.monthValue) { (1..selectedDate.lengthOfMonth()).toList() }
                        WheelPicker(
                            items = days,
                            initialIndex = selectedDate.dayOfMonth - 1,
                            modifier = Modifier.weight(1f),
                            onItemSelected = { selectedDate = selectedDate.withDayOfMonth(it) }
                        )

                        val months = remember { (1..12).toList() }
                        WheelPicker(
                            items = months,
                            initialIndex = selectedDate.monthValue - 1,
                            modifier = Modifier.weight(2f),
                            format = { month ->
                                LocalDate.of(2000, month, 1)
                                    .month.getDisplayName(TextStyle.FULL, locale)
                            },
                            onItemSelected = {
                                val newMonth = it
                                val maxDays = LocalDate.of(selectedDate.year, newMonth, 1).lengthOfMonth()
                                val newDay = selectedDate.dayOfMonth.coerceAtMost(maxDays)
                                selectedDate = selectedDate.withMonth(newMonth).withDayOfMonth(newDay)
                            }
                        )

                        val years = remember(initialDate.year) { (initialDate.year - 10..initialDate.year + 50).toList() }
                        WheelPicker(
                            items = years,
                            initialIndex = years.indexOf(selectedDate.year),
                            modifier = Modifier.weight(1.5f),
                            onItemSelected = {
                                val newYear = it
                                val maxDays = LocalDate.of(newYear, selectedDate.monthValue, 1).lengthOfMonth()
                                val newDay = selectedDate.dayOfMonth.coerceAtMost(maxDays)
                                selectedDate = selectedDate.withYear(newYear).withDayOfMonth(newDay)
                            }
                        )
                    }

                    if (showTime) {
                        if (showDate) {
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.width(1.dp).fillMaxHeight(0.6f).background(XiaomiBg))
                            Spacer(Modifier.width(8.dp))
                        }

                        val hours = remember { (0..23).toList() }
                        WheelPicker(
                            items = hours,
                            initialIndex = selectedTime.hour,
                            modifier = Modifier.weight(1f),
                            format = { String.format(locale, "%02d", it) },
                            onItemSelected = { selectedTime = selectedTime.withHour(it) }
                        )

                        val minutes = remember { (0..59).toList() }
                        WheelPicker(
                            items = minutes,
                            initialIndex = selectedTime.minute,
                            modifier = Modifier.weight(1f),
                            format = { String.format(locale, "%02d", it) },
                            onItemSelected = { selectedTime = selectedTime.withMinute(it) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onNone != null) {
                        TextButton(onClick = onNone) {
                            Text(trans(R.string.detail_repeat_never), color = XiaomiBlue)
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    TextButton(onClick = onDismiss) {
                        Text(trans(R.string.common_cancel), color = XiaomiTextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(selectedDate, selectedTime) }) {
                        Text(trans(R.string.common_ok), color = XiaomiBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialIndex: Int,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    format: (T) -> String = { it.toString() }
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val snappedIndex = listState.firstVisibleItemIndex
            if (snappedIndex in items.indices) {
                onItemSelected(items[snappedIndex])
            }
        }
    }

    Box(modifier = modifier.height(200.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(XiaomiBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected by remember {
                    derivedStateOf { listState.firstVisibleItemIndex == index }
                }
                Box(
                    modifier = Modifier.height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(item),
                        fontSize = if (isSelected) 18.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) XiaomiBlue else XiaomiTextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

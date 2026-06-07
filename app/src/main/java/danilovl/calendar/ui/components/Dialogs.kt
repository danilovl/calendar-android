package danilovl.calendar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import danilovl.calendar.R
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiSurface
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.ui.trans
import danilovl.calendar.util.RegexPatterns
import java.util.Locale

val eventColors = listOf(
    0xFF1677FF,
    0xFFFF4D4D,
    0xFFFF9800,
    0xFF4CAF50,
    0xFF9C27B0,
    0xFFE91E63,
    0xFF00BCD4,
    0xFF795548,
    0xFF607D8B
)

@Composable
fun ThemedDialog(
    onDismiss: () -> Unit,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = XiaomiSurface,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = horizontalAlignment,
                content = content
            )
        }
    }
}

@Composable
fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ThemedDialog(onDismiss = onDismiss) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = XiaomiTextPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn {
            items(options) { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = settingLabel(option),
                        fontSize = 16.sp,
                        color = if (option == selected) XiaomiBlue else XiaomiTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (option == selected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = XiaomiBlue)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text(trans(R.string.common_cancel), color = XiaomiTextSecondary)
            }
        }
    }
}

@Composable
fun ReminderWheelPickerDialog(
    initialOffset: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onNone: () -> Unit
) {
    val initialNum = (RegexPatterns.LEAD_NUMBER.find(initialOffset)?.value?.toInt() ?: 10).coerceIn(1, 60)
    val initialUnit = when {
        initialOffset.contains("_week") -> 3
        initialOffset.contains("_day") -> 2
        initialOffset.contains("_hour") -> 1
        else -> 0
    }

    var selectedNum by remember { mutableIntStateOf(initialNum) }
    var selectedUnitIndex by remember { mutableIntStateOf(initialUnit) }

    val units = listOf(
        stringResource(R.string.unit_min_short),
        stringResource(R.string.unit_hours_short),
        stringResource(R.string.unit_days_short),
        stringResource(R.string.unit_weeks_short)
    )
    val unitKeys = listOf("_min", "_hour", "_day", "_week")

    ThemedDialog(onDismiss = onDismiss, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = trans(R.string.settings_reminder_item),
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
                    WheelPicker(
                        items = (1..60).toList(),
                        initialIndex = initialNum - 1,
                        modifier = Modifier.weight(1f),
                        onItemSelected = { selectedNum = it }
                    )

                    WheelPicker(
                        items = units,
                        initialIndex = initialUnit,
                        modifier = Modifier.weight(1f),
                        onItemSelected = { selectedUnitIndex = units.indexOf(it) }
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onNone) {
                        Text(trans(R.string.reminder_off), color = XiaomiBlue)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(trans(R.string.common_cancel), color = XiaomiTextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        onSelect("${selectedNum}${unitKeys[selectedUnitIndex]}")
                    }) {
                        Text(trans(R.string.common_ok), color = XiaomiBlue, fontWeight = FontWeight.Bold)
                    }
                }
    }
}


@Composable
fun RepeatDialog(
    currentRepeat: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustom by remember { mutableStateOf(false) }
    
    if (showCustom) {
        RepeatWheelPickerDialog(
            initialRepeat = if (currentRepeat.contains("_")) currentRepeat else "1_day",
            onSelect = { onSelect(it); showCustom = false },
            onDismiss = { showCustom = false }
        )
    }

    val options = listOf("none", "daily", "weekly", "monthly", "yearly", "custom")
    ChoiceDialog(
        title = trans(R.string.detail_repeat),
        options = options,
        selected = if (options.contains(currentRepeat)) currentRepeat else "custom",
        onSelect = {
            if (it == "custom") {
                showCustom = true
            } else {
                onSelect(it)
            }
        },
        onDismiss = onDismiss
    )
}

@Composable
fun RepeatWheelPickerDialog(
    initialRepeat: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val resources = context.resources
    val initialNum = (RegexPatterns.LEAD_NUMBER.find(initialRepeat)?.value?.toInt() ?: 1).coerceIn(1, 99)
    val initialUnit = when {
        initialRepeat.contains("_year") -> 3
        initialRepeat.contains("_month") -> 2
        initialRepeat.contains("_week") -> 1
        else -> 0
    }

    var selectedNum by remember { mutableIntStateOf(initialNum) }
    var selectedUnitIndex by remember { mutableIntStateOf(initialUnit) }

    val unitKeys = listOf("_day", "_week", "_month", "_year")
    val pluralResources = listOf(
        Triple(R.string.repeat_days_one, R.string.repeat_days_few, R.string.repeat_days_many),
        Triple(R.string.repeat_weeks_one, R.string.repeat_weeks_few, R.string.repeat_weeks_many),
        Triple(R.string.repeat_months_one, R.string.repeat_months_few, R.string.repeat_months_many),
        Triple(R.string.repeat_years_one, R.string.repeat_years_few, R.string.repeat_years_many)
    )

    ThemedDialog(onDismiss = onDismiss, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = trans(R.string.repeat_every),
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
                    val numbers = (1..99).toList()
                    WheelPicker(
                        items = numbers,
                        initialIndex = initialNum - 1,
                        modifier = Modifier.weight(1f),
                        onItemSelected = { selectedNum = it }
                    )

                    WheelPicker(
                        items = (0..3).toList(),
                        initialIndex = initialUnit,
                        modifier = Modifier.weight(1f),
                        format = { index ->
                            val it = pluralResources[index]
                            val resId = if (selectedNum % 10 == 1 && selectedNum % 100 != 11) it.first
                            else if (selectedNum % 10 in 2..4 && (selectedNum % 100 < 10 || selectedNum % 100 >= 20)) it.second
                            else it.third
                            resources.getString(resId)
                        },
                        onItemSelected = { selectedUnitIndex = it }
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(trans(R.string.common_cancel), color = XiaomiTextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        onSelect("${selectedNum}${unitKeys[selectedUnitIndex]}")
                    }) {
                        Text(trans(R.string.common_ok), color = XiaomiBlue, fontWeight = FontWeight.Bold)
                    }
                }
    }
}

@Composable
fun ColorPickerDialog(
    currentColor: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustom by remember { mutableStateOf(false) }
    var customColor by remember { 
        val c = currentColor ?: XiaomiBlue.toArgb()
        mutableStateOf(Color(c)) 
    }

    ThemedDialog(onDismiss = onDismiss) {
        Text(
            text = trans(if (showCustom) R.string.color_custom else R.string.select_color),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = XiaomiTextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (!showCustom) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(if (currentColor == null) XiaomiBlue.copy(alpha = 0.1f) else Color.Transparent)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (currentColor == null) XiaomiBlue else Color.LightGray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { onSelect(null) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentColor == null) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = XiaomiBlue, modifier = Modifier.size(20.dp))
                                } else {
                                    Text(trans(R.string.color_default).take(1), fontSize = 11.sp, color = XiaomiTextSecondary)
                                }
                            }
                        }

                        items(eventColors) { colorInt ->
                            val color = Color(colorInt)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { onSelect(colorInt.toInt()) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentColor == colorInt.toInt()) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(2.dp, Color.White, CircleShape)
                                            .padding(4.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize())
                                    }
                                }
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(Color.LightGray.copy(alpha = 0.2f))
                                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape)
                                    .clickable { showCustom = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = XiaomiTextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                } else {
                    HsvColorPicker(
                        initialColor = customColor,
                        onColorChanged = { customColor = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { if (showCustom) showCustom = false else onDismiss() }) {
                        Text(trans(R.string.common_cancel), color = XiaomiTextSecondary)
                    }
                    if (showCustom) {
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onSelect(customColor.toArgb()) },
                            colors = ButtonDefaults.buttonColors(containerColor = XiaomiBlue)
                        ) {
                            Text(trans(R.string.action_save))
                        }
                    }
                }
    }
}

@Composable
fun HsvColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit
) {
    val hsv = remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        mutableStateOf(hsv)
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.hsv(hsv.value[0], hsv.value[1], hsv.value[2]))
                    .border(1.dp, Color.LightGray.copy(alpha = 0.3f), CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = String.format(Locale.ROOT, "#%06X", (0xFFFFFF and Color.hsv(hsv.value[0], hsv.value[1], hsv.value[2]).toArgb())),
                fontSize = 14.sp,
                color = XiaomiTextSecondary,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()

            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val s = (change.position.x / width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / height).coerceIn(0f, 1f)
                    val newHsv = hsv.value.copyOf()
                    newHsv[1] = s
                    newHsv[2] = v
                    hsv.value = newHsv
                    onColorChanged(Color.hsv(newHsv[0], newHsv[1], newHsv[2]))
                }
            }) {
                drawRect(Color.hsv(hsv.value[0], 1f, 1f))
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color.White, Color.Transparent)
                    )
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black)
                    )
                )
                
                val x = hsv.value[1] * size.width
                val y = (1f - hsv.value[2]) * size.height
                
                drawCircle(
                    color = Color.Black.copy(alpha = 0.2f),
                    radius = 10.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(x, y),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.color_hue_label),
            fontSize = 12.sp,
            color = XiaomiTextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val h = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                            val newHsv = hsv.value.copyOf()
                            newHsv[0] = h
                            hsv.value = newHsv
                            onColorChanged(Color.hsv(newHsv[0], newHsv[1], newHsv[2]))
                        }
                    }
            ) {
                val colors = (0..360 step 60).map { Color.hsv(it.toFloat(), 1f, 1f) }
                drawRect(
                    brush = Brush.horizontalGradient(colors)
                )
                
                val x = (hsv.value[0] / 360f) * size.width
                
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(x, size.height / 2),
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = Color.hsv(hsv.value[0], 1f, 1f),
                    radius = 4.dp.toPx(),
                    center = Offset(x, size.height / 2)
                )
            }
        }
    }
}

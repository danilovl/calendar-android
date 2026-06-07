package danilovl.calendar.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import danilovl.calendar.R
import danilovl.calendar.data.EventType
import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.ui.appLocale
import danilovl.calendar.ui.components.eventAccentColor
import danilovl.calendar.ui.theme.XiaomiBg
import danilovl.calendar.ui.theme.XiaomiSurface
import danilovl.calendar.ui.theme.XiaomiTextPrimary
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.util.DateTimeUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private fun LocalDate.toCurrentYearOccurrence(year: Int): LocalDate =
    if (monthValue == 2 && dayOfMonth == 29 && !java.time.Year.isLeap(year.toLong()))
        LocalDate.of(year, 2, 28)
    else
        LocalDate.of(year, monthValue, dayOfMonth)

private data class SearchItem(
    val date: LocalDate,
    val title: String,
    val subtitle: String,
    val dotColor: Color,
    val event: CalendarEvent?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    allEvents: List<CalendarEvent>,
    onBack: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val locale = appLocale()
    val timeFmt = DateTimeUtils.timeFormatter
    val today = remember { LocalDate.now() }
    val allDayText = stringResource(R.string.common_all_day)
    val birthdayText = stringResource(R.string.event_birthday)
    val holidayText = stringResource(R.string.event_holiday)

    val items = remember(allEvents, today, allDayText, birthdayText, holidayText) {
        val list = ArrayList<SearchItem>()
        allEvents.forEach { e ->
            val subtitle = when {
                e.eventType == EventType.HOLIDAY.value -> holidayText
                e.eventType == EventType.BIRTHDAY.value -> birthdayText
                e.isAllDay -> allDayText
                e.startTime != null -> e.startTime.format(timeFmt) +
                    (e.endTime?.let { "-${it.format(timeFmt)}" } ?: "")
                else -> allDayText
            }
            val title = e.title
            list.add(SearchItem(e.date, title, subtitle, eventAccentColor(e), e))
        }
        list.sortedBy { it.date }
    }

    val filtered = remember(items, query) {
        if (query.isBlank()) items
        else items.filter { it.title.contains(query.trim(), ignoreCase = true) }
    }
    val grouped = remember(filtered) {
        filtered.groupBy { YearMonth.from(it.date) }.toSortedMap()
    }

    val anchorIndex = remember(grouped, today) {
        var idx = 0
        for ((_, monthItems) in grouped) {
            if (monthItems.any { !it.date.isBefore(today) }) return@remember idx
            idx += 1 + monthItems.size
        }
        maxOf(0, idx - 1)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(anchorIndex, query.isBlank()) {
        if (query.isBlank()) listState.scrollToItem(anchorIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title), fontWeight = FontWeight.Medium) },
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
                placeholder = { Text(stringResource(R.string.search_title)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = XiaomiTextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = XiaomiSurface,
                    unfocusedContainerColor = XiaomiSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (grouped.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.common_not_found), color = XiaomiTextSecondary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (month, monthItems) ->
                        item(key = "h-$month") {
                            Text(
                                text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = XiaomiTextPrimary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(monthItems, key = { "${it.date}-${it.title}-${it.event?.id ?: "b"}" }) { it ->
                            SearchRow(it, locale, onEventClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(item: SearchItem, locale: Locale, onEventClick: (CalendarEvent) -> Unit) {
    val icon = when {
        item.event?.eventType == EventType.BIRTHDAY.value -> Icons.Default.Cake
        item.event?.eventType == EventType.ANNIVERSARY.value -> Icons.Default.Favorite
        item.event?.eventType == EventType.REMINDER.value -> Icons.Default.Notifications
        else -> Icons.Default.Event
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.event != null) { item.event?.let(onEventClick) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = XiaomiSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.width(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(item.date.dayOfMonth.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = XiaomiTextPrimary)
                Text(
                    item.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    fontSize = 12.sp,
                    color = XiaomiTextSecondary
                )
            }
            Spacer(Modifier.width(16.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = item.dotColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = XiaomiTextPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.event?.reminder == true) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = item.dotColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(item.subtitle, fontSize = 13.sp, color = XiaomiTextSecondary)
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(item.dotColor)
            )
        }
    }
}

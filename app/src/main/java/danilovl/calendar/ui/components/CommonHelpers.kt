package danilovl.calendar.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import danilovl.calendar.R
import danilovl.calendar.ui.trans
import danilovl.calendar.util.RegexPatterns
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime

@Composable
fun rememberCurrentDateTime(): LocalDateTime {
    var dateTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            dateTime = now
            val nextMinute = now.plusMinutes(1).withSecond(0).withNano(0)
            val delayMillis = Duration.between(LocalDateTime.now(), nextMinute).toMillis()
            delay(delayMillis.coerceAtLeast(1000L))
        }
    }
    return dateTime
}

@Composable
fun repeatLabel(repeat: String): String {
    if (repeat.contains("_")) {
        val num = RegexPatterns.LEAD_NUMBER.find(repeat)?.value?.toInt() ?: 1
        val unitRes = when {
            repeat.contains("_year") -> Triple(R.string.repeat_years_one, R.string.repeat_years_few, R.string.repeat_years_many)
            repeat.contains("_month") -> Triple(R.string.repeat_months_one, R.string.repeat_months_few, R.string.repeat_months_many)
            repeat.contains("_week") -> Triple(R.string.repeat_weeks_one, R.string.repeat_weeks_few, R.string.repeat_weeks_many)
            else -> Triple(R.string.repeat_days_one, R.string.repeat_days_few, R.string.repeat_days_many)
        }
        val unit = trans(selectPlural(num, unitRes))

        return "${trans(R.string.repeat_every)} $num $unit"
    }
    return when (repeat) {
        "daily" -> trans(R.string.repeat_daily)
        "weekly" -> trans(R.string.repeat_weekly)
        "monthly" -> trans(R.string.repeat_monthly)
        "yearly" -> trans(R.string.repeat_yearly)
        else -> trans(R.string.repeat_none)
    }
}

private fun selectPlural(num: Int, forms: Triple<Int, Int, Int>): Int {
    val mod10 = num % 10
    val mod100 = num % 100

    return when {
        mod10 == 1 && mod100 != 11 -> forms.first
        mod10 in 2..4 && (mod100 < 10 || mod100 >= 20) -> forms.second
        else -> forms.third
    }
}

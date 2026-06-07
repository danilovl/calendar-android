package danilovl.calendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@Composable
@ReadOnlyComposable
fun appLocale(): Locale {
    return LocalConfiguration.current.locales[0]
}

@Composable
fun weekdayLabels(weekStartsSunday: Boolean): List<String> {
    val locale = appLocale()
    val symbols = DateFormatSymbols.getInstance(locale)
    val shortWeekdays = symbols.shortWeekdays 

    return if (weekStartsSunday) {
        listOf(
            shortWeekdays[Calendar.SUNDAY],
            shortWeekdays[Calendar.MONDAY],
            shortWeekdays[Calendar.TUESDAY],
            shortWeekdays[Calendar.WEDNESDAY],
            shortWeekdays[Calendar.THURSDAY],
            shortWeekdays[Calendar.FRIDAY],
            shortWeekdays[Calendar.SATURDAY]
        )
    } else {
        listOf(
            shortWeekdays[Calendar.MONDAY],
            shortWeekdays[Calendar.TUESDAY],
            shortWeekdays[Calendar.WEDNESDAY],
            shortWeekdays[Calendar.THURSDAY],
            shortWeekdays[Calendar.FRIDAY],
            shortWeekdays[Calendar.SATURDAY],
            shortWeekdays[Calendar.SUNDAY]
        )
    }
}

@Composable
fun trans(id: Int, vararg args: Any): String {
    return stringResource(id, *args)
}

fun ruPlural(n: Int, one: String, few: String, many: String): String {
    val mod10 = n % 10
    val mod100 = n % 100
    val text = when {
        mod10 == 1 && mod100 != 11 -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
    return "$n $text"
}

@Composable
fun formatDays(n: Int): String {
    val locale = appLocale()
    return when (locale.language) {
        "ru" -> ruPlural(n, "день", "дня", "дней")
        "en" -> if (n == 1) "$n day" else "$n days"
        "es" -> if (n == 1) "$n día" else "$n días"
        "fr" -> if (n <= 1) "$n jour" else "$n jours"
        "zh" -> "$n 天"
        else -> if (n == 1) "$n day" else "$n days"
    }
}

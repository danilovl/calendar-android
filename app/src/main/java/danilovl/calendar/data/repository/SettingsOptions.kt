package danilovl.calendar.data.repository

object SettingsOptions {
    val weekStarts = listOf("monday", "sunday")
    val otherCalendars = listOf("none", "chinese", "islamic", "hebrew")
    val melodies = listOf("default", "quiet", "energetic", "melodic")
    val timeZones = listOf(
        "GMT+00:00", "GMT+01:00", "GMT+02:00", "GMT+03:00",
        "GMT+04:00", "GMT+05:00", "GMT+06:00", "GMT+08:00"
    )
}

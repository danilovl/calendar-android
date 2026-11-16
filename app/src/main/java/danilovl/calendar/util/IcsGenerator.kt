package danilovl.calendar.util

import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.service.RepeatUnit
import danilovl.calendar.service.excludedDateSet
import danilovl.calendar.service.parseRepeat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Minimal RFC 5545 writer. Timed values are written in UTC. */
object IcsGenerator {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

    fun generate(
        events: List<CalendarEvent>,
        zone: ZoneId = ZoneId.systemDefault(),
        now: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
    ): String {
        val sb = StringBuilder()
        fun line(text: String) = sb.append(fold(text)).append("\r\n")
        fun utc(dt: LocalDateTime) = dt.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime().format(dateTimeFormatter)

        line("BEGIN:VCALENDAR")
        line("VERSION:2.0")
        line("PRODID:-//danilovl//Calendar//EN")

        val stamp = now.format(dateTimeFormatter)
        events.forEachIndexed { index, event ->
            line("BEGIN:VEVENT")
            line("UID:${if (event.id > 0) event.id else "x$index"}-${event.date.format(dateFormatter)}@danilovl.calendar")
            line("DTSTAMP:$stamp")
            line("SUMMARY:${escape(event.title)}")
            if (event.description.isNotEmpty()) line("DESCRIPTION:${escape(event.description)}")
            if (event.location.isNotEmpty()) line("LOCATION:${escape(event.location)}")

            val allDay = event.isAllDay || event.startTime == null
            val start: LocalDateTime = event.date.atTime(event.startTime ?: java.time.LocalTime.MIDNIGHT)
            if (allDay) {
                line("DTSTART;VALUE=DATE:${event.date.format(dateFormatter)}")
                // DTEND is exclusive.
                line("DTEND;VALUE=DATE:${(event.endDate ?: event.date).plusDays(1).format(dateFormatter)}")
            } else {
                line("DTSTART:${utc(start)}")
                val endDay = event.endDate ?: event.date
                var end = event.endTime?.let { endDay.atTime(it) } ?: start.plusHours(1)
                if (!end.isAfter(start)) end = start.plusHours(1)
                line("DTEND:${utc(end)}")
            }

            parseRepeat(event.repeat)?.let { rule ->
                val freq = when (rule.unit) {
                    RepeatUnit.DAY -> "DAILY"
                    RepeatUnit.WEEK -> "WEEKLY"
                    RepeatUnit.MONTH -> "MONTHLY"
                    RepeatUnit.YEAR -> "YEARLY"
                }
                val parts = mutableListOf("FREQ=$freq")
                if (rule.interval > 1) parts.add("INTERVAL=${rule.interval}")
                event.repeatUntil?.let { until ->
                    parts.add(
                        if (allDay) "UNTIL=${until.format(dateFormatter)}"
                        else "UNTIL=${utc(until.atTime(23, 59, 59))}"
                    )
                }
                line("RRULE:${parts.joinToString(";")}")

                event.excludedDateSet().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sorted()
                    .forEach { ex ->
                        if (allDay) line("EXDATE;VALUE=DATE:${ex.format(dateFormatter)}")
                        else line("EXDATE:${utc(ex.atTime(start.toLocalTime()))}")
                    }
            }

            line("END:VEVENT")
        }

        line("END:VCALENDAR")
        return sb.toString()
    }

    private fun escape(s: String): String {
        return s.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    /** Folds lines longer than 75 octets (RFC 5545 §3.1) without splitting UTF-8 characters. */
    private fun fold(text: String): String {
        if (text.toByteArray(Charsets.UTF_8).size <= 75) return text
        val sb = StringBuilder()
        var lineBytes = 0
        var limit = 75
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val chars = Character.charCount(cp)
            val bytes = String(Character.toChars(cp)).toByteArray(Charsets.UTF_8).size
            if (lineBytes + bytes > limit) {
                sb.append("\r\n ")
                lineBytes = 0
                limit = 74 // continuation lines start with a space
            }
            sb.appendCodePoint(cp)
            lineBytes += bytes
            i += chars
        }
        return sb.toString()
    }
}

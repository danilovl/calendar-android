package danilovl.calendar.util

import danilovl.calendar.data.local.CalendarEvent
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object IcsGenerator {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

    fun generate(events: List<CalendarEvent>): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//danilovl//Calendar//EN\r\n")

        for (event in events) {
            sb.append("BEGIN:VEVENT\r\n")
            sb.append("SUMMARY:${escape(event.title)}\r\n")
            if (event.description.isNotEmpty()) {
                sb.append("DESCRIPTION:${escape(event.description)}\r\n")
            }
            if (event.location.isNotEmpty()) {
                sb.append("LOCATION:${escape(event.location)}\r\n")
            }

            if (event.isAllDay) {
                sb.append("DTSTART;VALUE=DATE:${event.date.format(dateFormatter)}\r\n")
                val endDate = (event.endDate ?: event.date).plusDays(1)
                sb.append("DTEND;VALUE=DATE:${endDate.format(dateFormatter)}\r\n")
            } else {
                val start = event.startTime?.let { event.date.atTime(it) } ?: event.date.atStartOfDay()
                val utcStart = start.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
                sb.append("DTSTART:${utcStart.format(dateTimeFormatter)}\r\n")
                
                val end = if (event.endDate != null && event.endTime != null) {
                    event.endDate.atTime(event.endTime)
                } else {
                    start.plusHours(1)
                }
                val utcEnd = end.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
                sb.append("DTEND:${utcEnd.format(dateTimeFormatter)}\r\n")
            }
            
            when (event.repeat) {
                "daily" -> sb.append("RRULE:FREQ=DAILY\r\n")
                "weekly" -> sb.append("RRULE:FREQ=WEEKLY\r\n")
                "monthly" -> sb.append("RRULE:FREQ=MONTHLY\r\n")
                "yearly" -> sb.append("RRULE:FREQ=YEARLY\r\n")
            }

            sb.append("END:VEVENT\r\n")
        }

        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    private fun escape(s: String): String {
        return s.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }
}

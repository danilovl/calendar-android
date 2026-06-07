package danilovl.calendar.util

import danilovl.calendar.data.local.CalendarEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object IcsParser {
    fun parse(content: String): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val lines = content.lines()
        var currentEvent: MutableMap<String, String>? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            when {
                trimmed.startsWith("BEGIN:VEVENT") -> currentEvent = mutableMapOf()
                trimmed.startsWith("END:VEVENT") -> {
                    currentEvent?.let {
                        val title = unescape(it["SUMMARY"] ?: "No title")
                        val dtStartStr = it["DTSTART"] ?: ""
                        if (dtStartStr.isNotEmpty()) {
                            val startDateTime = parseIcsDateTime(dtStartStr)
                            val date = startDateTime?.toLocalDate() ?: LocalDate.now()
                            val startTime = if (dtStartStr.contains("T")) startDateTime?.toLocalTime() else null
                            
                            val dtEndStr = it["DTEND"] ?: ""
                            val endDateTime = if (dtEndStr.isNotEmpty()) parseIcsDateTime(dtEndStr) else null
                            val endDate = endDateTime?.toLocalDate()
                            val endTime = if (dtEndStr.contains("T")) endDateTime?.toLocalTime() else null
                            
                            val isAllDay = !dtStartStr.contains("T")
                            
                            val rrule = it["RRULE"] ?: ""
                            val repeat = when {
                                rrule.contains("FREQ=DAILY") -> "daily"
                                rrule.contains("FREQ=WEEKLY") -> "weekly"
                                rrule.contains("FREQ=MONTHLY") -> "monthly"
                                rrule.contains("FREQ=YEARLY") -> "yearly"
                                else -> "none"
                            }
                            
                            val repeatUntil = if (rrule.contains("UNTIL=")) {
                                val untilStr = rrule.substringAfter("UNTIL=").substringBefore(";")
                                parseIcsDateTime(untilStr)?.toLocalDate()
                            } else null

                            events.add(CalendarEvent(
                                id = 0,
                                title = title,
                                description = unescape(it["DESCRIPTION"] ?: ""),
                                location = unescape(it["LOCATION"] ?: ""),
                                date = date,
                                endDate = if (endDate != date) endDate else null,
                                startTime = startTime,
                                endTime = endTime,
                                eventType = "default",
                                isAllDay = isAllDay,
                                repeat = repeat,
                                repeatUntil = repeatUntil
                            ))
                        }
                    }
                    currentEvent = null
                }
                currentEvent != null && trimmed.contains(":") -> {
                    val parts = trimmed.split(":", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].split(";")[0]
                        currentEvent[key] = parts[1]
                    }
                }
            }
        }
        return events
    }

    private fun parseIcsDateTime(dt: String): LocalDateTime? {
        return try {
            if (dt.contains("T")) {
                val cleanDt = dt.split(":")[0]
                val format = if (dt.endsWith("Z")) "yyyyMMdd'T'HHmmss'Z'" else "yyyyMMdd'T'HHmmss"
                LocalDateTime.parse(cleanDt, DateTimeFormatter.ofPattern(format))
            } else {
                LocalDate.parse(dt.take(8), DateTimeFormatter.ofPattern("yyyyMMdd")).atStartOfDay()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun unescape(s: String): String {
        return s.replace("\\\\", "\\")
            .replace("\\;", ";")
            .replace("\\,", ",")
            .replace("\\n", "\n")
            .replace("\\N", "\n")
    }
}

package danilovl.calendar.util

import danilovl.calendar.data.local.CalendarEvent
import danilovl.calendar.service.RepeatRule
import danilovl.calendar.service.RepeatUnit
import danilovl.calendar.service.formatRepeat
import danilovl.calendar.service.nextOccurrence
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Minimal RFC 5545 reader for VEVENTs. */
object IcsParser {
    private val DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    private data class Property(val params: Map<String, String>, val value: String)

    fun parse(content: String, zone: ZoneId = ZoneId.systemDefault()): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        var current: MutableMap<String, MutableList<Property>>? = null

        for (line in unfold(content)) {
            if (line.isBlank()) continue
            when {
                line.startsWith("BEGIN:VEVENT") -> current = mutableMapOf()
                line.startsWith("END:VEVENT") -> {
                    current?.let { props -> toEvent(props, zone)?.let(events::add) }
                    current = null
                }
                current != null -> {
                    val colon = indexOfValueColon(line)
                    if (colon <= 0) continue
                    val head = line.substring(0, colon).split(";")
                    val name = head[0].uppercase()
                    val params = head.drop(1).mapNotNull {
                        val eq = it.indexOf('=')
                        if (eq > 0) it.substring(0, eq).uppercase() to it.substring(eq + 1).trim('"') else null
                    }.toMap()
                    current.getOrPut(name) { mutableListOf() }.add(Property(params, line.substring(colon + 1)))
                }
            }
        }
        return events
    }

    /** Lines starting with a space or tab continue the previous line (RFC 5545 §3.1). */
    private fun unfold(content: String): List<String> {
        val result = mutableListOf<String>()
        for (raw in content.split("\r\n", "\n")) {
            if ((raw.startsWith(" ") || raw.startsWith("\t")) && result.isNotEmpty()) {
                result[result.size - 1] = result.last() + raw.substring(1)
            } else {
                result.add(raw)
            }
        }
        return result
    }

    /** First ':' that is not inside a quoted parameter value. */
    private fun indexOfValueColon(line: String): Int {
        var quoted = false
        for (i in line.indices) {
            when (line[i]) {
                '"' -> quoted = !quoted
                ':' -> if (!quoted) return i
            }
        }
        return -1
    }

    private data class IcsTime(val local: LocalDateTime, val isDate: Boolean)

    private fun parseTime(prop: Property?, zone: ZoneId): IcsTime? {
        prop ?: return null
        return parseValue(prop.value, prop.params["TZID"], prop.params["VALUE"] == "DATE", zone)
    }

    private fun parseValue(raw: String, tzid: String?, forceDate: Boolean, zone: ZoneId): IcsTime? {
        val value = raw.trim()
        return try {
            if (forceDate || !value.contains("T")) {
                IcsTime(LocalDate.parse(value.take(8), DATE).atStartOfDay(), true)
            } else {
                val local = LocalDateTime.parse(value.removeSuffix("Z").take(15), DATE_TIME)
                val source: ZoneId? = when {
                    value.endsWith("Z") -> ZoneOffset.UTC
                    tzid != null -> runCatching { ZoneId.of(tzid) }.getOrNull()
                    else -> null // floating time: already local
                }
                val converted = source?.let { local.atZone(it).withZoneSameInstant(zone).toLocalDateTime() } ?: local
                IcsTime(converted, false)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun toEvent(props: Map<String, List<Property>>, zone: ZoneId): CalendarEvent? {
        val start = parseTime(props["DTSTART"]?.firstOrNull(), zone) ?: return null
        val end = parseTime(props["DTEND"]?.firstOrNull(), zone)
        val isAllDay = start.isDate
        val date = start.local.toLocalDate()

        // DTEND is exclusive: an all-day event on the 5th has DTEND = 6th.
        val endDate = when {
            end == null -> null
            isAllDay -> end.local.toLocalDate().minusDays(1)
            else -> end.local.toLocalDate()
        }?.takeIf { it.isAfter(date) }

        val rruleParts = props["RRULE"]?.firstOrNull()?.value
            ?.split(";")
            ?.mapNotNull { part -> part.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0].uppercase() to it[1] } }
            ?.toMap()
            .orEmpty()
        val unit = when (rruleParts["FREQ"]?.uppercase()) {
            "DAILY" -> RepeatUnit.DAY
            "WEEKLY" -> RepeatUnit.WEEK
            "MONTHLY" -> RepeatUnit.MONTH
            "YEARLY" -> RepeatUnit.YEAR
            else -> null
        }
        val rule = unit?.let { RepeatRule(it, rruleParts["INTERVAL"]?.toIntOrNull()?.coerceIn(1, 99) ?: 1) }
        val repeat = formatRepeat(rule)

        val exDates = props["EXDATE"].orEmpty().flatMap { prop ->
            prop.value.split(",").mapNotNull { v ->
                parseValue(v, prop.params["TZID"], prop.params["VALUE"] == "DATE", zone)?.local?.toLocalDate()
            }
        }.map { it.toString() }.distinct()

        val startTime = if (isAllDay) null else start.local.toLocalTime()
        val endTime = if (isAllDay || end == null) null else end.local.toLocalTime()

        val base = CalendarEvent(
            id = 0,
            title = unescape(props["SUMMARY"]?.firstOrNull()?.value ?: "").ifBlank { "No title" },
            description = unescape(props["DESCRIPTION"]?.firstOrNull()?.value ?: ""),
            location = unescape(props["LOCATION"]?.firstOrNull()?.value ?: ""),
            date = date,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            eventType = "default",
            isAllDay = isAllDay,
            repeat = repeat,
            excludedDates = exDates.joinToString(",")
        )

        val until = rruleParts["UNTIL"]?.let { parseValue(it, null, false, zone)?.local?.toLocalDate() }
        val count = rruleParts["COUNT"]?.toIntOrNull()
        val repeatUntil = when {
            rule == null -> null
            until != null -> until
            count != null && count > 0 -> lastOccurrence(base, count)
            else -> null
        }
        return base.copy(repeatUntil = repeatUntil)
    }

    /** Date of the [count]-th occurrence (COUNT includes the first one, excluded dates still count). */
    private fun lastOccurrence(event: CalendarEvent, count: Int): LocalDate? {
        val plain = event.copy(excludedDates = "")
        var occ: LocalDate = event.date
        repeat(count - 1) {
            occ = nextOccurrence(plain, occ.plusDays(1)) ?: return occ
        }
        return occ
    }

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    'n', 'N' -> sb.append('\n')
                    else -> sb.append(n) // \\ \; \,
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}

package danilovl.calendar.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import danilovl.calendar.data.local.CalendarEvent
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object EventBackupUtils {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
        .setPrettyPrinting()
        .create()

    fun exportToJson(events: List<CalendarEvent>): String {
        return gson.toJson(events)
    }

    /**
     * Gson bypasses Kotlin constructors, so a backup missing a field (older app version, hand-edited
     * file) would put `null` into non-null properties and crash later. Parse into a nullable DTO and
     * fill in defaults instead. Entries without a title or date are skipped.
     */
    fun importFromJson(json: String): List<CalendarEvent> {
        val type = object : TypeToken<List<BackupEvent?>>() {}.type
        val raw: List<BackupEvent?> = gson.fromJson(json, type) ?: emptyList()
        return raw.mapNotNull { it?.toEvent() }
    }

    private data class BackupEvent(
        val title: String?,
        val description: String?,
        val date: LocalDate?,
        val endDate: LocalDate?,
        val startTime: LocalTime?,
        val endTime: LocalTime?,
        val isAllDay: Boolean?,
        val repeat: String?,
        val reminder: Boolean?,
        val reminderMelody: String?,
        val reminderOffset: String?,
        val timeZone: String?,
        val eventType: String?,
        val color: Int?,
        val location: String?,
        val repeatUntil: LocalDate?,
        val excludedDates: String?
    ) {
        fun toEvent(): CalendarEvent? {
            val d = CalendarEvent(title = "", date = LocalDate.MIN)
            return CalendarEvent(
                id = 0,
                title = title?.takeIf { it.isNotBlank() } ?: return null,
                description = description ?: d.description,
                date = date ?: return null,
                endDate = endDate?.takeIf { !it.isBefore(date) },
                startTime = startTime,
                endTime = endTime,
                isAllDay = isAllDay ?: (startTime == null),
                repeat = repeat ?: d.repeat,
                reminder = reminder ?: d.reminder,
                reminderMelody = reminderMelody ?: d.reminderMelody,
                reminderOffset = reminderOffset ?: d.reminderOffset,
                timeZone = timeZone ?: d.timeZone,
                eventType = eventType ?: d.eventType,
                color = color,
                location = location ?: d.location,
                repeatUntil = repeatUntil,
                excludedDates = excludedDates ?: d.excludedDates
            )
        }
    }

    private class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate?> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        override fun serialize(src: LocalDate, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.format(formatter))
        }
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDate? {
            return runCatching { LocalDate.parse(json.asString, formatter) }.getOrNull()
        }
    }

    private class LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime?> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_TIME
        override fun serialize(src: LocalTime, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.format(formatter))
        }
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalTime? {
            return runCatching { LocalTime.parse(json.asString, formatter) }.getOrNull()
        }
    }
}

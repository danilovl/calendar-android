package danilovl.calendar.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
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

    fun importFromJson(json: String): List<CalendarEvent> {
        val type = object : com.google.gson.reflect.TypeToken<List<CalendarEvent>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        override fun serialize(src: LocalDate, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.format(formatter))
        }
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDate {
            return LocalDate.parse(json.asString, formatter)
        }
    }

    private class LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_TIME
        override fun serialize(src: LocalTime, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.format(formatter))
        }
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalTime {
            return LocalTime.parse(json.asString, formatter)
        }
    }
}

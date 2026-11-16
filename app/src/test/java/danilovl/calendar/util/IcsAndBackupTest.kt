package danilovl.calendar.util

import danilovl.calendar.data.local.CalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class IcsAndBackupTest {
    private val moscow = ZoneId.of("Europe/Moscow")

    @Test
    fun timedEventSurvivesRoundTripWithoutTimeShift() {
        val e = CalendarEvent(
            id = 5, title = "Meet; a, b", description = "line1\nline2",
            date = LocalDate.of(2026, 3, 10), startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 30),
            repeat = "2_week", repeatUntil = LocalDate.of(2026, 6, 1), excludedDates = "2026-03-24"
        )
        val parsed = IcsParser.parse(IcsGenerator.generate(listOf(e), moscow), moscow).single()
        assertEquals(e.title, parsed.title)
        assertEquals(e.description, parsed.description)
        assertEquals(e.date, parsed.date)
        assertEquals(LocalTime.of(10, 0), parsed.startTime)
        assertEquals(LocalTime.of(11, 30), parsed.endTime)
        assertEquals("2_week", parsed.repeat)
        assertEquals(LocalDate.of(2026, 6, 1), parsed.repeatUntil)
        assertEquals("2026-03-24", parsed.excludedDates)
        assertNull(parsed.endDate)
    }

    @Test
    fun singleAllDayEventStaysOneDay() {
        val e = CalendarEvent(title = "Day off", date = LocalDate.of(2026, 5, 1), isAllDay = true)
        val parsed = IcsParser.parse(IcsGenerator.generate(listOf(e), moscow), moscow).single()
        assertTrue(parsed.isAllDay)
        assertEquals(LocalDate.of(2026, 5, 1), parsed.date)
        assertNull(parsed.endDate)
    }

    @Test
    fun multiDayAllDayEventKeepsEndDate() {
        val e = CalendarEvent(title = "Trip", date = LocalDate.of(2026, 5, 1), endDate = LocalDate.of(2026, 5, 3), isAllDay = true)
        val parsed = IcsParser.parse(IcsGenerator.generate(listOf(e), moscow), moscow).single()
        assertEquals(LocalDate.of(2026, 5, 3), parsed.endDate)
    }

    @Test
    fun parsesTzidFoldedLinesAndCount() {
        val ics = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
            "SUMMARY:Very long\r\n  title\r\n" +
            "DTSTART;TZID=Europe/Berlin:20260310T090000\r\n" +
            "DTEND;TZID=Europe/Berlin:20260310T100000\r\n" +
            "RRULE:FREQ=DAILY;COUNT=3\r\n" +
            "END:VEVENT\r\nEND:VCALENDAR\r\n"
        val parsed = IcsParser.parse(ics, moscow).single()
        assertEquals("Very long title", parsed.title)
        assertEquals(LocalTime.of(11, 0), parsed.startTime) // Berlin +1, Moscow +3 in March
        assertEquals("daily", parsed.repeat)
        assertEquals(LocalDate.of(2026, 3, 12), parsed.repeatUntil)
    }

    @Test
    fun longLinesAreFolded() {
        val e = CalendarEvent(title = "x".repeat(200), date = LocalDate.of(2026, 1, 1), isAllDay = true)
        val ics = IcsGenerator.generate(listOf(e), moscow)
        assertTrue(ics.split("\r\n").all { it.toByteArray().size <= 75 })
        assertEquals(e.title, IcsParser.parse(ics, moscow).single().title)
    }

    @Test
    fun backupWithMissingFieldsGetsDefaults() {
        val json = """[{"title":"Old","date":"2024-01-01"},{"date":"2024-01-02"},{"title":"No date"}]"""
        val events = EventBackupUtils.importFromJson(json)
        assertEquals(1, events.size)
        val e = events.single()
        assertEquals("", e.excludedDates)
        assertEquals("none", e.repeat)
        assertEquals("", e.description)
        assertEquals(0, e.id)
    }

    @Test
    fun backupRoundTrip() {
        val e = CalendarEvent(
            id = 7, title = "A", date = LocalDate.of(2026, 2, 2), startTime = LocalTime.of(9, 15),
            repeat = "weekly", excludedDates = "2026-02-09", reminder = true
        )
        val back = EventBackupUtils.importFromJson(EventBackupUtils.exportToJson(listOf(e))).single()
        assertEquals(e.copy(id = 0), back)
    }
}

package danilovl.calendar.service

import danilovl.calendar.data.local.CalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarLogicTest {

    private fun event(
        date: String,
        repeat: String = "none",
        endDate: String? = null,
        until: String? = null,
        excluded: String = ""
    ) = CalendarEvent(
        title = "t",
        date = LocalDate.parse(date),
        endDate = endDate?.let(LocalDate::parse),
        repeat = repeat,
        repeatUntil = until?.let(LocalDate::parse),
        excludedDates = excluded
    )

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun singleEvent() {
        val e = event("2026-03-10")
        assertTrue(isEventOnDate(e, d("2026-03-10")))
        assertFalse(isEventOnDate(e, d("2026-03-11")))
    }

    @Test
    fun multiDayEventCoversWholeSpan() {
        val e = event("2026-03-10", endDate = "2026-03-12")
        assertFalse(isEventOnDate(e, d("2026-03-09")))
        assertTrue(isEventOnDate(e, d("2026-03-10")))
        assertTrue(isEventOnDate(e, d("2026-03-11")))
        assertTrue(isEventOnDate(e, d("2026-03-12")))
        assertFalse(isEventOnDate(e, d("2026-03-13")))
    }

    @Test
    fun weeklyAndCustomInterval() {
        val weekly = event("2026-01-05", "weekly")
        assertTrue(isEventOnDate(weekly, d("2026-01-12")))
        assertFalse(isEventOnDate(weekly, d("2026-01-13")))

        val everyTwoWeeks = event("2026-01-05", "2_week")
        assertFalse(isEventOnDate(everyTwoWeeks, d("2026-01-12")))
        assertTrue(isEventOnDate(everyTwoWeeks, d("2026-01-19")))

        val everyThreeDays = event("2026-01-01", "3_day")
        assertTrue(isEventOnDate(everyThreeDays, d("2026-01-04")))
        assertFalse(isEventOnDate(everyThreeDays, d("2026-01-05")))
    }

    @Test
    fun monthlyOn31stClampsToLastDay() {
        val e = event("2026-01-31", "monthly")
        assertTrue(isEventOnDate(e, d("2026-02-28")))
        assertTrue(isEventOnDate(e, d("2026-04-30")))
        assertTrue(isEventOnDate(e, d("2026-03-31")))
        assertFalse(isEventOnDate(e, d("2026-03-30")))
    }

    @Test
    fun everyTwoMonthsDoesNotHitOddMonths() {
        // Old implementation used ChronoUnit.MONTHS (0 between Jan 31 and Feb 28) and matched February.
        val e = event("2026-01-31", "2_month")
        assertFalse(isEventOnDate(e, d("2026-02-28")))
        assertTrue(isEventOnDate(e, d("2026-03-31")))
    }

    @Test
    fun yearlyLeapDay() {
        val e = event("2024-02-29", "yearly")
        assertTrue(isEventOnDate(e, d("2025-02-28")))
        assertTrue(isEventOnDate(e, d("2028-02-29")))
        assertFalse(isEventOnDate(e, d("2028-02-28")))
    }

    @Test
    fun exclusionsAndUntil() {
        val e = event("2026-01-01", "daily", until = "2026-01-10", excluded = "2026-01-03,2026-01-05")
        assertFalse(isEventOnDate(e, d("2026-01-03")))
        assertTrue(isEventOnDate(e, d("2026-01-04")))
        assertFalse(isEventOnDate(e, d("2026-01-11")))
        assertEquals(d("2026-01-04"), nextOccurrence(e, d("2026-01-03")))
        assertNull(nextOccurrence(e, d("2026-01-11")))
    }

    @Test
    fun nextOccurrenceFarInFuture() {
        val e = event("2000-01-01", "daily")
        assertEquals(d("2026-11-16"), nextOccurrence(e, d("2026-11-16")))
        val yearly = event("1990-06-15", "yearly")
        assertEquals(d("2027-06-15"), nextOccurrence(yearly, d("2026-11-16")))
    }

    @Test
    fun occurrencesBetweenIncludesSpanStartingBeforeRange() {
        val e = event("2026-01-30", "weekly", endDate = "2026-02-02")
        val occ = occurrencesBetween(e, d("2026-02-01"), d("2026-02-14"))
        assertEquals(listOf(d("2026-01-30"), d("2026-02-06"), d("2026-02-13")), occ)
    }

    @Test
    fun repeatRoundTrip() {
        listOf("none", "daily", "weekly", "monthly", "yearly", "2_day", "3_week", "4_month", "5_year").forEach {
            assertEquals(it, formatRepeat(parseRepeat(it)))
        }
    }

    @Test
    fun leadMinutesParsing() {
        assertEquals(15L, leadMinutes("15_min"))
        assertEquals(120L, leadMinutes("2_hour"))
        assertEquals(1440L, leadMinutes("1_day"))
        assertEquals(10080L, leadMinutes("1_week"))
        assertEquals(0L, leadMinutes("none"))
    }
}

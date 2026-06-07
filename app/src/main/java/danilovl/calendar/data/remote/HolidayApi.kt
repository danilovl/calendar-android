package danilovl.calendar.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object HolidayApi {
    private const val BASE = "https://date.nager.at/api/v3"

    suspend fun fetchCountries(): String = httpGet("$BASE/AvailableCountries")

    suspend fun fetchHolidays(year: Int, code: String): String = httpGet("$BASE/PublicHolidays/$year/$code")

    private suspend fun httpGet(urlStr: String): String = withContext(Dispatchers.IO) {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw IOException("HTTP ${conn.responseCode} for $urlStr")
            }
        } finally {
            conn.disconnect()
        }
    }
}


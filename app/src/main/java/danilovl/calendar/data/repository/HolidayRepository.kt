package danilovl.calendar.data.repository

import android.content.Context
import com.google.gson.Gson
import danilovl.calendar.data.remote.Country
import danilovl.calendar.data.remote.CountryDto
import danilovl.calendar.data.remote.HolidayApi
import danilovl.calendar.data.remote.HolidayCountries
import danilovl.calendar.data.remote.HolidayDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.time.LocalDate

private data class HolidayJson(
    val month: Int,
    val day: Int,
    val name: String,
    val isWorkday: Boolean = false
)

class HolidayRepository(private val context: Context) {
    private val gson = Gson()
    @Volatile
    private var cachedBundled: Map<String, List<HolidayJson>>? = null
    private val holidayMemoryCache = ConcurrentHashMap<String, List<Holiday>>()

    private fun cacheDir(): File = File(context.filesDir, "holiday_cache").apply { mkdirs() }

    private fun readCache(name: String): String? =
        File(cacheDir(), name).takeIf { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }

    private fun writeCache(name: String, content: String) {
        runCatching { File(cacheDir(), name).writeText(content) }
    }

    suspend fun getCountries(): List<Country> = withContext(Dispatchers.IO) {
        fun parse(json: String): List<Country> {
            val type = object : com.google.gson.reflect.TypeToken<List<CountryDto>>() {}.type
            return gson.fromJson<List<CountryDto>>(json, type)
                .mapNotNull { it.toCountry() }
                .map { c -> c.copy(name = HolidayCountries.displayName(context, c.code)) }
                .sortedBy { it.name }
        }
        try {
            val json = HolidayApi.fetchCountries()
            writeCache(COUNTRIES_CACHE, json)
            parse(json)
        } catch (e: Exception) {
            readCache(COUNTRIES_CACHE)?.let { runCatching { parse(it) }.getOrNull() }
                ?: HolidayCountries.staticCountries.map { it.copy(name = HolidayCountries.displayName(context, it.code)) }
        }
    }

    /** Disk cache and bundled assets are read on IO, never on the caller's (often main) thread. */
    suspend fun getHolidays(year: Int, code: String): List<Holiday> = withContext(Dispatchers.IO) {
        fun parseApi(json: String): List<Holiday> {
            val type = object : com.google.gson.reflect.TypeToken<List<HolidayDto>>() {}.type
            return gson.fromJson<List<HolidayDto>>(json, type).mapNotNull { it.toHoliday(year) }
        }

        val cacheKey = "${code}_$year"
        val cacheName = "holidays_${code}_$year.json"

        holidayMemoryCache[cacheKey]?.let { return@withContext it }

        readCache(cacheName)?.let { cached ->
            runCatching { parseApi(cached) }.getOrNull()?.let { result ->
                holidayMemoryCache[cacheKey] = result
                return@withContext result
            }
        }

        try {
            val json = HolidayApi.fetchHolidays(year, code)
            writeCache(cacheName, json)
            parseApi(json).also { holidayMemoryCache[cacheKey] = it }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // a cancelled load must not pin the offline fallback in the cache
        } catch (e: Exception) {
            bundledFallback(year, code).also { holidayMemoryCache[cacheKey] = it }
        }
    }

    private fun bundledFallback(year: Int, code: String): List<Holiday> {
        val russianName = HolidayCountries.bundledByCode[code] ?: return emptyList()
        val byCountry = loadBundled()[russianName] ?: return emptyList()
        // runCatching: Feb 29 in a non-leap year must not break the whole list
        return byCountry.mapNotNull { h -> runCatching { Holiday(LocalDate.of(year, h.month, h.day), h.name, h.isWorkday) }.getOrNull() }
    }

    private fun loadBundled(): Map<String, List<HolidayJson>> {
        cachedBundled?.let { return it }

        return try {
            val jsonString = context.assets.open("holidays.json").bufferedReader().use { it.readText() }
            val type = object : com.google.gson.reflect.TypeToken<Map<String, List<HolidayJson>>>() {}.type
            val data: Map<String, List<HolidayJson>> = gson.fromJson(jsonString, type)
            cachedBundled = data
            data
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private companion object {
        const val COUNTRIES_CACHE = "countries.json"
    }
}

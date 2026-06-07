package danilovl.calendar.data.repository

import android.content.Context
import android.provider.ContactsContract
import danilovl.calendar.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ContactsRepository(private val context: Context) {
    private var birthdayCache: List<ContactBirthday>? = null
    private var birthdayCacheTime: Long = 0L

    suspend fun getBirthdays(): List<ContactBirthday> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (birthdayCache != null && now - birthdayCacheTime < CACHE_TTL_MS) {
            return@withContext birthdayCache!!
        }

        val birthdays = mutableListOf<ContactBirthday>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Event.CONTACT_ID,
            ContactsContract.CommonDataKinds.Event.START_DATE,
            ContactsContract.CommonDataKinds.Event.TYPE,
            ContactsContract.Contacts.DISPLAY_NAME
        )

        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
        val selectionArgs = arrayOf(
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()
        )

        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val dateIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

            while (it.moveToNext()) {
                val dateStr = it.getString(dateIndex)
                val name = it.getString(nameIndex)
                
                val date = parseBirthday(dateStr)
                if (date != null && name != null) {
                    birthdays.add(ContactBirthday(name, date))
                }
            }
        }
        birthdayCache = birthdays
        birthdayCacheTime = System.currentTimeMillis()
        birthdays
    }

    private fun parseBirthday(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null

        if (dateStr.startsWith("--")) {
            val parts = dateStr.removePrefix("--").split("-")
            val month = parts.getOrNull(0)?.toIntOrNull()
            val day = parts.getOrNull(1)?.toIntOrNull()

            if (month != null && day != null) {
                return runCatching { LocalDate.of(LocalDate.now().year, month, day) }.getOrNull()
            }

            return null
        }

        for (format in BIRTHDAY_FORMATS) {
            val parsed = runCatching { LocalDate.parse(dateStr, format) }.getOrNull()
            if (parsed != null) {
                return parsed
            }
        }

        AppLog.w("ContactsRepository", "Unparseable contact birthday date: $dateStr")

        return null
    }

    companion object {
        private const val CACHE_TTL_MS = 5 * 60 * 1000L
        private val BIRTHDAY_FORMATS = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy")
        )
    }
}

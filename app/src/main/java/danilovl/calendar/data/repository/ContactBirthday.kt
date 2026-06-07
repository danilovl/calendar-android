package danilovl.calendar.data.repository

import java.io.Serializable
import java.time.LocalDate

data class ContactBirthday(
    val name: String,
    val date: LocalDate
) : Serializable

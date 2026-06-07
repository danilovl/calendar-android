package danilovl.calendar.data.repository

import java.io.Serializable
import java.time.LocalDate

data class Holiday(
    val date: LocalDate,
    val name: String,
    val isWorkday: Boolean = false
) : Serializable

package danilovl.calendar.data

data class ExternalCalendar(
    val id: Long,
    val name: String,
    val accountName: String,
    val color: Int,
    val isVisible: Boolean = true
)

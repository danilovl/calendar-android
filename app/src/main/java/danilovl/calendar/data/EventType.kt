package danilovl.calendar.data

enum class EventType(val value: String) {
    EVENT("event"),
    BIRTHDAY("birthday"),
    ANNIVERSARY("anniversary"),
    REMINDER("reminder"),
    HOLIDAY("holiday"),
    EXTERNAL("external");

    companion object {
        fun fromString(value: String?): EventType {
            return when (value) {
                BIRTHDAY.value -> BIRTHDAY
                ANNIVERSARY.value -> ANNIVERSARY
                REMINDER.value -> REMINDER
                HOLIDAY.value -> HOLIDAY
                EXTERNAL.value -> EXTERNAL
                else -> EVENT
            }
        }
    }
}

package danilovl.calendar.data.remote

internal data class CountryDto(val countryCode: String?, val name: String?) {
    fun toCountry(): Country? {
        val code = countryCode ?: return null
        return Country(code, name ?: code)
    }
}

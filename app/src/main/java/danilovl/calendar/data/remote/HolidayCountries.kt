package danilovl.calendar.data.remote

import danilovl.calendar.R
import android.content.Context

object HolidayCountries {
    val bundledByCode: Map<String, String> = mapOf(
        "RU" to "Россия", "BY" to "Беларусь", "KZ" to "Казахстан", "UZ" to "Узбекистан",
        "US" to "США", "AM" to "Армения", "GE" to "Грузия", "KG" to "Киргизия",
        "TJ" to "Таджикистан", "AZ" to "Азербайджан", "UA" to "Украина", "TR" to "Турция",
        "FR" to "Франция", "DE" to "Германия", "CZ" to "Чехия", "IT" to "Италия",
        "ES" to "Испания", "PL" to "Польша", "GB" to "Великобритания", "AT" to "Австрия",
        "MD" to "Молдова", "LT" to "Литва", "LV" to "Латвия", "EE" to "Эстония",
        "FI" to "Финляндия", "SE" to "Швеция", "NO" to "Норвегия", "CH" to "Швейцария",
        "NL" to "Нидерланды", "BE" to "Бельгия", "GR" to "Греция", "PT" to "Португалия",
        "HU" to "Венгрия", "RO" to "Румыния", "BG" to "Болгария", "RS" to "Сербия",
        "HR" to "Хорватия", "SK" to "Словакия", "IL" to "Израиль", "CA" to "Канада",
        "CN" to "Китай", "JP" to "Япония", "KR" to "Южная Корея", "BR" to "Бразилия",
        "IN" to "Индия"
    )

    private val countryResIds = mapOf(
        "RU" to R.string.country_ru, "BY" to R.string.country_by, "KZ" to R.string.country_kz,
        "UZ" to R.string.country_uz, "US" to R.string.country_us, "AM" to R.string.country_am,
        "GE" to R.string.country_ge, "KG" to R.string.country_kg, "TJ" to R.string.country_tj,
        "AZ" to R.string.country_az, "UA" to R.string.country_ua, "TR" to R.string.country_tr,
        "FR" to R.string.country_fr, "DE" to R.string.country_de, "CZ" to R.string.country_cz,
        "IT" to R.string.country_it, "ES" to R.string.country_es, "PL" to R.string.country_pl,
        "GB" to R.string.country_gb, "AT" to R.string.country_at, "MD" to R.string.country_md,
        "LT" to R.string.country_lt, "LV" to R.string.country_lv, "EE" to R.string.country_ee,
        "FI" to R.string.country_fi, "SE" to R.string.country_se, "NO" to R.string.country_no,
        "CH" to R.string.country_ch, "NL" to R.string.country_nl, "BE" to R.string.country_be,
        "GR" to R.string.country_gr, "PT" to R.string.country_pt, "HU" to R.string.country_hu,
        "RO" to R.string.country_ro, "BG" to R.string.country_bg, "RS" to R.string.country_rs,
        "HR" to R.string.country_hr, "SK" to R.string.country_sk, "IL" to R.string.country_il,
        "CA" to R.string.country_ca, "CN" to R.string.country_cn, "JP" to R.string.country_jp,
        "KR" to R.string.country_kr, "BR" to R.string.country_br, "IN" to R.string.country_in
    )

    private val nameToCode: Map<String, String> = bundledByCode.entries.associate { (code, name) -> name to code }

    val staticCountries: List<Country> = bundledByCode.keys.map { code ->
        Country(code, bundledByCode[code] ?: code)
    }

    fun normalizeCode(value: String): String = when {
        value.length == 2 && value == value.uppercase() -> value
        nameToCode.containsKey(value) -> nameToCode.getValue(value)
        else -> "RU"
    }

    fun displayName(context: Context, code: String): String {
        return countryResIds[code]?.let { context.getString(it) } ?: bundledByCode[code] ?: code
    }
}

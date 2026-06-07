package danilovl.calendar.data

import android.content.Context
import java.util.Locale

data class AppLanguage(val code: String, val nativeName: String)

object AppLanguages {
    const val SYSTEM = "system"

    val supported: List<AppLanguage> = listOf(
        AppLanguage("en", "English"),
        AppLanguage("de", "Deutsch"),
        AppLanguage("es", "Español"),
        AppLanguage("fr", "Français"),
        AppLanguage("pt", "Português"),
        AppLanguage("ru", "Русский"),
        AppLanguage("zh", "中文"),
        AppLanguage("hi", "हिन्दी"),
        AppLanguage("bn", "বাংলা"),
        AppLanguage("ar", "العربية"),
        AppLanguage("ur", "اردو")
    )

    fun nativeName(code: String): String =
        supported.find { it.code == code }?.nativeName ?: code
}

object LocaleHelper {
    private const val PREFS = "calendar_settings"
    private const val KEY_LANGUAGE = "language"

    private fun persistedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        return prefs.getString(KEY_LANGUAGE, AppLanguages.SYSTEM) ?: AppLanguages.SYSTEM
    }

    fun wrap(base: Context): Context {
        val lang = persistedLanguage(base)

        if (lang == AppLanguages.SYSTEM || lang.isBlank()) {
            return base
        }

        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return base.createConfigurationContext(config)
    }
}

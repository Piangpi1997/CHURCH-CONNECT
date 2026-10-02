package org.cmf.churchconnect

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** Persists an app language choice. Tedim intentionally falls back to English until church review. */
internal object AppLocale {
    const val SYSTEM = "system"
    private const val PREFS = "church_connect_preferences"
    private const val KEY_LANGUAGE = "language_tag"
    private val supported = setOf("en", "my", "ctd")

    internal fun normalizeSelection(value: String?): String =
        value?.takeIf { it == SYSTEM || it in supported } ?: SYSTEM

    fun selectedTag(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val tags = context.getSystemService(LocaleManager::class.java)?.applicationLocales?.toLanguageTags().orEmpty()
            if (tags.isBlank()) return SYSTEM
            return normalizeSelection(Locale.forLanguageTag(tags.substringBefore(',')).language)
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, SYSTEM)
            .let(::normalizeSelection)
    }

    fun save(context: Context, tag: String) {
        require(tag == SYSTEM || tag in supported) { "Unsupported app language" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, tag)
            .apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag == SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        }
    }

    fun wrap(context: Context): Context {
        val selected = selectedTag(context)
        if (selected == SYSTEM) {
            Locale.setDefault(context.resources.configuration.locales[0] ?: Locale.getDefault())
            return context
        }
        val locale = Locale.forLanguageTag(selected)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList(locale))
        Locale.setDefault(locale)
        return context.createConfigurationContext(configuration)
    }
}

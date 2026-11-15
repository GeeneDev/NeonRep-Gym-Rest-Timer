package com.geenedev.gymreps.data

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * In-app language choice (Android 13+ also shows it in the system's per-app language setting).
 * An empty tag means "follow the phone".
 */
object AppLanguage {
    /** Language tags offered in the picker, each labelled in its own language. */
    val options = listOf("en" to "English", "it" to "Italiano", "es" to "Español")

    private const val PREFS = "settings"
    private const val KEY = "language"

    /** The chosen tag ("en", "it", "es"), or "" for the system default. */
    fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')

    fun set(context: Context, tag: String) {
        // Kept here too so services and alarms can use it before any activity has run (Android 12 and older).
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
        )
    }

    /**
     * [context] in the chosen language, for notifications posted from the service or an alarm.
     * Android 13+ applies the per-app language to every context already.
     */
    fun wrap(context: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return context
        val tag = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
        if (tag.isEmpty()) return context
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(tag))
        return context.createConfigurationContext(config)
    }
}

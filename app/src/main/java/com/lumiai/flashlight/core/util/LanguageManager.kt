package com.lumiai.flashlight.core.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * Manages runtime language switching.
 *
 * Supported values for [lang]:
 *   "system" — use device locale
 *   "en"     — force English
 *   "es"     — force Español
 *
 * Adding a new language in the future:
 *   1. Add values-XX/strings.xml
 *   2. Add entry to [SUPPORTED_LOCALES]
 *   3. Add option in SettingsScreen language selector
 */
object LanguageManager {

    /** Map of language code → display name (in that language) */
    val SUPPORTED_LOCALES: LinkedHashMap<String, String> = linkedMapOf(
        "system" to "\uD83C\uDF10 System default",
        "en"     to "🇬🇧 English",
        "es"     to "🇪🇸 Español",
    )

    /**
     * Returns a Context wrapped with the correct locale.
     * Call this in Activity.attachBaseContext().
     */
    fun wrap(base: Context, lang: String): Context {
        val locale = resolveLocale(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /**
     * Applies the locale and recreates the Activity so all strings update.
     * Call after saving the language preference.
     */
    fun applyAndRecreate(activity: Activity, lang: String) {
        val locale = resolveLocale(lang)
        Locale.setDefault(locale)
        val config = Configuration(activity.resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")  // createConfigurationContext preferred on API 26+; keeping for minSdk 23 compat
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
        activity.recreate()
    }

    /**
     * Returns the effective Locale for a language code.
     * "system" resolves to the device's current locale.
     */
    fun resolveLocale(lang: String): Locale = when (lang) {
        "es"    -> Locale("es")
        "en"    -> Locale("en")
        else    -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                       android.os.LocaleList.getDefault().get(0)
                   else
                       @Suppress("DEPRECATION") Locale.getDefault()
    }

    /**
     * Detects the system language and returns it if supported.
     * Used to auto-apply a supported language on first run.
     */
    fun detectSystemLanguage(): String {
        val systemLang = Locale.getDefault().language
        return if (SUPPORTED_LOCALES.containsKey(systemLang)) systemLang else "en"
    }
}

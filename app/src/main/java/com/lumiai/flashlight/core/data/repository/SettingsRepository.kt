package com.lumiai.flashlight.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lumiai.flashlight.core.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val LAST_MODE      = stringPreferencesKey("last_mode")
        val STROBE_HZ      = floatPreferencesKey("strobe_hz")
        val DISCO_BPM      = floatPreferencesKey("disco_bpm")
        val SCREEN_BRIGHT  = floatPreferencesKey("screen_brightness")
        val AUTO_OFF       = intPreferencesKey("auto_off_minutes")
        val DARK_THEME     = booleanPreferencesKey("dark_theme")
        val SHAKE_TOGGLE   = booleanPreferencesKey("shake_toggle")
        val KEEP_SCREEN    = booleanPreferencesKey("keep_screen_on")
        val SEEN_ONBOARDING  = booleanPreferencesKey("seen_onboarding")
        val NOTIF_ENABLED    = booleanPreferencesKey("notif_flash_enabled")
        val NOTIF_CALLS      = booleanPreferencesKey("notif_flash_calls")
        val NOTIF_MESSAGES   = booleanPreferencesKey("notif_flash_messages")
        val NOTIF_OTHER      = booleanPreferencesKey("notif_flash_other")
        val APP_LANGUAGE     = stringPreferencesKey("app_language")
    }

    val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            lastMode          = prefs[Keys.LAST_MODE]     ?: "steady",
            strobeHz          = prefs[Keys.STROBE_HZ]     ?: 5f,
            discoBpm          = prefs[Keys.DISCO_BPM]     ?: 120f,
            screenBrightness  = prefs[Keys.SCREEN_BRIGHT] ?: 1f,
            autoOffMinutes    = prefs[Keys.AUTO_OFF]     ?: 0,
            isDarkTheme       = prefs[Keys.DARK_THEME]    ?: true,
            shakeToToggle     = prefs[Keys.SHAKE_TOGGLE]  ?: true,
            keepScreenOn      = prefs[Keys.KEEP_SCREEN]   ?: true,
            hasSeenOnboarding   = prefs[Keys.SEEN_ONBOARDING]  ?: false,
            notifFlashEnabled   = prefs[Keys.NOTIF_ENABLED]   ?: false,
            notifFlashCalls     = prefs[Keys.NOTIF_CALLS]     ?: true,
            notifFlashMessages  = prefs[Keys.NOTIF_MESSAGES]  ?: true,
            notifFlashOther     = prefs[Keys.NOTIF_OTHER]     ?: false,
            appLanguage         = prefs[Keys.APP_LANGUAGE]   ?: "system",
        )
    }

    suspend fun updateLastMode(modeId: String) {
        dataStore.edit { it[Keys.LAST_MODE] = modeId }
    }
    suspend fun updateStrobeHz(hz: Float) {
        dataStore.edit { it[Keys.STROBE_HZ] = hz }
    }
    suspend fun updateDiscoBpm(bpm: Float) {
        dataStore.edit { it[Keys.DISCO_BPM] = bpm }
    }
    suspend fun updateScreenBrightness(v: Float) {
        dataStore.edit { it[Keys.SCREEN_BRIGHT] = v }
    }
    suspend fun setAutoOffMinutes(minutes: Int) {
        dataStore.edit { it[Keys.AUTO_OFF] = minutes }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        dataStore.edit { it[Keys.DARK_THEME] = dark }
    }
    suspend fun setShakeToToggle(enabled: Boolean) {
        dataStore.edit { it[Keys.SHAKE_TOGGLE] = enabled }
    }
    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[Keys.KEEP_SCREEN] = enabled }
    }
    suspend fun setNotifFlashEnabled(v: Boolean)  { dataStore.edit { it[Keys.NOTIF_ENABLED]  = v } }
    suspend fun setNotifFlashCalls(v: Boolean)    { dataStore.edit { it[Keys.NOTIF_CALLS]    = v } }
    suspend fun setNotifFlashMessages(v: Boolean) { dataStore.edit { it[Keys.NOTIF_MESSAGES] = v } }
    suspend fun setNotifFlashOther(v: Boolean)    { dataStore.edit { it[Keys.NOTIF_OTHER]    = v } }
    suspend fun setAppLanguage(lang: String)      { dataStore.edit { it[Keys.APP_LANGUAGE]   = lang } }

    suspend fun markOnboardingSeen() {
        dataStore.edit { it[Keys.SEEN_ONBOARDING] = true }
    }
}

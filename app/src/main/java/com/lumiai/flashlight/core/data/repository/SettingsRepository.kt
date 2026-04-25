package com.lumiai.flashlight.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
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
        val LAST_MODE      = stringKey("last_mode")
        val STROBE_HZ      = floatKey("strobe_hz")
        val DISCO_BPM      = floatKey("disco_bpm")
        val SCREEN_BRIGHT  = floatKey("screen_brightness")
        val DARK_THEME     = booleanKey("dark_theme")
        val SHAKE_TOGGLE   = booleanKey("shake_toggle")
        val KEEP_SCREEN    = booleanKey("keep_screen_on")
        val SEEN_ONBOARDING = booleanKey("seen_onboarding")
    }

    val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            lastMode          = prefs[Keys.LAST_MODE]     ?: "steady",
            strobeHz          = prefs[Keys.STROBE_HZ]     ?: 5f,
            discoBpm          = prefs[Keys.DISCO_BPM]     ?: 120f,
            screenBrightness  = prefs[Keys.SCREEN_BRIGHT] ?: 1f,
            isDarkTheme       = prefs[Keys.DARK_THEME]    ?: true,
            shakeToToggle     = prefs[Keys.SHAKE_TOGGLE]  ?: true,
            keepScreenOn      = prefs[Keys.KEEP_SCREEN]   ?: true,
            hasSeenOnboarding = prefs[Keys.SEEN_ONBOARDING] ?: false,
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
    suspend fun setDarkTheme(dark: Boolean) {
        dataStore.edit { it[Keys.DARK_THEME] = dark }
    }
    suspend fun setShakeToToggle(enabled: Boolean) {
        dataStore.edit { it[Keys.SHAKE_TOGGLE] = enabled }
    }
    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[Keys.KEEP_SCREEN] = enabled }
    }
    suspend fun markOnboardingSeen() {
        dataStore.edit { it[Keys.SEEN_ONBOARDING] = true }
    }
}

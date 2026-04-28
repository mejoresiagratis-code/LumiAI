package com.lumiai.flashlight.core.domain.model

/**
 * Persisted user preferences (DataStore).
 */
data class UserSettings(
    val lastMode: String         = "steady",
    val strobeHz: Float          = 5f,
    val discoBpm: Float          = 120f,
    val screenBrightness: Float  = 1f,
    val isDarkTheme: Boolean     = true,
    val shakeToToggle: Boolean   = true,
    val autoOffMinutes: Int      = 0,
    val keepScreenOn: Boolean    = true,
    val hasSeenOnboarding: Boolean = false,
    val notifFlashEnabled: Boolean  = false,
    val notifFlashCalls: Boolean    = true,
    val notifFlashMessages: Boolean = true,
    val notifFlashOther: Boolean    = false,
    val appLanguage: String         = "system",
    val torchIntensity: Float        = 1.0f,  // "system" | "en" | "es"
)

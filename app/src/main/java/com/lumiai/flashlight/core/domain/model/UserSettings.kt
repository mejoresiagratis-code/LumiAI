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
    val torchIntensity: Float        = 1.0f,
    val morseText: String           = "",
    val morseSpeed: Float           = 1.0f,
    val sleepMinutes: Int           = 3,
    val micSensitivity: Float       = 1.0f,
    val screenColorId: String       = "white",
    val screenText: String          = "",  // "system" | "en" | "es"
    val customPattern: String       = "",  // CSV of on/off ms durations for Custom mode
)

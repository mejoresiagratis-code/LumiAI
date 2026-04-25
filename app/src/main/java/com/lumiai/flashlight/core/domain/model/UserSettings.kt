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
    val keepScreenOn: Boolean    = true,
    val hasSeenOnboarding: Boolean = false,
)

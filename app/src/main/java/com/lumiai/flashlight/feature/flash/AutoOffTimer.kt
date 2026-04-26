package com.lumiai.flashlight.feature.flash

/**
 * Auto-off timer options in minutes.
 * NONE = disabled (default).
 */
enum class AutoOffOption(val minutes: Int, val label: String) {
    NONE(0,  "Off"),
    MIN_5(5,  "5 min"),
    MIN_10(10, "10 min"),
    MIN_30(30, "30 min"),
    MIN_60(60, "1 hour"),
}

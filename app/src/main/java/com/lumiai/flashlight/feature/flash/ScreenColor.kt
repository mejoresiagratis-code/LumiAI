package com.lumiai.flashlight.feature.flash

import androidx.compose.ui.graphics.Color

/**
 * Available screen colors for Screen mode.
 */
enum class ScreenColor(val color: Color, val label: String) {
    WHITE(      Color(0xFFFFFFFF), "White"),
    WARM_WHITE( Color(0xFFFFE4B5), "Warm white"),
    RED(        Color(0xFFFF3B30), "Red"),
    RED_DARK(   Color(0xFF8B0000), "Night red"),
    GREEN(      Color(0xFF34C759), "Green"),
    GREEN_DARK( Color(0xFF1A5C2A), "Night green"),
    BLUE(       Color(0xFF007AFF), "Blue"),
    CYAN(       Color(0xFF00CED1), "Cyan"),
    YELLOW(     Color(0xFFFFD60A), "Yellow"),
    LIME(       Color(0xFF90EE50), "Lime"),
    ORANGE(     Color(0xFFFF9500), "Orange"),
    PURPLE(     Color(0xFFBF5AF2), "Purple"),
}

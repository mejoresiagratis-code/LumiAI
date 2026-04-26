package com.lumiai.flashlight.feature.flash

import androidx.compose.ui.graphics.Color

/**
 * Available screen colors for Screen mode.
 */
enum class ScreenColor(val color: Color, val label: String) {
    WHITE(  Color(0xFFFFFBEB), "White"),
    RED(    Color(0xFFFF3B30), "Red"),
    GREEN(  Color(0xFF34C759), "Green"),
    BLUE(   Color(0xFF007AFF), "Blue"),
    YELLOW( Color(0xFFFFD60A), "Yellow"),
    ORANGE( Color(0xFFFF9500), "Orange"),
    PURPLE( Color(0xFFBF5AF2), "Purple"),
    WARM(   Color(0xFFFF8C42), "Warm"),
}

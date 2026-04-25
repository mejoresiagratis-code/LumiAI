package com.lumiai.flashlight.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System font with tight tracking — precision tool aesthetic
// Replace with custom font (e.g. DM Mono, Space Mono) if desired
val LumiTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.W300, fontSize = 57.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.W300, fontSize = 45.sp, letterSpacing = (-0.3).sp),
    headlineLarge  = TextStyle(fontWeight = FontWeight.W600, fontSize = 32.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.W500, fontSize = 28.sp, letterSpacing = (-0.2).sp),
    titleLarge   = TextStyle(fontWeight = FontWeight.W700, fontSize = 22.sp, letterSpacing = 0.sp),
    titleMedium  = TextStyle(fontWeight = FontWeight.W600, fontSize = 16.sp, letterSpacing = 0.15.sp),
    titleSmall   = TextStyle(fontWeight = FontWeight.W600, fontSize = 14.sp, letterSpacing = 0.1.sp),
    bodyLarge    = TextStyle(fontWeight = FontWeight.W400, fontSize = 16.sp, letterSpacing = 0.5.sp, lineHeight = 24.sp),
    bodyMedium   = TextStyle(fontWeight = FontWeight.W400, fontSize = 14.sp, letterSpacing = 0.25.sp, lineHeight = 20.sp),
    bodySmall    = TextStyle(fontWeight = FontWeight.W400, fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelLarge   = TextStyle(fontWeight = FontWeight.W600, fontSize = 14.sp, letterSpacing = 0.08.sp),
    labelMedium  = TextStyle(fontWeight = FontWeight.W600, fontSize = 12.sp, letterSpacing = 0.1.sp),
    labelSmall   = TextStyle(fontWeight = FontWeight.W600, fontSize = 10.sp, letterSpacing = 0.12.sp),
)

package com.lumiai.flashlight.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Brand colors ──────────────────────────────────────────────────────────────
val Amber400    = Color(0xFFFFD84A)
val Amber600    = Color(0xFFE5B800)
val Navy900     = Color(0xFF0A0A0F)
val Navy800     = Color(0xFF12121A)
val Navy700     = Color(0xFF1E1E2E)
val Navy600     = Color(0xFF2A2A3C)
val Purple400   = Color(0xFF8B5CF6)
val Purple200   = Color(0xFFBEA7F5)
val White       = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary           = Amber400,
    onPrimary         = Navy900,
    primaryContainer  = Navy700,
    onPrimaryContainer = Amber400,
    secondary         = Purple400,
    onSecondary       = White,
    background        = Navy900,
    onBackground      = White,
    surface           = Navy800,
    onSurface         = White,
    surfaceVariant    = Navy700,
    onSurfaceVariant  = Color(0xFFCAC4D0),
    error             = Color(0xFFCF6679),
)

private val LightColorScheme = lightColorScheme(
    primary           = Color(0xFFB8860B),
    onPrimary         = White,
    primaryContainer  = Color(0xFFFFF3CC),
    onPrimaryContainer = Color(0xFF5A4200),
    secondary         = Purple400,
    onSecondary       = White,
    background        = Color(0xFFF8F8F8),
    onBackground      = Navy900,
    surface           = White,
    onSurface         = Navy900,
)

@Composable
fun LumiAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,   // Disabled: we want brand-consistent amber, not wallpaper color
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = LumiTypography,
        content     = content,
    )
}

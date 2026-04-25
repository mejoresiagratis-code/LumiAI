package com.lumiai.flashlight.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.lumiai.flashlight.ui.theme.LumiColor

private val DarkColorScheme = darkColorScheme(
    primary            = LumiColor.Amber400,
    onPrimary          = LumiColor.Navy900,
    primaryContainer   = LumiColor.Navy700,
    onPrimaryContainer = LumiColor.Amber400,
    secondary          = LumiColor.Purple400,
    onSecondary        = LumiColor.White,
    secondaryContainer = LumiColor.Purple900,
    onSecondaryContainer = LumiColor.Purple300,
    background         = LumiColor.Navy950,
    onBackground       = LumiColor.White,
    surface            = LumiColor.Navy800,
    onSurface          = LumiColor.White,
    surfaceVariant     = LumiColor.Navy700,
    onSurfaceVariant   = LumiColor.Gray400,
    outline            = LumiColor.Navy600,
    error              = LumiColor.Error,
    onError            = LumiColor.White,
)

private val LightColorScheme = lightColorScheme(
    primary            = LumiColor.Amber600,
    onPrimary          = LumiColor.White,
    primaryContainer   = LumiColor.Amber100,
    onPrimaryContainer = LumiColor.Amber700,
    secondary          = LumiColor.Purple500,
    onSecondary        = LumiColor.White,
    background         = LumiColor.Gray100,
    onBackground       = LumiColor.Navy900,
    surface            = LumiColor.White,
    onSurface          = LumiColor.Navy900,
    surfaceVariant     = LumiColor.Gray100,
    onSurfaceVariant   = LumiColor.Gray500,
)

@Composable
fun LumiAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Dynamic color disabled — brand amber identity takes priority
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = LumiTypography,
        content     = content,
    )
}

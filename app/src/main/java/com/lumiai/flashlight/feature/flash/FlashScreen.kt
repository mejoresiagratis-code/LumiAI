package com.lumiai.flashlight.feature.flash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.core.domain.model.ProStatus
import com.lumiai.flashlight.ui.components.AdBanner
import com.lumiai.flashlight.ui.components.FlashButton
import com.lumiai.flashlight.ui.components.LumiIcons
import com.lumiai.flashlight.ui.components.ModeControls
import com.lumiai.flashlight.ui.components.ModeSelector
import com.lumiai.flashlight.ui.theme.LumiColor

@Composable
fun FlashScreen(
    viewModel: FlashViewModel,
    onOpenSettings: () -> Unit,
    onOpenPro: () -> Unit,
) {
    val uiState      by viewModel.uiState.collectAsState()
    val isPro         = uiState.proStatus == ProStatus.Pro
    val isOn          = uiState.isFlashOn
    val mode          = uiState.currentMode
    val isScreenMode  = mode is FlashMode.Screen && isOn

    val bgColor = if (isScreenMode) LumiColor.BeamColor else LumiColor.Navy950

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(bgColor),
    ) {
        val screenH = maxHeight

        // Subtle top glow when flash is ON
        if (isOn && !isScreenMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(LumiColor.Amber400.copy(alpha = 0.06f), Color.Transparent)
                        )
                    )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── Top bar ────────────────────────────────────────────────────
            TopBar(
                isPro          = isPro,
                isScreenMode   = isScreenMode,
                onOpenSettings = onOpenSettings,
                onOpenPro      = onOpenPro,
            )

            // ── Status text — single Text, no AnimatedContent overlap ──────
            Spacer(Modifier.height(4.dp))
            val statusText = statusLabel(isOn, mode, isScreenMode, uiState)
            val statusColor = when {
                isScreenMode -> LumiColor.Navy900.copy(alpha = 0.35f)
                isOn         -> LumiColor.Amber400.copy(alpha = 0.8f)
                else         -> LumiColor.Gray600
            }
            Text(
                text          = statusText,
                fontSize      = 10.sp,
                fontWeight    = FontWeight.W600,
                letterSpacing = 0.14.sp,
                color         = statusColor,
                textAlign     = TextAlign.Center,
                modifier      = Modifier.fillMaxWidth(),
            )

            // ── Button: 32% of screen height ──────────────────────────────
            Spacer(Modifier.weight(1f))
            val btnSize = (screenH * 0.32f).coerceIn(144.dp, 196.dp)
            FlashButton(
                isOn    = isOn,
                onClick = { viewModel.toggleFlash() },
                size    = btnSize,
            )
            Spacer(Modifier.weight(1f))

            // ── Contextual Hz/BPM slider ───────────────────────────────────
            ModeControls(
                currentMode        = mode,
                strobeHz           = uiState.strobeHz,
                discoBpm           = uiState.discoBpm,
                screenBrightness   = 1f,
                onStrobeHzChange   = { viewModel.activateMode(FlashMode.Strobe(it)) },
                onDiscoBpmChange   = { viewModel.activateMode(FlashMode.Disco(it)) },
                onBrightnessChange = { },
                modifier           = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(14.dp))

            // ── Mode chips ─────────────────────────────────────────────────
            if (!isScreenMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    ModeSelector(
                        currentMode  = mode,
                        isPro        = isPro,
                        onModeSelect = { viewModel.activateMode(it) },
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── Ad banner (Free) ───────────────────────────────────────────
            if (!isPro && !isScreenMode) {
                AdBanner(modifier = Modifier.navigationBarsPadding())
            } else {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────
@Composable
private fun TopBar(
    isPro: Boolean,
    isScreenMode: Boolean,
    onOpenSettings: () -> Unit,
    onOpenPro: () -> Unit,
) {
    val onDark = !isScreenMode

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // Wordmark
        Column {
            Text(
                "LUMI",
                fontSize      = 17.sp,
                fontWeight    = FontWeight.W900,
                letterSpacing = 0.12.sp,
                color         = if (onDark) LumiColor.Amber400 else LumiColor.Navy800,
            )
            Text(
                "AI",
                fontSize      = 9.sp,
                fontWeight    = FontWeight.W400,
                letterSpacing = 0.2.sp,
                color         = if (onDark) LumiColor.Gray600 else LumiColor.Navy800.copy(0.4f),
                modifier      = Modifier.offset(y = (-2).dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            // Pro badge — only show upgrade if not Pro, keep it small
            if (!isPro) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(LumiColor.Purple900)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onOpenPro,
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("✦", fontSize = 9.sp, color = LumiColor.Purple300)
                        Text(
                            "Pro",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.W600,
                            color      = LumiColor.Purple300,
                        )
                    }
                }
            } else {
                // Pro active — minimal pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LumiColor.Purple900.copy(0.6f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        "PRO",
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.W700,
                        letterSpacing = 0.08.sp,
                        color         = LumiColor.Purple400,
                    )
                }
            }

            // Settings — 44dp touch target
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (onDark) LumiColor.Navy800 else LumiColor.Navy800.copy(0.12f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onOpenSettings,
                    ),
            ) {
                Icon(
                    LumiIcons.Settings,
                    contentDescription = "Ajustes",
                    tint               = if (onDark) LumiColor.Gray400 else LumiColor.Navy800.copy(0.6f),
                    modifier           = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun statusLabel(
    isOn: Boolean,
    mode: FlashMode,
    isScreenMode: Boolean,
    uiState: FlashUiState,
): String = when {
    !isOn            -> "TAP TO TURN ON"
    isScreenMode     -> "SCREEN MODE"
    mode is FlashMode.Sos              -> "SOS · · · — — —"
    mode is FlashMode.Strobe           -> "STROBE · ${uiState.strobeHz.toInt()} HZ"
    mode is FlashMode.Disco            -> "DISCO · ${uiState.discoBpm.toInt()} BPM"
    mode is FlashMode.SmartBrightness  -> "AI SMART MODE"
    mode is FlashMode.ReadingMode      -> "AI READING MODE"
    mode is FlashMode.AmbientSmart     -> "AI AMBIENT"
    mode is FlashMode.CustomRhythm     -> "AI CUSTOM RHYTHM"
    mode is FlashMode.SleepTimer       -> "AI SLEEP TIMER"
    else             -> "FLASHLIGHT ON"
}

package com.lumiai.flashlight.feature.flash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.core.domain.model.ProStatus
import com.lumiai.flashlight.ui.components.AdBanner
import com.lumiai.flashlight.ui.components.FlashButton
import com.lumiai.flashlight.ui.components.LumiIcons
import com.lumiai.flashlight.ui.components.ModePanel
import com.lumiai.flashlight.ui.theme.LumiColor

@Composable
fun FlashScreen(
    viewModel: FlashViewModel,
    onOpenSettings: () -> Unit,
    onOpenPro: () -> Unit,
) {
    val uiState     by viewModel.uiState.collectAsState()
    val isPro        = uiState.proStatus == ProStatus.Pro
    val isOn         = uiState.isFlashOn
    val mode         = uiState.currentMode
    val isScreenMode = mode is FlashMode.Screen && isOn

    val bgColor = if (isScreenMode) LumiColor.BeamColor else LumiColor.Navy950

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        val screenH = maxHeight

        // Subtle top glow when ON
        if (isOn && !isScreenMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(LumiColor.Amber400.copy(alpha = 0.05f), Color.Transparent)
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Top bar ────────────────────────────────────────────────────
            TopBar(
                isPro          = isPro,
                isScreenMode   = isScreenMode,
                onOpenSettings = onOpenSettings,
                onOpenPro      = onOpenPro,
            )

            // ── Status ─────────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Text(
                text          = statusLabel(isOn, mode, uiState),
                fontSize      = 10.sp,
                fontWeight    = FontWeight.W500,
                letterSpacing = 0.14.sp,
                color         = when {
                    isScreenMode -> LumiColor.Navy900.copy(.35f)
                    isOn         -> LumiColor.Amber400.copy(.7f)
                    else         -> LumiColor.Gray600
                },
            )

            // ── Hero button ────────────────────────────────────────────────
            val btnSize = (screenH * 0.28f).coerceIn(130.dp, 180.dp)
            Spacer(Modifier.height(20.dp))
            FlashButton(
                isOn    = isOn,
                onClick = { viewModel.toggleFlash() },
                size    = btnSize,
            )
            Spacer(Modifier.height(28.dp))

            // ── Mode panel (tabs + cards + slider) ─────────────────────────
            if (!isScreenMode) {
                ModePanel(
                    currentMode      = mode,
                    strobeHz         = uiState.strobeHz,
                    discoBpm         = uiState.discoBpm,
                    onModeSelect     = { viewModel.activateMode(it) },
                    onStrobeHzChange = { viewModel.updateStrobeHz(it) },
                    onDiscoBpmChange = { viewModel.updateDiscoBpm(it) },
                    modifier         = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Ad banner ──────────────────────────────────────────────────
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
                "LUMI·AI",
                fontSize      = 14.sp,
                fontWeight    = FontWeight.W700,
                letterSpacing = 0.16.sp,
                color         = if (isScreenMode) LumiColor.Navy800 else LumiColor.White,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            if (!isPro) {
                // Star upgrade button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(LumiColor.Navy800)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onOpenPro,
                        ),
                ) {
                    Icon(
                        LumiIcons.Star,
                        contentDescription = "Pro",
                        tint     = LumiColor.Purple300,
                        modifier = Modifier.size(14.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LumiColor.Navy800)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        "PRO",
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.W700,
                        letterSpacing = 0.1.sp,
                        color         = LumiColor.Purple400,
                    )
                }
            }

            // Settings
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(LumiColor.Navy800)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenSettings,
                    ),
            ) {
                Icon(
                    LumiIcons.Settings,
                    contentDescription = "Ajustes",
                    tint     = LumiColor.Gray500,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ── Status label ──────────────────────────────────────────────────────────────
private fun statusLabel(isOn: Boolean, mode: FlashMode, uiState: FlashUiState): String = when {
    !isOn                        -> "TAP TO TURN ON"
    mode is FlashMode.Screen     -> "SCREEN MODE"
    mode is FlashMode.Sos        -> "SOS · · · — — —"
    mode is FlashMode.Strobe     -> "STROBE · ${uiState.strobeHz.toInt()} HZ"
    mode is FlashMode.Disco      -> "DISCO · ${uiState.discoBpm.toInt()} BPM"
    mode is FlashMode.SmartBrightness -> "◎ SMART MODE"
    mode is FlashMode.ReadingMode     -> "☽ READING MODE"
    mode is FlashMode.AmbientSmart    -> "◈ AMBIENT MODE"
    mode is FlashMode.CustomRhythm    -> "⬡ CUSTOM RHYTHM"
    mode is FlashMode.SleepTimer      -> "◌ SLEEP TIMER"
    else                         -> "FLASHLIGHT ON"
}

package com.lumiai.flashlight.feature.flash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
    val uiState     by viewModel.uiState.collectAsState()
    val isPro        = uiState.proStatus == ProStatus.Pro
    val isOn         = uiState.isFlashOn
    val mode         = uiState.currentMode
    val isScreenMode = mode is FlashMode.Screen && isOn

    // Screen mode: full bright white
    val bgColor = if (isScreenMode) LumiColor.BeamColor else LumiColor.Navy950

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        val screenH = maxHeight

        // Ambient glow when ON
        if (isOn && !isScreenMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                LumiColor.Amber400.copy(alpha = 0.07f),
                                Color.Transparent,
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── Top bar — compact ──────────────────────────────────────────
            TopBar(
                isPro          = isPro,
                isScreenMode   = isScreenMode,
                onOpenSettings = onOpenSettings,
                onOpenPro      = onOpenPro,
            )

            // ── Status text ────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            StatusLabel(isOn = isOn, mode = mode, isScreenMode = isScreenMode)

            // ── HERO button — fills ~40% of screen height ──────────────────
            Spacer(Modifier.weight(1f))
            val btnSize = (screenH * 0.30f).coerceIn(140.dp, 200.dp)
            FlashButton(
                isOn    = isOn,
                onClick = { viewModel.toggleFlash() },
                size    = btnSize,
            )
            Spacer(Modifier.weight(1f))

            // ── Contextual slider (Hz / BPM / brightness) ─────────────────
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

            Spacer(Modifier.height(16.dp))

            // ── Mode selector ──────────────────────────────────────────────
            if (!isScreenMode) {
                // Wrap in horizontal scroll for small screens
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
                Spacer(Modifier.height(12.dp))
            }

            // ── AdBanner (Free only) ───────────────────────────────────────
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
    val contentColor = if (isScreenMode) LumiColor.Navy900 else LumiColor.White

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
                letterSpacing = 0.15.sp,
                color         = if (isScreenMode) LumiColor.Navy900 else LumiColor.Amber400,
            )
            Text(
                "AI",
                fontSize      = 9.sp,
                fontWeight    = FontWeight.W400,
                letterSpacing = 0.25.sp,
                color         = if (isScreenMode) LumiColor.Navy900.copy(0.4f) else LumiColor.Gray500,
                modifier      = Modifier.offset(y = (-3).dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            // Pro badge / upgrade CTA
            if (!isPro) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(LumiColor.Purple900.copy(alpha = 0.9f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onOpenPro,
                        )
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            LumiIcons.Star,
                            contentDescription = null,
                            tint               = LumiColor.Purple300,
                            modifier           = Modifier.size(12.dp),
                        )
                        Text(
                            "Pro",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.W600,
                            color      = LumiColor.Purple300,
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(LumiColor.Purple900.copy(alpha = 0.5f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
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

            // Settings button — 44dp min touch target
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isScreenMode) LumiColor.Navy800.copy(0.12f)
                        else LumiColor.Navy700.copy(0.7f)
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
                    tint               = contentColor.copy(alpha = 0.75f),
                    modifier           = Modifier.size(19.dp),
                )
            }
        }
    }
}

// ── Status label ──────────────────────────────────────────────────────────────
@Composable
private fun StatusLabel(isOn: Boolean, mode: FlashMode, isScreenMode: Boolean) {
    val textColor = when {
        isScreenMode -> LumiColor.Navy900.copy(alpha = 0.35f)
        isOn         -> LumiColor.Amber400.copy(alpha = 0.85f)
        else         -> LumiColor.Gray600
    }
    val statusText = when {
        !isOn                        -> "TAP TO TURN ON"
        isScreenMode                 -> "SCREEN MODE"
        mode is FlashMode.Sos        -> "SOS ACTIVE"
        mode is FlashMode.Strobe     -> "STROBE · ${uiState_strobeHz(mode)} HZ"
        mode is FlashMode.Disco      -> "DISCO · ${uiState_discoBpm(mode)} BPM"
        mode is FlashMode.SmartBrightness -> "AI SMART MODE"
        mode is FlashMode.ReadingMode     -> "AI READING MODE"
        mode is FlashMode.AmbientSmart    -> "AI AMBIENT MODE"
        mode is FlashMode.CustomRhythm   -> "AI CUSTOM RHYTHM"
        mode is FlashMode.SleepTimer      -> "AI SLEEP TIMER"
        else                         -> "FLASHLIGHT ON"
    }

    AnimatedContent(
        targetState   = statusText,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
        label         = "status",
    ) { text ->
        Text(
            text          = text,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.W600,
            letterSpacing = 0.14.sp,
            color         = textColor,
            textAlign     = TextAlign.Center,
        )
    }
}

// Helpers to extract Hz/BPM from mode
private fun uiState_strobeHz(mode: FlashMode) = (mode as? FlashMode.Strobe)?.hz?.toInt() ?: 5
private fun uiState_discoBpm(mode: FlashMode) = (mode as? FlashMode.Disco)?.bpm?.toInt() ?: 120

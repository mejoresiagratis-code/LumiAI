package com.lumiai.flashlight.feature.flash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.lumiai.flashlight.ui.components.*
import com.lumiai.flashlight.ui.theme.LumiColor

@Composable
fun FlashScreen(
    viewModel: FlashViewModel,
    onOpenSettings: () -> Unit,
    onOpenPro: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPro   = uiState.proStatus == ProStatus.Pro
    val isOn    = uiState.isFlashOn
    val mode    = uiState.currentMode

    // Screen-mode: full bright white background
    val isScreenMode = mode is FlashMode.Screen && isOn
    val bgColor by animateColorAsState(
        targetValue = if (isScreenMode) LumiColor.BeamColor else LumiColor.Navy950,
        animationSpec = tween(200),
        label = "bg",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        // ── Ambient top gradient (visible when ON, not screen mode) ────────
        if (isOn && !isScreenMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                LumiColor.Amber400.copy(alpha = 0.06f),
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

            // ── Top bar ────────────────────────────────────────────────────
            TopBar(
                isPro          = isPro,
                isScreenMode   = isScreenMode,
                onOpenSettings = onOpenSettings,
                onOpenPro      = onOpenPro,
            )

            Spacer(Modifier.weight(0.8f))

            // ── Status label ───────────────────────────────────────────────
            StatusLabel(isOn = isOn, mode = mode, isScreenMode = isScreenMode)

            Spacer(Modifier.height(32.dp))

            // ── HERO: Flash button ─────────────────────────────────────────
            FlashButton(
                isOn    = isOn,
                onClick = { viewModel.toggleFlash() },
                size    = 180.dp,
            )

            Spacer(Modifier.height(40.dp))

            // ── Contextual mode controls (slider) ──────────────────────────
            if (!isScreenMode) {
                ModeControls(
                    currentMode       = mode,
                    strobeHz          = uiState.strobeHz,
                    discoBpm          = uiState.discoBpm,
                    screenBrightness  = 1f,
                    onStrobeHzChange  = { viewModel.activateMode(FlashMode.Strobe(it)) },
                    onDiscoBpmChange  = { viewModel.activateMode(FlashMode.Disco(it)) },
                    onBrightnessChange = { /* handled via SettingsScreen */ },
                    modifier          = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.weight(1f))

            // ── Mode selector ──────────────────────────────────────────────
            if (!isScreenMode) {
                ModeSelector(
                    currentMode  = mode,
                    isPro        = isPro,
                    onModeSelect = { selected ->
                        if (selected.isPro && !isPro) onOpenPro()
                        else viewModel.activateMode(selected)
                    },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── AdMob banner (Free only, not in screen mode) ───────────────
            if (!isPro && !isScreenMode) {
                AdBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                )
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Logo wordmark
        Column {
            Text(
                text = "LUMI",
                fontSize = 18.sp,
                fontWeight = FontWeight.W900,
                letterSpacing = 0.2.sp,
                color = if (isScreenMode) LumiColor.Navy900
                        else LumiColor.Amber400,
            )
            Text(
                text = "AI",
                fontSize = 10.sp,
                fontWeight = FontWeight.W400,
                letterSpacing = 0.3.sp,
                color = if (isScreenMode) LumiColor.Navy900.copy(alpha = 0.5f)
                        else LumiColor.Gray500,
                modifier = Modifier.offset(y = (-4).dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Pro badge / upgrade button
            if (!isPro) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(LumiColor.Purple900.copy(alpha = 0.8f))
                        .clip(RoundedCornerShape(20.dp))
                        .then(Modifier.clickableNoRipple(onOpenPro))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = LumiIcons.Star,
                            contentDescription = null,
                            tint = LumiColor.Purple300,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Pro",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W600,
                            color = LumiColor.Purple300,
                        )
                    }
                }
            } else {
                // Pro badge — just a subtle indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(LumiColor.Purple900.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "PRO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W700,
                        letterSpacing = 0.1.sp,
                        color = LumiColor.Purple400,
                    )
                }
            }

            // Settings
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isScreenMode) LumiColor.Navy800.copy(alpha = 0.15f)
                        else LumiColor.Navy700.copy(alpha = 0.6f)
                    )
                    .then(Modifier.clickableNoRipple(onOpenSettings)),
            ) {
                Icon(
                    imageVector = LumiIcons.Settings,
                    contentDescription = "Ajustes",
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ── Status label ──────────────────────────────────────────────────────────────
@Composable
private fun StatusLabel(isOn: Boolean, mode: FlashMode, isScreenMode: Boolean) {
    val textColor = if (isScreenMode) LumiColor.Navy900.copy(alpha = 0.4f)
                   else if (isOn) LumiColor.Amber400.copy(alpha = 0.9f)
                   else LumiColor.Gray600

    val statusText = when {
        !isOn        -> "TAP TO TURN ON"
        isScreenMode -> "SCREEN MODE"
        mode is FlashMode.Sos    -> "SOS ACTIVE"
        mode is FlashMode.Strobe -> "STROBE · ${(mode as FlashMode.Strobe).hz.toInt()} HZ"
        mode is FlashMode.Disco  -> "DISCO · ${(mode as FlashMode.Disco).bpm.toInt()} BPM"
        else         -> "FLASHLIGHT ON"
    }

    AnimatedContent(
        targetState = statusText,
        transitionSpec = {
            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
        },
        label = "status_text",
    ) { text ->
        Text(
            text      = text,
            fontSize  = 11.sp,
            fontWeight = FontWeight.W600,
            letterSpacing = 0.15.sp,
            color     = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Extension ─────────────────────────────────────────────────────────────────
private fun Modifier.clickableNoRipple(onClick: () -> Unit) = this.then(
    Modifier.clickable(
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    )
)

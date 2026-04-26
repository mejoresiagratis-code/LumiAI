package com.lumiai.flashlight.feature.flash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalView
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
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
import com.lumiai.flashlight.core.util.MorseEncoder
import com.lumiai.flashlight.feature.flash.ScreenColor
import com.lumiai.flashlight.feature.flash.AutoOffOption
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

    val bgColor = if (isScreenMode) uiState.screenColor.color else LumiColor.Navy950

    // Apply screen brightness when Screen mode is active
    val view = LocalView.current
    if (isScreenMode) {
        val brightness = uiState.screenBrightness
        androidx.compose.runtime.LaunchedEffect(brightness) {
            val window = (view.context as? android.app.Activity)?.window
            window?.attributes = window?.attributes?.also { lp ->
                lp.screenBrightness = brightness
            }
        }
        // Restore full brightness when leaving Screen mode
        androidx.compose.runtime.DisposableEffect(Unit) {
            onDispose {
                val window = (view.context as? android.app.Activity)?.window
                window?.attributes = window?.attributes?.also { lp ->
                    lp.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }
    }

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


            // ── Screen color picker (Screen mode) ───────────────────────
            if (mode is com.lumiai.flashlight.core.domain.model.FlashMode.Screen) {
                ScreenColorPicker(
                    current  = uiState.screenColor,
                    onSelect = { viewModel.setScreenColor(it) },
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                )
            }

            // ── Morse text input ─────────────────────────────────────────
            if (mode is com.lumiai.flashlight.core.domain.model.FlashMode.MorseCustom) {
                val morseText by viewModel.morseText.collectAsState()
                MorseInputPanel(
                    text     = morseText,
                    onText   = { viewModel.updateMorseText(it) },
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                )
            }

            // ── Auto-off chip ────────────────────────────────────────────
            if (uiState.autoOffOption != AutoOffOption.NONE && isOn) {
                AutoOffChip(
                    option   = uiState.autoOffOption,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
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

// ── Screen color picker ───────────────────────────────────────────────────────
@Composable
private fun ScreenColorPicker(
    current: ScreenColor,
    onSelect: (ScreenColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ScreenColor.entries.forEach { sc ->
            val isSelected = sc == current
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(sc.color)
                    .then(if (isSelected) Modifier.border(
                        2.dp, LumiColor.White,
                        androidx.compose.foundation.shape.CircleShape
                    ) else Modifier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(sc) },
                    ),
            )
        }
    }
}

// ── Auto-off countdown chip ───────────────────────────────────────────────────
@Composable
private fun AutoOffChip(
    option: AutoOffOption,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(LumiColor.Navy700)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            "⏱ Auto-off: ${option.label}",
            fontSize  = 11.sp,
            color     = LumiColor.Gray400,
        )
    }
}

// ── Morse text input panel ────────────────────────────────────────────────────
@Composable
private fun MorseInputPanel(
    text: String,
    onText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val morsePreview = remember(text) {
        if (text.isBlank()) "" else MorseEncoder.toReadable(text)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LumiColor.Navy800)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "MORSE — TYPE YOUR MESSAGE",
            fontSize      = 9.sp,
            letterSpacing = 0.12.sp,
            color         = LumiColor.Gray600,
            fontWeight    = FontWeight.W500,
        )

        OutlinedTextField(
            value         = text,
            onValueChange = { onText(it.take(60)) }, // max 60 chars
            placeholder   = { Text("SOS, HELLO, your name...",
                fontSize = 13.sp, color = LumiColor.Gray600) },
            singleLine    = true,
            colors        = TextFieldDefaults.colors(
                focusedContainerColor      = LumiColor.Navy700,
                unfocusedContainerColor    = LumiColor.Navy700,
                focusedTextColor           = LumiColor.White,
                unfocusedTextColor         = LumiColor.White,
                focusedIndicatorColor      = LumiColor.Amber400.copy(.6f),
                unfocusedIndicatorColor    = LumiColor.Navy600,
                cursorColor                = LumiColor.Amber400,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier        = Modifier.fillMaxWidth(),
        )

        // Morse preview
        if (morsePreview.isNotBlank()) {
            Text(
                text       = morsePreview,
                fontSize   = 11.sp,
                color      = LumiColor.Amber400.copy(.7f),
                letterSpacing = 0.06.sp,
                lineHeight = 16.sp,
            )
        }
        Text(
            "${text.length}/60 chars",
            fontSize = 9.sp,
            color    = LumiColor.Gray600,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

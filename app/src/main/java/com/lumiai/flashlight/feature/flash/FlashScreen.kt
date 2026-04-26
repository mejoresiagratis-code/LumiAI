package com.lumiai.flashlight.feature.flash

import android.app.Activity
import android.view.WindowManager
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
    val uiState by viewModel.uiState.collectAsState()
    [span_1](start_span)val morseText by viewModel.morseText.collectAsState() // Corregido: Movido aquí para evitar Unresolved reference[span_1](end_span)
    val isPro = uiState.proStatus == ProStatus.Pro
    [span_2](start_span)val isOn = uiState.isFlashOn[span_2](end_span)
    val mode = uiState.currentMode
    val isScreenMode = mode is FlashMode.Screen && isOn

    val bgColor = if (isScreenMode) uiState.screenColor.color else LumiColor.Navy950

    val view = LocalView.current
    if (isScreenMode) {
        val brightness = uiState.screenBrightness
        LaunchedEffect(brightness) {
            [span_3](start_span)val window = (view.context as? Activity)?.window[span_3](end_span)
            window?.attributes = window?.attributes?.also { lp ->
                lp.screenBrightness = brightness
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                [span_4](start_span)val window = (view.context as? Activity)?.window[span_4](end_span)
                window?.attributes = window?.attributes?.also { lp ->
                    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            [span_5](start_span).background(bgColor),[span_5](end_span)
    ) {
        val screenH = maxHeight

        if (isOn && !isScreenMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        [span_6](start_span)Brush.verticalGradient([span_6](end_span)
                            [span_7](start_span)listOf(LumiColor.Amber400.copy(alpha = 0.05f), Color.Transparent)[span_7](end_span)
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                [span_8](start_span).verticalScroll(rememberScrollState()),[span_8](end_span)
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(
                isPro = isPro,
                isScreenMode = isScreenMode,
                onOpenSettings = onOpenSettings,
                [span_9](start_span)onOpenPro = onOpenPro,[span_9](end_span)
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = statusLabel(isOn, mode, uiState),
                fontSize = 10.sp,
                [span_10](start_span)fontWeight = FontWeight.W500,[span_10](end_span)
                letterSpacing = 0.14.sp,
                color = when {
                    isScreenMode -> LumiColor.Navy900.copy(.35f)
                    [span_11](start_span)isOn -> LumiColor.Amber400.copy(.7f)[span_11](end_span)
                    else -> LumiColor.Gray600
                },
            )

            [span_12](start_span)val btnSize = (screenH * 0.28f).coerceIn(130.dp, 180.dp)[span_12](end_span)
            Spacer(Modifier.height(20.dp))
            FlashButton(
                isOn = isOn,
                onClick = { viewModel.toggleFlash() },
                [span_13](start_span)size = btnSize,[span_13](end_span)
            )
            Spacer(Modifier.height(28.dp))

            if (!isScreenMode) {
                ModePanel(
                    currentMode = mode,
                    [span_14](start_span)strobeHz = uiState.strobeHz,[span_14](end_span)
                    discoBpm = uiState.discoBpm,
                    onModeSelect = { viewModel.activateMode(it) },
                    [span_15](start_span)onStrobeHzChange = { viewModel.updateStrobeHz(it) },[span_15](end_span)
                    onDiscoBpmChange = { viewModel.updateDiscoBpm(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                [span_16](start_span)Spacer(Modifier.height(8.dp))[span_16](end_span)
            }

            if (mode is FlashMode.Screen) {
                ScreenColorPicker(
                    current = uiState.screenColor,
                    [span_17](start_span)onSelect = { viewModel.setScreenColor(it) },[span_17](end_span)
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                )
            }

            if (mode is FlashMode.MorseCustom) {
                MorseInputPanel(
                    text = morseText,
                    onText = { viewModel.updateMorseText(it) },
                    [span_18](start_span)modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),[span_18](end_span)
                )
            }

            if (uiState.autoOffOption != AutoOffOption.NONE && isOn) {
                AutoOffChip(
                    [span_19](start_span)option = uiState.autoOffOption,[span_19](end_span)
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (!isPro && !isScreenMode) {
                [span_20](start_span)AdBanner(modifier = Modifier.navigationBarsPadding())[span_20](end_span)
            } else {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

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
            [span_21](start_span).padding(horizontal = 16.dp, vertical = 10.dp),[span_21](end_span)
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                [span_22](start_span)"LUMI·AI",[span_22](end_span)
                fontSize = 14.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = 0.16.sp,
                [span_23](start_span)color = if (isScreenMode) LumiColor.Navy800 else LumiColor.White,[span_23](end_span)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isPro) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        [span_24](start_span).clip(CircleShape)[span_24](end_span)
                        .background(LumiColor.Navy800)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            [span_25](start_span)indication = null,[span_25](end_span)
                            onClick = onOpenPro,
                        ),
                ) {
                    Icon(
                        [span_26](start_span)LumiIcons.Star,[span_26](end_span)
                        contentDescription = "Pro",
                        tint = LumiColor.Purple300,
                        [span_27](start_span)modifier = Modifier.size(14.dp),[span_27](end_span)
                    )
                }
            } else {
                Box(
                    [span_28](start_span)modifier = Modifier[span_28](end_span)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LumiColor.Navy800)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        "PRO",
                        fontSize = 9.sp,
                        [span_29](start_span)fontWeight = FontWeight.W700,[span_29](end_span)
                        letterSpacing = 0.1.sp,
                        color = LumiColor.Purple400,
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    [span_30](start_span).clip(CircleShape)[span_30](end_span)
                    .background(LumiColor.Navy800)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        [span_31](start_span)indication = null,[span_31](end_span)
                        onClick = onOpenSettings,
                    ),
            ) {
                Icon(
                    [span_32](start_span)LumiIcons.Settings,[span_32](end_span)
                    contentDescription = "Ajustes",
                    tint = LumiColor.Gray500,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

private fun statusLabel(isOn: Boolean, mode: FlashMode, uiState: FlashUiState): String = when {
    !isOn -> "TAP TO TURN ON"
    mode is FlashMode.Screen -> "SCREEN MODE"
    mode is FlashMode.Sos -> "SOS · · · — — —"
    [span_33](start_span)mode is FlashMode.Strobe -> "STROBE · ${uiState.strobeHz.toInt()} HZ"[span_33](end_span)
    mode is FlashMode.Disco -> "DISCO · ${uiState.discoBpm.toInt()} BPM"
    mode is FlashMode.SmartBrightness -> "◎ SMART MODE"
    mode is FlashMode.ReadingMode -> "☽ READING MODE"
    mode is FlashMode.AmbientSmart -> "◈ AMBIENT MODE"
    mode is FlashMode.CustomRhythm -> "⬡ CUSTOM RHYTHM"
    mode is FlashMode.SleepTimer -> "◌ SLEEP TIMER"
    [span_34](start_span)else -> "FLASHLIGHT ON"[span_34](end_span)
}

@Composable
private fun ScreenColorPicker(
    current: ScreenColor,
    onSelect: (ScreenColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        [span_35](start_span)ScreenColor.entries.forEach { sc ->[span_35](end_span)
            val isSelected = sc == current
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    [span_36](start_span).background(sc.color)[span_36](end_span)
                    .then(
                        [span_37](start_span)if (isSelected) Modifier.border(2.dp, LumiColor.White, CircleShape)[span_37](end_span)
                        else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        [span_38](start_span)onClick = { onSelect(sc) },[span_38](end_span)
                    ),
            )
        }
    }
}

@Composable
private fun AutoOffChip(
    option: AutoOffOption,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            [span_39](start_span).background(LumiColor.Navy700)[span_39](end_span)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            "⏱ Auto-off: ${option.label}",
            fontSize = 11.sp,
            color = LumiColor.Gray400,
        )
    }
}

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
            [span_40](start_span).padding(14.dp),[span_40](end_span)
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "MORSE — TYPE YOUR MESSAGE",
            fontSize = 9.sp,
            letterSpacing = 0.12.sp,
            [span_41](start_span)color = LumiColor.Gray600,[span_41](end_span)
            fontWeight = FontWeight.W500,
        )

        OutlinedTextField(
            value = text,
            onValueChange = { onText(it.take(60)) },
            placeholder = {
                Text(
                    [span_42](start_span)"SOS, HELLO, your name...",[span_42](end_span)
                    fontSize = 13.sp, color = LumiColor.Gray600
                )
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = LumiColor.Navy700,
                [span_43](start_span)unfocusedContainerColor = LumiColor.Navy700,[span_43](end_span)
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = LumiColor.Amber400.copy(.6f),
                [span_44](start_span)unfocusedIndicatorColor = LumiColor.Navy600,[span_44](end_span)
                cursorColor = LumiColor.Amber400,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        if (morsePreview.isNotBlank()) {
            Text(
                text = morsePreview,
                fontSize = 11.sp,
                color = LumiColor.Amber400.copy(.7f),
                [span_45](start_span)letterSpacing = 0.06.sp,[span_45](end_span)
                lineHeight = 16.sp,
            )
        }
        Text(
            "${text.length}/60 chars",
            fontSize = 9.sp,
            color = LumiColor.Gray600,
            [span_46](start_span)modifier = Modifier.align(Alignment.End),[span_46](end_span)
        )
    }
}

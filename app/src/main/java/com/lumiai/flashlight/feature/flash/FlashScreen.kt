package com.lumiai.flashlight.feature.flash

import com.lumiai.flashlight.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.lumiai.flashlight.feature.flash.AutoOffOption
import com.lumiai.flashlight.feature.flash.ScreenColor
import com.lumiai.flashlight.core.domain.model.ProStatus
import com.lumiai.flashlight.core.util.MorseEncoder
import com.lumiai.flashlight.ui.components.AdBanner
import com.lumiai.flashlight.ui.components.FlashButton
import com.lumiai.flashlight.ui.components.LumiIcons
import com.lumiai.flashlight.ui.components.ModePanel
import com.lumiai.flashlight.ui.theme.LumiColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashScreen(
    viewModel: FlashViewModel,
    onOpenSettings: () -> Unit,
    onOpenPro: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val morseText by viewModel.morseText.collectAsState() // Corregido: Movido aquí para evitar Unresolved reference
    val isPro = uiState.proStatus == ProStatus.Pro
    val isOn = uiState.isFlashOn
    val mode = uiState.currentMode
    val isScreenMode = mode is FlashMode.Screen && isOn

    val screenColor   by viewModel.screenColor.collectAsState()
    val torchIntensity by viewModel.torchIntensity.collectAsState()
    val bgColor = if (isScreenMode) screenColor.color else LumiColor.Navy950

    val view = LocalView.current
    if (isScreenMode) {
        val brightness = uiState.screenBrightness
        LaunchedEffect(brightness) {
            val window = (view.context as? Activity)?.window
            window?.attributes = window?.attributes?.also { lp ->
                lp.screenBrightness = brightness
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                val window = (view.context as? Activity)?.window
                window?.attributes = window?.attributes?.also { lp ->
                    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }
    }

    // ── Config BottomSheet ────────────────────────────────────────────────────
    val showSheet by viewModel.showConfigSheet.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest  = { viewModel.closeConfigSheet() },
            sheetState        = sheetState,
            containerColor    = LumiColor.Navy800,
            dragHandle        = null,
        ) {
            ModeConfigSheet(
                mode          = mode,
                uiState       = uiState,
                morseText     = morseText,
                onStrobeHz    = { viewModel.updateStrobeHz(it) },
                onDiscoBpm    = { viewModel.updateDiscoBpm(it) },
                onMorseText   = { viewModel.updateMorseText(it) },
                screenColor   = screenColor,
                onScreenColor = { viewModel.setScreenColor(it) },
                onDismiss     = { viewModel.closeConfigSheet() },
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        val screenH = maxHeight

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
            TopBar(
                isPro = isPro,
                isScreenMode = isScreenMode,
                onOpenSettings = onOpenSettings,
                onOpenPro = onOpenPro,
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = statusLabel(isOn, mode, uiState),
                fontSize = 10.sp,
                fontWeight = FontWeight.W500,
                letterSpacing = 0.14.sp,
                color = when {
                    isScreenMode -> LumiColor.Navy900.copy(.35f)
                    isOn -> LumiColor.Amber400.copy(.7f)
                    else -> LumiColor.Gray600
                },
            )

            val btnSize = (screenH * 0.28f).coerceIn(130.dp, 180.dp)
            Spacer(Modifier.height(20.dp))
            FlashButton(
                isOn = isOn,
                onClick = { viewModel.toggleFlash() },
                size = btnSize,
            )
            Spacer(Modifier.height(28.dp))

            if (!isScreenMode) {
                ModePanel(
                    currentMode      = mode,
                    strobeHz         = uiState.strobeHz,
                    discoBpm         = uiState.discoBpm,
                    onModeSelect     = { viewModel.activateMode(it) },
                    onModeConfig     = {
                        viewModel.activateMode(it)
                        viewModel.openConfigSheet()
                    },
                    onStrobeHzChange = { viewModel.updateStrobeHz(it) },
                    onDiscoBpmChange = { viewModel.updateDiscoBpm(it) },
                    modifier         = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }

            if (uiState.autoOffOption != AutoOffOption.NONE && isOn) {
                AutoOffChip(
                    option = uiState.autoOffOption,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (isScreenMode) {
                // Quick color picker strip at bottom when Screen mode is ON
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 32.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                ) {
                    ScreenColor.entries.forEach { sc ->
                        val sel = sc == screenColor
                        Box(
                            modifier = Modifier
                                .size(if (sel) 40.dp else 32.dp)
                                .clip(CircleShape)
                                .background(sc.color.copy(alpha = if (sel) 1f else 0.6f))
                                .then(if (sel) Modifier.border(3.dp, LumiColor.White, CircleShape) else Modifier)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { viewModel.setScreenColor(sc) },
                                ),
                        )
                    }
                }
            } else {
                Spacer(Modifier.navigationBarsPadding())
            }

            // Bottom padding so content scrolls above the ad banner
            if (!isPro && !isScreenMode) Spacer(Modifier.height(90.dp))
        }

        // ── AdBanner overlay ────────────────────────────────────────────────
        // Always visible for Free users, overlays the scroll content at bottom
        if (!isPro && !isScreenMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(LumiColor.Navy950.copy(alpha = 0.97f))
                    .navigationBarsPadding(),
            ) {
                AdBanner()
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                "LUMI·AI",
                fontSize = 14.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = 0.16.sp,
                color = if (isScreenMode) LumiColor.Navy800 else LumiColor.White,
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
                        tint = LumiColor.Purple300,
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
                        fontSize = 9.sp,
                        fontWeight = FontWeight.W700,
                        letterSpacing = 0.1.sp,
                        color = LumiColor.Purple400,
                    )
                }
            }

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
                    tint = LumiColor.Gray500,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun statusLabel(isOn: Boolean, mode: FlashMode, uiState: FlashUiState): String = when {
    !isOn -> stringResource(R.string.status_tap_to_turn_on)
    mode is FlashMode.Screen -> stringResource(R.string.status_screen_mode)
    mode is FlashMode.Sos -> "SOS · · · — — —"
    mode is FlashMode.Strobe -> "STROBE · ${uiState.strobeHz.toInt()} HZ"
    mode is FlashMode.Disco -> "DISCO · ${uiState.discoBpm.toInt()} BPM"
    mode is FlashMode.SmartBrightness -> "◎ SMART MODE"
    mode is FlashMode.ReadingMode -> "☽ READING MODE"
    mode is FlashMode.AmbientSmart -> "◈ AMBIENT MODE"
    mode is FlashMode.CustomRhythm -> "⬡ CUSTOM RHYTHM"
    mode is FlashMode.SleepTimer -> "◌ SLEEP TIMER"
    else -> stringResource(R.string.status_flashlight_on)
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
        ScreenColor.entries.forEach { sc ->
            val isSelected = sc == current
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(sc.color)
                    .then(
                        if (isSelected) Modifier.border(2.dp, LumiColor.White, CircleShape)
                        else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(sc) },
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
            .background(LumiColor.Navy700)
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
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "MORSE — TYPE YOUR MESSAGE",
            fontSize = 9.sp,
            letterSpacing = 0.12.sp,
            color = LumiColor.Gray600,
            fontWeight = FontWeight.W500,
        )

        OutlinedTextField(
            value = text,
            onValueChange = { onText(it.take(60)) },
            placeholder = {
                Text(
                    "SOS, HELLO, your name...",
                    fontSize = 13.sp, color = LumiColor.Gray600
                )
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = LumiColor.Navy700,
                unfocusedContainerColor = LumiColor.Navy700,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = LumiColor.Amber400.copy(.6f),
                unfocusedIndicatorColor = LumiColor.Navy600,
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
                letterSpacing = 0.06.sp,
                lineHeight = 16.sp,
            )
        }
        Text(
            "${text.length}/60 chars",
            fontSize = 9.sp,
            color = LumiColor.Gray600,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

// ── Mode Config Bottom Sheet ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeConfigSheet(
    mode: FlashMode,
    uiState: FlashUiState,
    morseText: String,
    screenColor: ScreenColor = ScreenColor.WHITE,
    torchIntensity: Float = 1.0f,
    smartSpeed: Float = 1.0f,
    sleepMinutes: Int = 3,
    micSensitivity: Float = 1.0f,
    onTorchIntensity: (Float) -> Unit = {},
    onScreenBrightness: (Float) -> Unit = {},
    onStrobeHz: (Float) -> Unit,
    onDiscoBpm: (Float) -> Unit,
    onMorseText: (String) -> Unit,
    onScreenColor: (ScreenColor) -> Unit,
    onSmartSpeed: (Float) -> Unit = {},
    onSleepMinutes: (Int) -> Unit = {},
    onMicSensitivity: (Float) -> Unit = {},
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Handle + title
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LumiColor.Gray700)
                .align(Alignment.CenterHorizontally)
        )

        val title = when (mode) {
            is FlashMode.Steady      -> "Steady — Intensity"
            is FlashMode.Strobe      -> stringResource(R.string.config_strobe_title)
            is FlashMode.Disco       -> stringResource(R.string.config_disco_title)
            is FlashMode.MorseCustom -> stringResource(R.string.config_morse_title)
            is FlashMode.Screen      -> stringResource(R.string.config_screen_title)
            is FlashMode.Sos         -> "SOS — Intensity"
            else -> return
        }
        Text(
            title,
            fontSize   = 16.sp,
            fontWeight = FontWeight.W600,
            color      = LumiColor.White,
        )

        // ── Intensity slider for torch modes ─────────────────────────────────
        val hasTorchIntensity = mode is FlashMode.Steady || mode is FlashMode.Strobe ||
                                mode is FlashMode.Disco  || mode is FlashMode.Sos    ||
                                mode is FlashMode.MorseCustom
        if (hasTorchIntensity) {
            var intensity by remember { mutableFloatStateOf(torchIntensity) }
            val intensityPct = (intensity * 100).toInt()
            Text("FLASH INTENSITY", fontSize = 10.sp, color = LumiColor.Gray600,
                fontWeight = FontWeight.W500, letterSpacing = 0.1.sp)
            Text("$intensityPct%", fontSize = 24.sp,
                fontWeight = FontWeight.W700, color = LumiColor.Amber400)
            Slider(
                value = intensity,
                onValueChange = { intensity = it },
                onValueChangeFinished = { onTorchIntensity(intensity) },
                valueRange = 0.1f..1.0f,
                steps = 17,
                colors = SliderDefaults.colors(
                    thumbColor = LumiColor.Amber400,
                    activeTrackColor = LumiColor.Amber400,
                    inactiveTrackColor = LumiColor.Navy600,
                ),
            )
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("10%", fontSize = 11.sp, color = LumiColor.Gray600)
                Text("100%", fontSize = 11.sp, color = LumiColor.Gray600)
            }
            if (mode is FlashMode.Steady || mode is FlashMode.Sos) {
                return  // Steady/SOS only have intensity — no other config
            }
        }

        when (mode) {
            is FlashMode.Strobe -> {
                var hz by remember { mutableFloatStateOf(uiState.strobeHz) }
                Text("${hz.toInt()} Hz", fontSize = 28.sp,
                    fontWeight = FontWeight.W700, color = LumiColor.Amber400)
                Slider(
                    value            = hz,
                    onValueChange    = { hz = it },
                    onValueChangeFinished = { onStrobeHz(hz.toInt().toFloat()) },
                    valueRange       = 1f..20f,
                    steps            = 18,
                    colors           = SliderDefaults.colors(
                        thumbColor            = LumiColor.Amber400,
                        activeTrackColor      = LumiColor.Amber400,
                        inactiveTrackColor    = LumiColor.Navy600,
                    ),
                )
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("1 Hz", fontSize = 11.sp, color = LumiColor.Gray600)
                    Text("20 Hz", fontSize = 11.sp, color = LumiColor.Gray600)
                }
            }
            is FlashMode.Disco -> {
                var bpm by remember { mutableFloatStateOf(uiState.discoBpm) }
                Text("${bpm.toInt()} BPM", fontSize = 28.sp,
                    fontWeight = FontWeight.W700, color = LumiColor.Amber400)
                Slider(
                    value            = bpm,
                    onValueChange    = { bpm = it },
                    onValueChangeFinished = { onDiscoBpm(bpm.toInt().toFloat()) },
                    valueRange       = 60f..200f,
                    steps            = 27,
                    colors           = SliderDefaults.colors(
                        thumbColor            = LumiColor.Amber400,
                        activeTrackColor      = LumiColor.Amber400,
                        inactiveTrackColor    = LumiColor.Navy600,
                    ),
                )
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("60 BPM", fontSize = 11.sp, color = LumiColor.Gray600)
                    Text("200 BPM", fontSize = 11.sp, color = LumiColor.Gray600)
                }
            }
            is FlashMode.MorseCustom -> {
                val morsePreview = remember(morseText) {
                    if (morseText.isBlank()) "" else MorseEncoder.toReadable(morseText)
                }
                OutlinedTextField(
                    value         = morseText,
                    onValueChange = { onMorseText(it.take(60)) },
                    placeholder   = { Text("Type your message...",
                        fontSize = 14.sp, color = LumiColor.Gray600) },
                    singleLine    = false,
                    maxLines      = 3,
                    colors        = TextFieldDefaults.colors(
                        focusedContainerColor      = LumiColor.Navy700,
                        unfocusedContainerColor    = LumiColor.Navy700,
                        focusedTextColor           = LumiColor.White,
                        unfocusedTextColor         = LumiColor.White,
                        focusedIndicatorColor      = LumiColor.Amber400.copy(.6f),
                        unfocusedIndicatorColor    = LumiColor.Navy600,
                        cursorColor                = LumiColor.Amber400,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (morsePreview.isNotBlank()) {
                    Text(
                        morsePreview,
                        fontSize  = 12.sp,
                        color     = LumiColor.Amber400.copy(.7f),
                        lineHeight = 18.sp,
                    )
                }
                Text("${morseText.length}/60",
                    fontSize = 10.sp, color = LumiColor.Gray600,
                    modifier = Modifier.align(Alignment.End))
            }
            is FlashMode.AmbientSmart -> {
                Text("AMBIENT SENSITIVITY", fontSize = 10.sp, color = LumiColor.Gray600,
                    fontWeight = FontWeight.W500, letterSpacing = 0.1.sp)
                Spacer(Modifier.height(4.dp))
                Text("Mode reads lux once at activation and stays steady.",
                    fontSize = 12.sp, color = LumiColor.Gray500, lineHeight = 17.sp)
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LumiColor.Navy700)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                // Re-activate to re-read lux
                                onDismiss()
                            }
                        )
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Re-read ambient light now", fontSize = 14.sp,
                        color = LumiColor.Amber400, fontWeight = FontWeight.W600)
                }
                Spacer(Modifier.height(8.dp))
                Text("Tap to re-sample the current light level and adjust.",
                    fontSize = 11.sp, color = LumiColor.Gray600)
            }
            is FlashMode.CustomRhythm -> {
                Text("CUSTOM RHYTHM", fontSize = 10.sp, color = LumiColor.Gray600,
                    fontWeight = FontWeight.W500, letterSpacing = 0.1.sp)
                Spacer(Modifier.height(4.dp))
                Text("Pattern changes automatically based on time of day:",
                    fontSize = 12.sp, color = LumiColor.Gray500)
                Spacer(Modifier.height(10.dp))
                val patterns = listOf(
                    "6–9h" to "Fast triple · active morning",
                    "10–14h" to "Steady double · work hours",
                    "15–19h" to "Slow pulse · afternoon",
                    "20–22h" to "Very slow · evening wind-down",
                    "23–5h" to "Ultra slow · night",
                )
                patterns.forEach { (time, desc) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(time, fontSize = 12.sp, fontWeight = FontWeight.W600, color = LumiColor.Amber400)
                        Text(desc, fontSize = 12.sp, color = LumiColor.Gray500)
                    }
                }
            }
            is FlashMode.SmartBrightness -> {
                Text("PULSE SPEED", fontSize = 10.sp, color = LumiColor.Gray600,
                    fontWeight = FontWeight.W500, letterSpacing = 0.1.sp)
                val speedLabels = listOf("0.5×" to 0.5f, "1×" to 1.0f, "1.5×" to 1.5f, "2×" to 2.0f)
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    speedLabels.forEach { (label, v) ->
                        val sel = kotlin.math.abs(smartSpeed - v) < 0.1f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) LumiColor.Amber400 else LumiColor.Navy700)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onSmartSpeed(v) }
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.W600,
                                color = if (sel) LumiColor.Navy950 else LumiColor.White)
                        }
                    }
                }
                Text("Faster = more reactive outdoors. Slower = more subtle indoors.",
                    fontSize = 11.sp, color = LumiColor.Gray600)
            }
            is FlashMode.SleepTimer -> {
                Text("FADE DURATION", fontSize = 10.sp, color = LumiColor.Gray600,
                    fontWeight = FontWeight.W500, letterSpacing = 0.1.sp)
                val durations = listOf("1 min" to 1, "3 min" to 3, "5 min" to 5, "10 min" to 10)
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    durations.forEach { (label, v) ->
                        val sel = sleepMinutes == v
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) LumiColor.Amber400 else LumiColor.Navy700)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onSleepMinutes(v) }
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.W600,
                                color = if (sel) LumiColor.Navy950 else LumiColor.White)
                        }
                    }
                }
            }
            is FlashMode.Music, is FlashMode.Voice -> {
                val modeLabel = if (mode is FlashMode.Music) "BEAT SENSITIVITY" else "SOUND SENSITIVITY"
                Text(modeLabel, fontSize = 10.sp, color = LumiColor.Gray600,
                    fontWeight = FontWeight.W500, letterSpacing = 0.1.sp)
                var sens by remember { mutableFloatStateOf(micSensitivity) }
                val sensLabel = when {
                    sens < 0.8f -> "Low — only loud sounds"
                    sens < 1.3f -> "Medium — normal sounds"
                    else        -> "High — very sensitive"
                }
                Text(sensLabel, fontSize = 14.sp, color = LumiColor.Amber400, fontWeight = FontWeight.W600)
                Slider(
                    value = sens,
                    onValueChange = { sens = it },
                    onValueChangeFinished = { onMicSensitivity(sens) },
                    valueRange = 0.5f..2.0f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = LumiColor.Amber400,
                        activeTrackColor = LumiColor.Amber400,
                        inactiveTrackColor = LumiColor.Navy600,
                    ),
                )
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Low", fontSize = 11.sp, color = LumiColor.Gray600)
                    Text("High", fontSize = 11.sp, color = LumiColor.Gray600)
                }
            }
            is FlashMode.Screen -> {
                // Screen brightness slider
                var brightness by remember { mutableFloatStateOf(uiState.screenBrightness) }
                Text("SCREEN BRIGHTNESS", fontSize = 10.sp, color = LumiColor.Gray600,
                    fontWeight = FontWeight.W500, letterSpacing = 0.1.sp)
                Text("${(brightness * 100).toInt()}%", fontSize = 24.sp,
                    fontWeight = FontWeight.W700, color = LumiColor.Amber400)
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    onValueChangeFinished = {
                        onScreenBrightness(brightness)
                    },
                    valueRange = 0.05f..1.0f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = LumiColor.Amber400,
                        activeTrackColor = LumiColor.Amber400,
                        inactiveTrackColor = LumiColor.Navy600,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text("Tap to pick a color",
                    fontSize = 13.sp, color = LumiColor.Gray500)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ScreenColor.entries.forEach { sc ->
                        val sel = sc == screenColor
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(sc.color)
                                .then(if (sel) Modifier.border(3.dp, LumiColor.White, CircleShape) else Modifier)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null,
                                    onClick           = { onScreenColor(sc); onDismiss() },
                                ),
                        )
                    }
                }
            }
            else -> {}
        }
    }
}

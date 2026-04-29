package com.lumiai.flashlight.feature.flash

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlinx.coroutines.delay
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.lumiai.flashlight.feature.flash.ScreenEffect
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.animation.core.tween
import com.lumiai.flashlight.R
import com.lumiai.flashlight.core.util.StrobePattern
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
import androidx.activity.compose.BackHandler
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

    LaunchedEffect(Unit) {
        viewModel.showPaywallEvent.collect { onOpenPro() }
    }
    val morseText by viewModel.morseText.collectAsState() // Corregido: Movido aquí para evitar Unresolved reference
    val isPro = uiState.proStatus == ProStatus.Pro
    val isOn = uiState.isFlashOn
    val mode = uiState.currentMode
    val isScreenMode = mode is FlashMode.Screen && isOn

    // Physical back button turns off Screen mode
    BackHandler(enabled = isScreenMode) {
        viewModel.toggleFlash()
    }

    // Auto-hide: hides 3s after LAST INTERACTION, not 3s after appearing
    var uiVisible by remember { mutableStateOf(true) }
    var lastInteractionMs by remember { mutableStateOf(System.currentTimeMillis()) }

    // Helper called on every user interaction to reset the idle timer
    fun onUserInteraction() {
        lastInteractionMs = System.currentTimeMillis()
        uiVisible = true
    }

    LaunchedEffect(isScreenMode) {
        if (!isScreenMode) { uiVisible = true; return@LaunchedEffect }
        // Poll every 500ms — hide when idle for 3000ms
        while (true) {
            delay(500L)
            if (!isScreenMode) break
            if (uiVisible && System.currentTimeMillis() - lastInteractionMs >= 3000L) {
                uiVisible = false
            }
        }
    }
    val uiAlpha by animateFloatAsState(
        targetValue = if (!isScreenMode || uiVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(400),
        label = "uiAlpha",
    )

    val screenColor   by viewModel.screenColor.collectAsState()
    val torchIntensity by viewModel.torchIntensity.collectAsState()
    val morseSpeed     by viewModel.morseSpeed.collectAsState()
    val strobePattern  by viewModel.strobePattern.collectAsState()
    val screenEffect   by viewModel.screenEffect.collectAsState()
    val screenTab      by viewModel.screenTab.collectAsState()
    val screenHue      by viewModel.screenHue.collectAsState()
    val screenTemp     by viewModel.screenTemp.collectAsState()
    // Effect engine drives bgColor when an effect is active in Screen mode
    var effectBgColor by remember { mutableStateOf(screenColor.color) }
    // Sync effectBgColor when screenColor changes (solid/hue)
    LaunchedEffect(screenColor) {
        if (screenEffect == null) effectBgColor = screenColor.color
    }
    // Apply temperature color when screenTemp changes
    LaunchedEffect(screenTemp) {
        if (screenEffect == null) {
            val r = 1.0f
            val g = (0.78f + screenTemp * 0.22f).coerceIn(0f, 1f)
            val b = (screenTemp * 0.90f).coerceIn(0f, 1f)
            effectBgColor = androidx.compose.ui.graphics.Color(red = r, green = g, blue = b)
        }
    }
    // Apply hue color when screenHue changes
    LaunchedEffect(screenHue) {
        if (screenEffect == null) {
            val hsv = floatArrayOf(screenHue, 1f, 1f)
            effectBgColor = androidx.compose.ui.graphics.Color(android.graphics.Color.HSVToColor(hsv))
        }
    }

    val bgColor = if (isScreenMode) effectBgColor else LumiColor.Navy950

    if (isScreenMode) {
        ScreenEffectEngine(
            effect = screenEffect,
            onColorChange = { effectBgColor = it },
        )
    }

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
            val smartSpeed     by viewModel.smartSpeed.collectAsState()
                val sleepMinutes   by viewModel.sleepMinutes.collectAsState()
                val micSensitivity by viewModel.micSensitivity.collectAsState()
                ModeConfigSheet(
                mode             = mode,
                uiState          = uiState,
                morseText        = morseText,
                screenColor      = screenColor,
                torchIntensity   = torchIntensity,
                morseSpeed       = morseSpeed,
                strobePattern    = strobePattern,
                smartSpeed       = smartSpeed,
                sleepMinutes     = sleepMinutes,
                micSensitivity   = micSensitivity,
                onStrobeHz       = { viewModel.updateStrobeHz(it) },
                onDiscoBpm       = { viewModel.updateDiscoBpm(it) },
                onMorseText      = { viewModel.updateMorseText(it) },
                onScreenColor    = { viewModel.setScreenColor(it) },
                onScreenBrightness = { viewModel.setScreenBrightness(it) },
                onTorchIntensity = { viewModel.setTorchIntensity(it) },
                onMorseSpeed     = { viewModel.setMorseSpeed(it) },
                onStrobePattern  = { viewModel.setStrobePattern(it) },
                onSmartSpeed     = { viewModel.setSmartSpeed(it) },
                onSleepMinutes   = { viewModel.setSleepMinutes(it) },
                onMicSensitivity = { viewModel.setMicSensitivity(it) },
                onDismiss        = { viewModel.closeConfigSheet() },
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .then(if (isScreenMode) Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onUserInteraction() },
            ) else Modifier),
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
                modifier = Modifier.graphicsLayer { alpha = uiAlpha },
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isScreenMode) "" else statusLabel(isOn, mode, uiState),
                fontSize = 10.sp,
                fontWeight = FontWeight.W500,
                letterSpacing = 0.14.sp,
                color = when {
                    isOn -> LumiColor.Amber400.copy(.7f)
                    else -> LumiColor.Gray600
                },
            )

            val btnSize = (screenH * 0.28f).coerceIn(130.dp, 180.dp)
            if (isScreenMode) {
                // ── Screen ON: minimal close button ──────────────────────────
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .graphicsLayer { alpha = uiAlpha }
                        .background(LumiColor.Navy950.copy(alpha = 0.15f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { if (uiVisible) viewModel.toggleFlash() else onUserInteraction() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", fontSize = 20.sp, fontWeight = FontWeight.W300,
                        color = LumiColor.Navy950.copy(alpha = 0.4f))
                }
                Spacer(Modifier.height(12.dp))
            } else {
                Spacer(Modifier.height(20.dp))
                FlashButton(
                    isOn = isOn,
                    onClick = { viewModel.toggleFlash() },
                    size = btnSize,
                )
                Spacer(Modifier.height(28.dp))
            }

            if (!isScreenMode) {
                ModePanel(
                    currentMode      = mode,
                    isConfigSheetOpen = showSheet,
                    torchIntensity         = torchIntensity,
                    onTorchIntensityChange  = { viewModel.setTorchIntensity(it) },
                    screenBrightness       = uiState.screenBrightness,
                    onScreenBrightnessChange = { viewModel.setScreenBrightness(it) },
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
                Spacer(Modifier.weight(1f))
                // ── Tabbed control panel ─────────────────────────────────────
                ScreenControlPanel(
                    screenColor    = screenColor,
                    screenEffect   = screenEffect,
                    screenTab      = screenTab,
                    screenHue      = screenHue,
                    screenTemp     = screenTemp,
                    brightness     = uiState.screenBrightness,
                    uiAlpha        = uiAlpha,
                    onColorSelect  = { viewModel.setScreenColor(it) },
                    onEffectSelect = { viewModel.setScreenEffect(it) },
                    onTabSelect    = { viewModel.setScreenTab(it) },
                    onHueChange    = { viewModel.setScreenHue(it) },
                    onTempChange   = { viewModel.setScreenTemp(it) },
                    onBrightness   = { viewModel.setScreenBrightness(it) },
                    onInteraction  = { onUserInteraction() },
                )
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
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




// ── Screen mode: FX engine — drives screenColor changes for animated effects ─
@Composable
fun ScreenEffectEngine(
    effect: ScreenEffect?,
    onColorChange: (androidx.compose.ui.graphics.Color) -> Unit,
) {
    // Pre-compute first frame per effect so it's applied before cancelling prev
    val firstFrameColor: androidx.compose.ui.graphics.Color? = remember(effect) {
        when (effect) {
            ScreenEffect.POLICE  -> androidx.compose.ui.graphics.Color(0xFFFF3B30L)
            ScreenEffect.STROBE  -> androidx.compose.ui.graphics.Color.White
            ScreenEffect.CANDELA -> androidx.compose.ui.graphics.Color(0xFFFF9500L)
            ScreenEffect.RAINBOW -> androidx.compose.ui.graphics.Color(0xFF34C759L)
            null                 -> null
        }
    }
    // Apply first frame synchronously (before job starts) — eliminates perceptible transition frame
    LaunchedEffect(effect) {
        firstFrameColor?.let { onColorChange(it) }
    }
    // Run effect loop
    LaunchedEffect(effect) {
        when (effect) {
            ScreenEffect.CANDELA -> {
                var brightness = 0.65f; var dir = 1f
                while (true) {
                    val delta = (0.02f + kotlin.math.abs(kotlin.random.Random.nextFloat()) * 0.04f) * dir
                    brightness = (brightness + delta).coerceIn(0.50f, 0.90f)
                    if (brightness >= 0.90f || brightness <= 0.50f) dir *= -1f
                    onColorChange(androidx.compose.ui.graphics.Color(
                        red = 1f, green = 0.55f + brightness * 0.2f, blue = brightness * 0.3f
                    ))
                    delay((80 + (kotlin.random.Random.nextFloat() * 120).toLong()))
                }
            }
            ScreenEffect.POLICE -> {
                var red = true
                while (true) {
                    onColorChange(if (red) androidx.compose.ui.graphics.Color(0xFFFF3B30L)
                                  else     androidx.compose.ui.graphics.Color(0xFF007AFFL))
                    red = !red
                    delay(280L)
                }
            }
            ScreenEffect.RAINBOW -> {
                var hue = 0f
                while (true) {
                    hue = (hue + 1.5f) % 360f
                    val hsv = floatArrayOf(hue, 1f, 1f)
                    val argb = android.graphics.Color.HSVToColor(hsv)
                    onColorChange(androidx.compose.ui.graphics.Color(argb))
                    delay(30L)
                }
            }
            ScreenEffect.STROBE -> {
                var on = true
                while (true) {
                    onColorChange(if (on) androidx.compose.ui.graphics.Color.White
                                  else    androidx.compose.ui.graphics.Color.Black)
                    on = !on
                    delay(90L)
                }
            }
            null -> Unit
        }
    }
}

// ── Screen mode: unified tabbed control panel ─────────────────────────────────
@Composable
private fun ScreenControlPanel(
    screenColor: ScreenColor,
    screenEffect: ScreenEffect?,
    screenTab: Int,
    screenHue: Float,
    screenTemp: Float,
    brightness: Float,
    uiAlpha: Float,
    onColorSelect: (ScreenColor) -> Unit,
    onEffectSelect: (ScreenEffect?) -> Unit,
    onTabSelect: (Int) -> Unit,
    onHueChange: (Float) -> Unit,
    onTempChange: (Float) -> Unit,
    onBrightness: (Float) -> Unit,
    onInteraction: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .graphicsLayer { alpha = uiAlpha }
            .background(
                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.10f),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Brightness row
        var bVal by remember(brightness) { mutableFloatStateOf(brightness) }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("BRIGHTNESS", fontSize = 9.sp, fontWeight = FontWeight.W500,
                letterSpacing = 0.1.sp, color = androidx.compose.ui.graphics.Color.Black.copy(0.35f),
                modifier = Modifier.width(80.dp))
            Slider(value = bVal,
                onValueChange = { bVal = it; onBrightness(bVal); onInteraction() },
                onValueChangeFinished = {},
                valueRange = 0.05f..1.0f, steps = 18,
                colors = SliderDefaults.colors(
                    thumbColor = androidx.compose.ui.graphics.Color.Black.copy(0.4f),
                    activeTrackColor = androidx.compose.ui.graphics.Color.Black.copy(0.3f),
                    inactiveTrackColor = androidx.compose.ui.graphics.Color.Black.copy(0.12f),
                ),
                modifier = Modifier.weight(1f).height(28.dp),
            )
            Text("${(bVal*100).toInt()}%", fontSize = 9.sp,
                color = androidx.compose.ui.graphics.Color.Black.copy(0.4f),
                modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
        Spacer(Modifier.height(10.dp))
        // Tabs
        val tabLabels = listOf("Solid", "Hue", "Temp", "FX")
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
            tabLabels.forEachIndexed { i, label ->
                val sel = screenTab == i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (sel) androidx.compose.ui.graphics.Color.Black.copy(0.18f)
                            else     androidx.compose.ui.graphics.Color.Black.copy(0.07f)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelect(i); onInteraction() }
                        )
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, fontSize = 11.sp,
                        fontWeight = if (sel) FontWeight.W500 else FontWeight.W400,
                        color = androidx.compose.ui.graphics.Color.Black.copy(if (sel) 0.7f else 0.4f))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        // Tab content
        when (screenTab) {
            0 -> { // Solid
                val colorRows = ScreenColor.entries.chunked(6)
                colorRows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically) {
                        row.forEach { sc ->
                            val sel = sc == screenColor && screenEffect == null
                            val isLight = sc == ScreenColor.WHITE || sc == ScreenColor.WARM_WHITE ||
                                          sc == ScreenColor.YELLOW || sc == ScreenColor.LIME
                            Box(modifier = Modifier
                                .size(if (sel) 36.dp else 28.dp)
                                .clip(CircleShape)
                                .background(sc.color)
                                .then(if (!sel && isLight) Modifier.border(1.dp, LumiColor.Navy600.copy(0.3f), CircleShape) else Modifier)
                                .then(if (sel) Modifier.border(2.5.dp, if (isLight) LumiColor.Navy700 else androidx.compose.ui.graphics.Color.White, CircleShape) else Modifier)
                                .clickable(interactionSource = remember { MutableInteractionSource() },
                                    indication = null, onClick = { onColorSelect(sc); onInteraction() })
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            1 -> { // Hue
                Text("HUE — ${screenHue.toInt()}°", fontSize = 9.sp,
                    color = androidx.compose.ui.graphics.Color.Black.copy(0.4f),
                    fontWeight = FontWeight.W500, letterSpacing = 0.1.sp)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(colors = (0..12).map { i ->
                            val hsv = floatArrayOf(i * 30f, 1f, 1f)
                            androidx.compose.ui.graphics.Color(android.graphics.Color.HSVToColor(hsv))
                        })
                    )
                ) {
                    val thumbPct = screenHue / 360f
                    Box(modifier = Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .offset(x = (thumbPct * 1f).coerceIn(0f,1f).let {
                            androidx.compose.ui.unit.Dp(it)
                        })
                        .background(androidx.compose.ui.graphics.Color.White)
                    )
                    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            onHueChange((down.position.x / size.width.toFloat() * 360f).coerceIn(0f, 360f))
                            onInteraction()
                            var stillDown = true
                            while (stillDown) {
                                val event = awaitPointerEvent()
                                stillDown = event.changes.any { it.pressed }
                                event.changes.forEach { change ->
                                    if (change.pressed) {
                                        onHueChange((change.position.x / size.width.toFloat() * 360f).coerceIn(0f, 360f))
                                        change.consume()
                                    }
                                }
                            }
                        }
                    })
                }
                Spacer(Modifier.height(4.dp))
                val hueNames = listOf("red","orange","yellow","chartreuse","green","spring",
                    "cyan","azure","blue","violet","magenta","rose")
                Text(hueNames[(screenHue / 30f).toInt().coerceIn(0, 11)],
                    fontSize = 9.sp, color = androidx.compose.ui.graphics.Color.Black.copy(0.35f))
            }
            2 -> { // Temp
                val k = (2700 + screenTemp * (6500 - 2700)).toInt()
                val tempLabel = when {
                    k < 3000 -> "candlelight"
                    k < 3500 -> "incandescent"
                    k < 4500 -> "neutral white"
                    k < 5500 -> "cool daylight"
                    else      -> "cold daylight"
                }
                Text("TEMPERATURE — ${k}K  $tempLabel", fontSize = 9.sp,
                    color = androidx.compose.ui.graphics.Color.Black.copy(0.4f),
                    fontWeight = FontWeight.W500, letterSpacing = 0.05.sp)
                Spacer(Modifier.height(4.dp))
                Slider(value = screenTemp,
                    onValueChange = { onTempChange(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = androidx.compose.ui.graphics.Color.Black.copy(0.5f),
                        activeTrackColor = androidx.compose.ui.graphics.Color(0xFFFF8C00L),
                        inactiveTrackColor = androidx.compose.ui.graphics.Color(0xFFB8D4FFL),
                    ),
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                )
            }
            3 -> { // FX
                val effects = listOf(
                    ScreenEffect.CANDELA to "Candela",
                    ScreenEffect.POLICE  to "Police",
                    ScreenEffect.RAINBOW to "Rainbow",
                )
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    effects.forEach { (fx, label) ->
                        val active = screenEffect == fx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) androidx.compose.ui.graphics.Color.Black.copy(0.22f)
                                    else        androidx.compose.ui.graphics.Color.Black.copy(0.07f)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onEffectSelect(fx); onInteraction() }
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.W500 else FontWeight.W400,
                                color = androidx.compose.ui.graphics.Color.Black.copy(if (active) 0.75f else 0.4f))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (screenEffect == ScreenEffect.CANDELA) {
                    AnimatedCandle()
                } else {
                    Text(
                        when (screenEffect) {
                            ScreenEffect.POLICE  -> "red & blue alternating"
                            ScreenEffect.RAINBOW -> "full spectrum cycle"
                            else                 -> "tap to activate"
                        },
                        fontSize = 9.sp, color = androidx.compose.ui.graphics.Color.Black.copy(0.35f),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}


@Composable
private fun AnimatedCandle(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "candle")

    // Flame sway left-right
    val swayX by inf.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = LinearEasing), RepeatMode.Reverse
        ), label = "swayX"
    )
    // Flame height pulse
    val flameH by inf.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(600, easing = LinearEasing), RepeatMode.Reverse
        ), label = "flameH"
    )
    // Glow pulse
    val glowA by inf.animateFloat(
        initialValue = 0.12f, targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            tween(700, easing = LinearEasing), RepeatMode.Reverse
        ), label = "glowA"
    )

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    Canvas(
        modifier = Modifier
            .width(52.dp)
            .height(72.dp)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Glow halo
        drawCircle(
            color = androidx.compose.ui.graphics.Color(0xFFFF9500L).copy(alpha = glowA),
            radius = 36f * flameH,
            center = Offset(cx + swayX * 0.3f, cy - 8f),
        )

        // Candle body
        val bodyW = 18f; val bodyH = 30f
        val bodyTop = cy + 4f
        drawRoundRect(
            color = androidx.compose.ui.graphics.Color(0xFFF5E6C8L),
            topLeft = Offset(cx - bodyW / 2f, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(4f),
        )
        // Wax drip lines
        repeat(3) { i ->
            drawLine(
                color = androidx.compose.ui.graphics.Color(0xFFE8D5B0L),
                start = Offset(cx - bodyW / 2f + 4f + i * 5f, bodyTop + 4f),
                end   = Offset(cx - bodyW / 2f + 4f + i * 5f, bodyTop + 8f + i * 3f),
                strokeWidth = 2f,
            )
        }
        // Wick
        drawLine(
            color = androidx.compose.ui.graphics.Color(0xFF4A3728L),
            start = Offset(cx + swayX * 0.2f, bodyTop - 6f),
            end   = Offset(cx, bodyTop),
            strokeWidth = 2f,
        )

        // Flame — outer (orange)
        val flamePath = Path().apply {
            val fx = cx + swayX; val ftop = cy - 22f * flameH
            moveTo(fx, ftop)
            cubicTo(fx + 10f, ftop + 8f, fx + 12f, ftop + 20f, cx, bodyTop - 1f)
            cubicTo(cx - 12f, ftop + 20f, fx - 10f + swayX, ftop + 8f, fx, ftop)
            close()
        }
        drawPath(flamePath, androidx.compose.ui.graphics.Color(0xFFFF7700L).copy(alpha = 0.9f))

        // Flame — inner (yellow core)
        val innerPath = Path().apply {
            val fx = cx + swayX * 0.5f; val ftop = cy - 12f * flameH
            moveTo(fx, ftop)
            cubicTo(fx + 5f, ftop + 5f, fx + 6f, ftop + 12f, cx, bodyTop - 1f)
            cubicTo(cx - 6f, ftop + 12f, fx - 5f, ftop + 5f, fx, ftop)
            close()
        }
        drawPath(innerPath, androidx.compose.ui.graphics.Color(0xFFFFDD00L).copy(alpha = 0.85f))
    }
    } // Column
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
    morseSpeed: Float = 1.0f,
    strobePattern: StrobePattern = StrobePattern.SINGLE,
    onStrobePattern: (StrobePattern) -> Unit = {},
    smartSpeed: Float = 1.0f,
    sleepMinutes: Int = 3,
    micSensitivity: Float = 1.0f,
    onTorchIntensity: (Float) -> Unit = {},
    onScreenBrightness: (Float) -> Unit = {},
    onMorseSpeed: (Float) -> Unit = {},
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
            is FlashMode.Strobe            -> stringResource(R.string.config_strobe_title)
            is FlashMode.Disco             -> stringResource(R.string.config_disco_title)
            is FlashMode.MorseCustom       -> stringResource(R.string.config_morse_title)
            is FlashMode.SmartBrightness   -> stringResource(R.string.config_smart_title)
            is FlashMode.SleepTimer        -> stringResource(R.string.config_sleep_title)
            is FlashMode.Music             -> stringResource(R.string.config_music_title)
            is FlashMode.Voice             -> stringResource(R.string.config_voice_title)
            is FlashMode.AmbientSmart      -> stringResource(R.string.config_ambient_title)
            is FlashMode.CustomRhythm      -> stringResource(R.string.config_custom_title)
            else -> return  // ReadingMode, Walk — no config
        }
        Text(
            title,
            fontSize   = 16.sp,
            fontWeight = FontWeight.W600,
            color      = LumiColor.White,
        )

        // ── Intensity slider for torch modes ─────────────────────────────────
        val hasTorchIntensity = mode is FlashMode.Strobe ||
                                mode is FlashMode.Disco  ||
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
                Spacer(Modifier.height(12.dp))
                Text("BURST PATTERN", fontSize = 10.sp, color = LumiColor.Gray600,
                    fontWeight = FontWeight.W500, letterSpacing = 0.1.sp)
                val patternOptions = listOf(
                    "Single" to StrobePattern.SINGLE,
                    "Double" to StrobePattern.DOUBLE,
                    "Triple" to StrobePattern.TRIPLE,
                )
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    patternOptions.forEach { (label, p) ->
                        val sel = strobePattern == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) LumiColor.Amber400 else LumiColor.Navy700)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    onStrobePattern(p)
                                    // re-apply immediately if flash is on
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    val dots = when (p) {
                                        StrobePattern.SINGLE -> 1
                                        StrobePattern.DOUBLE -> 2
                                        StrobePattern.TRIPLE -> 3
                                    }
                                    repeat(dots) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (sel) LumiColor.Navy950 else LumiColor.Amber400)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.W600,
                                    color = if (sel) LumiColor.Navy950 else LumiColor.White)
                            }
                        }
                    }
                }
                Text("Single = classic · Double/Triple = burst per cycle",
                    fontSize = 10.sp, color = LumiColor.Gray600)
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
                Spacer(Modifier.height(12.dp))
                // ── Tap-to-tempo ─────────────────────────────────────────────
                var tapTimes by remember { mutableStateOf(listOf<Long>()) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LumiColor.Navy700)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            val now = System.currentTimeMillis()
                            val recent = tapTimes.filter { now - it < 3000L }
                            val newTaps = (recent + now).takeLast(8)
                            tapTimes = newTaps
                            if (newTaps.size >= 2) {
                                val intervals = newTaps.zipWithNext { a, b -> b - a }
                                val avgMs = intervals.average()
                                val detectedBpm = (60_000.0 / avgMs)
                                    .coerceIn(60.0, 200.0).toFloat()
                                bpm = detectedBpm
                                onDiscoBpm(detectedBpm.toInt().toFloat())
                            }
                        }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (tapTimes.size < 2) "TAP BEAT  👆" else "TAP BEAT  ${bpm.toInt()} BPM",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                        color = LumiColor.Amber400,
                    )
                }
                Text("Tap 3+ times to set tempo by feel",
                    fontSize = 10.sp, color = LumiColor.Gray600)
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
                Spacer(Modifier.height(4.dp))
                Text("TRANSMISSION SPEED", fontSize = 10.sp, color = LumiColor.Gray600,
                    fontWeight = FontWeight.W500, letterSpacing = 0.1.sp)
                val speedOptions = listOf("½×" to 0.5f, "1×" to 1.0f, "2×" to 2.0f, "4×" to 4.0f)
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    speedOptions.forEach { (label, v) ->
                        val sel = kotlin.math.abs(morseSpeed - v) < 0.1f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) LumiColor.Amber400 else LumiColor.Navy700)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onMorseSpeed(v) }
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.W600,
                                color = if (sel) LumiColor.Navy950 else LumiColor.White)
                        }
                    }
                }
                Text("½× = slow · 1× = standard ITU · 4× = fast burst",
                    fontSize = 10.sp, color = LumiColor.Gray600)
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

            else -> {}
        }
    }
}

package com.lumiai.flashlight.feature.flash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.core.domain.model.ProStatus
import com.lumiai.flashlight.ui.components.AdBanner
import com.lumiai.flashlight.ui.components.FlashButton
import com.lumiai.flashlight.ui.theme.LumiColor
import com.lumiai.flashlight.R
import androidx.compose.ui.res.stringResource

/**
 * Full-screen mode configuration.
 *
 * Layout:
 *   TopBar (back arrow + mode name)
 *   FlashButton — the original animated button, ON/OFF for this mode
 *   Status label (same as main screen)
 *   Mode-specific config content (scrollable, same ModeConfigSheet body)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeConfigScreen(
    modeId: String,
    viewModel: FlashViewModel,
    onBack: () -> Unit,
) {
    val uiState        by viewModel.uiState.collectAsState()
    val morseText      by viewModel.morseText.collectAsState()
    val torchIntensity by viewModel.torchIntensity.collectAsState()
    val morseSpeed     by viewModel.morseSpeed.collectAsState()
    val strobePattern  by viewModel.strobePattern.collectAsState()
    val smartSpeed     by viewModel.smartSpeed.collectAsState()
    val sleepMinutes   by viewModel.sleepMinutes.collectAsState()
    val micSensitivity by viewModel.micSensitivity.collectAsState()
    // screenColor is handled by ScreenControlPanel in FlashScreen

    val isOn = uiState.isFlashOn

    // The mode this config screen is for
    val configMode: FlashMode = remember(modeId) {
        FlashMode.all().firstOrNull { it.id == modeId } ?: FlashMode.Steady
    }

    val modeName = remember(modeId) {
        when (modeId) {
            "steady"           -> "Steady"
            "screen"           -> "Screen"
            "morse_custom"     -> "Morse"
            "strobe"           -> "Strobe"
            "sos"              -> "SOS"
            "disco"            -> "Disco"
            "smart_brightness" -> "Smart"
            "reading_mode"     -> "Reading"
            "ambient_smart"    -> "Ambient"
            "custom_rhythm"    -> "Custom"
            "sleep_timer"      -> "Sleep"
            "music"            -> "Music"
            "walk"             -> "Walk"
            "voice"            -> "Voice"
            else               -> "Mode"
        }
    }

    val isPro = uiState.proStatus == ProStatus.Pro

    BackHandler { onBack() }

    Scaffold(
        containerColor = LumiColor.Navy950,
        bottomBar = {
            // Sticky AdBanner — same as FlashScreen, doesn't disappear on scroll
            if (!isPro) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LumiColor.Navy950)
                        .navigationBarsPadding(),
                ) { AdBanner() }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modeName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W600,
                        color = LumiColor.White,
                    )
                },
                navigationIcon = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(LumiColor.Navy800)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBack,
                            ),
                    ) {
                        Icon(
                            configBackIcon(),
                            contentDescription = "Back",
                            tint = LumiColor.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LumiColor.Navy950),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Flash Button (the original, with animated ring) ───────────────
            FlashButton(
                isOn  = isOn,
                onClick = {
                    // If selecting a different mode, activate it first
                    if (uiState.currentMode.id != configMode.id) {
                        viewModel.activateMode(configMode)
                    } else {
                        viewModel.toggleFlash()
                    }
                },
                size = 160.dp,
            )

            Spacer(Modifier.height(8.dp))

            // Status label
            Text(
                text = when {
                    !isOn -> stringResource(R.string.config_tap_on)
                    uiState.currentMode.id != modeId -> stringResource(R.string.config_switch_mode, modeName)
                    else -> when (configMode) {
                        is FlashMode.Strobe -> "STROBE · ${uiState.strobeHz.toInt()} HZ"
                        is FlashMode.Disco  -> "DISCO · ${uiState.discoBpm.toInt()} BPM"
                        is FlashMode.Sos    -> "SOS · · · — — —"
                        else                -> stringResource(R.string.config_on)
                    }
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.W500,
                letterSpacing = 0.14.sp,
                color = if (isOn) LumiColor.Amber400.copy(.7f) else LumiColor.Gray600,
            )

            Spacer(Modifier.height(24.dp))

            // ── Mode config content ───────────────────────────────────────────
            // Reuses the existing ModeConfigSheet body — same composable, no duplication
            ModeConfigSheet(
                mode               = configMode,
                uiState            = uiState,
                morseText          = morseText,
                torchIntensity     = torchIntensity,
                morseSpeed         = morseSpeed,
                strobePattern      = strobePattern,
                smartSpeed         = smartSpeed,
                sleepMinutes       = sleepMinutes,
                micSensitivity     = micSensitivity,
                onStrobeHz         = { viewModel.updateStrobeHz(it) },
                onDiscoBpm         = { viewModel.updateDiscoBpm(it) },
                onMorseText        = { viewModel.updateMorseText(it) },
                onScreenBrightness = { viewModel.setScreenBrightness(it) },
                onTorchIntensity   = { viewModel.setTorchIntensity(it) },
                onMorseSpeed       = { viewModel.setMorseSpeed(it) },
                onStrobePattern    = { viewModel.setStrobePattern(it) },
                onSmartSpeed       = { viewModel.setSmartSpeed(it) },
                onSleepMinutes     = { viewModel.setSleepMinutes(it) },
                onMicSensitivity   = { viewModel.setMicSensitivity(it) },
                onReactivate       = {
                    if (uiState.isFlashOn) viewModel.activateMode(FlashMode.AmbientSmart)
                },
                onDismiss = {},  // no-op — screen has its own back nav
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun configBackIcon(): ImageVector = ImageVector.Builder(
    name = "Back", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f,
).apply {
    path(
        stroke = SolidColor(androidx.compose.ui.graphics.Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(19f, 12f); horizontalLineTo(5f)
        moveTo(12f, 19f); lineTo(5f, 12f); lineTo(12f, 5f)
    }
}.build()

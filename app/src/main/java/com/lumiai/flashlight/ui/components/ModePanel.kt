package com.lumiai.flashlight.ui.components

import com.lumiai.flashlight.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.ui.theme.LumiColor

data class FlashModeItem(
    val mode: FlashMode,
    val name: String,
    val desc: String,
    val symbol: String,
    val accentColor: Color = LumiColor.Amber400,
    val sensorTag: String? = null,
    val info: String = "",
)

val flashModeItems = listOf(
    FlashModeItem(FlashMode.Steady, "Steady", "Full brightness, continuous", "⚡",
        info = "Continuous full-brightness flash. Best for general lighting."),
    FlashModeItem(FlashMode.Screen, "Screen", "White screen, no flash", "▢",
        info = "Uses screen as white light source. Adjustable color and brightness."),
    FlashModeItem(FlashMode.MorseCustom(), "Morse", "Text to flash", "—·",
        info = "Type any text — flash transmits it in Morse code on loop."),
    FlashModeItem(FlashMode.Strobe(), "Strobe", "1–20 Hz pulse", "⊙",
        info = "Rapid 1–20 Hz pulse. Use with caution near photosensitive people."),
    FlashModeItem(FlashMode.Sos, "SOS", "Morse · · · — — —", "◬",
        info = "SOS signal: · · · — — — · · · repeating. International distress."),
    FlashModeItem(FlashMode.Disco(), "Disco", "Beat sync · 60–200 BPM", "◇",
        info = "Beat-synced flash 60–200 BPM. Great for parties."),
)

val aiModeItems = listOf(
    FlashModeItem(FlashMode.SmartBrightness, "Smart", "Adapts to ambient light", "◎",
        accentColor = Color(0xFFFFD84A), sensorTag = "Light sensor",
        info = "Reads light sensor and adjusts pulse speed automatically."),
    FlashModeItem(FlashMode.ReadingMode, "Read", "Warm pulse, auto-dims", "☽",
        accentColor = Color(0xFFFF9A6C), sensorTag = "Timer curve",
        info = "Steady warm light that dims over 20 min. Ideal for reading."),
    FlashModeItem(FlashMode.AmbientSmart, "Ambient", "Reads scene, picks pattern", "⬨",
        accentColor = Color(0xFF34D399), sensorTag = "Lux + ML Kit",
        info = "Detects scene brightness and picks the best light pattern."),
    FlashModeItem(FlashMode.CustomRhythm(), "Custom", "Pattern adapts to hour", "⬡",
        accentColor = Color(0xFFA78BFA), sensorTag = "Clock + gen",
        info = "Rhythm pattern changes automatically by time of day."),
    FlashModeItem(FlashMode.SleepTimer, "Sleep", "Fades out over 3 minutes", "◌",
        accentColor = Color(0xFF4ADE80), sensorTag = "Duty curve",
        info = "Gradually fades out over 3 minutes, then turns off."),
    FlashModeItem(FlashMode.Music, "Music", "Syncs flash to live beats", "♩",
        accentColor = Color(0xFF60A5FA), sensorTag = "Microphone",
        info = "Syncs to audio beats via mic. Works with any music."),
    FlashModeItem(FlashMode.Walk, "Walk", "Pulse on every step", "◉",
        accentColor = Color(0xFF818CF8), sensorTag = "Step detector",
        info = "Pulses on each step from step detector. Hands-free."),
    FlashModeItem(FlashMode.Voice, "Voice", "Reacts to voice and sound", "◍",
        accentColor = Color(0xFFF472B6), sensorTag = "Microphone",
        info = "Reacts to voice and sound spikes. Great for signaling."),
)




@Composable
fun ModePanel(
    currentMode: FlashMode,
    strobeHz: Float,
    discoBpm: Float,
    onModeSelect: (FlashMode) -> Unit,
    onModeConfig: (FlashMode) -> Unit = {},   // opens config sheet
    isConfigSheetOpen: Boolean = false,
    torchIntensity: Float = 1.0f,
    onTorchIntensityChange: (Float) -> Unit = {},
    screenBrightness: Float = 1.0f,
    onScreenBrightnessChange: (Float) -> Unit = {},
    onStrobeHzChange: (Float) -> Unit,
    onDiscoBpmChange: (Float) -> Unit,
    isPro: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(if (currentMode.isPro) 1 else 0) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LumiColor.Navy800),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            listOf(stringResource(R.string.tab_flash), stringResource(R.string.tab_ai_modes)).forEachIndexed { idx, label ->
                val active = selectedTab == idx
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) LumiColor.Navy700 else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { selectedTab = idx },
                        )
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.W600 else FontWeight.W400,
                        color = when {
                            active && idx == 1 -> LumiColor.Amber400
                            active -> LumiColor.White
                            else -> LumiColor.Gray600
                        },
                        letterSpacing = 0.04.sp,
                    )
                }
            }
        }

        val showSlider = selectedTab == 0 && !isConfigSheetOpen &&
            (currentMode is FlashMode.Steady || currentMode is FlashMode.Screen ||
             currentMode is FlashMode.Strobe || currentMode is FlashMode.Disco)
        if (showSlider) {
            when (currentMode) {
                is FlashMode.Steady -> key("steady_slider") {
                    ContextSlider(
                        label  = "FLASH INTENSITY",
                        value  = torchIntensity,
                        range  = 0.1f..1.0f,
                        steps  = 17,
                        format = { "${(it * 100).toInt()}%" },
                        onSettle = { onTorchIntensityChange(it) },
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                }
                is FlashMode.Screen -> key("screen_slider") {
                    ContextSlider(
                        label  = "SCREEN BRIGHTNESS",
                        value  = screenBrightness,
                        range  = 0.05f..1.0f,
                        steps  = 18,
                        format = { "${(it * 100).toInt()}%" },
                        onSettle = { onScreenBrightnessChange(it) },
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                }
                is FlashMode.Strobe -> key("strobe_slider") {
                    ContextSlider(
                        label = stringResource(R.string.config_frequency_label),
                        value = strobeHz,
                        range = 1f..20f,
                        steps = 18,
                        format = { "${it.toInt()} Hz" },
                        onSettle = { onStrobeHzChange(it) },
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                }
                is FlashMode.Disco -> key("disco_slider") {
                    ContextSlider(
                        label = stringResource(R.string.config_tempo_label),
                        value = discoBpm,
                        range = 60f..200f,
                        steps = 27,
                        format = { "${it.toInt()} BPM" },
                        onSettle = { onDiscoBpmChange(it) },
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                }
                else -> {}
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState)
                    slideInHorizontally { it / 3 } + fadeIn(tween(160)) togetherWith
                    slideOutHorizontally { -it / 3 } + fadeOut(tween(120))
                else
                    slideInHorizontally { -it / 3 } + fadeIn(tween(160)) togetherWith
                    slideOutHorizontally { it / 3 } + fadeOut(tween(120))
            },
            label = "tab_content",
        ) { tab ->
            if (tab == 0) {
                FlashModeGrid(
                    items = listOf(
                        FlashModeItem(FlashMode.Steady, stringResource(R.string.mode_steady), stringResource(R.string.mode_steady_desc), "⚡", info = stringResource(R.string.mode_steady_info)),
                        FlashModeItem(FlashMode.Screen, stringResource(R.string.mode_screen), stringResource(R.string.mode_screen_desc), "▢", info = stringResource(R.string.mode_screen_info)),
                        FlashModeItem(FlashMode.MorseCustom(), stringResource(R.string.mode_morse), stringResource(R.string.mode_morse_desc), "—·", info = stringResource(R.string.mode_morse_info)),
                        FlashModeItem(FlashMode.Strobe(), stringResource(R.string.mode_strobe), stringResource(R.string.mode_strobe_desc), "⊙", info = stringResource(R.string.mode_strobe_info)),
                        FlashModeItem(FlashMode.Sos, stringResource(R.string.mode_sos), stringResource(R.string.mode_sos_desc), "◬", info = stringResource(R.string.mode_sos_info)),
                        FlashModeItem(FlashMode.Disco(), stringResource(R.string.mode_disco), stringResource(R.string.mode_disco_desc), "◇", info = stringResource(R.string.mode_disco_info)),
                    ),
                    currentMode = currentMode,
                    onSelect = onModeSelect,
                    onConfig = onModeConfig,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                AiModeGrid(
                    items = listOf(
                        FlashModeItem(FlashMode.SmartBrightness, stringResource(R.string.mode_smart), stringResource(R.string.mode_smart_desc), "◎", accentColor = Color(0xFFFFD84A), sensorTag = stringResource(R.string.sensor_light), info = stringResource(R.string.mode_smart_info)),
                        FlashModeItem(FlashMode.ReadingMode, stringResource(R.string.mode_read), stringResource(R.string.mode_read_desc), "☽", accentColor = Color(0xFFFF9A6C), sensorTag = stringResource(R.string.sensor_timer), info = stringResource(R.string.mode_read_info)),
                        FlashModeItem(FlashMode.AmbientSmart, stringResource(R.string.mode_ambient), stringResource(R.string.mode_ambient_desc), "⬨", accentColor = Color(0xFF34D399), sensorTag = stringResource(R.string.sensor_lux_ml), info = stringResource(R.string.mode_ambient_info)),
                        FlashModeItem(FlashMode.CustomRhythm(), stringResource(R.string.mode_custom), stringResource(R.string.mode_custom_desc), "⬡", accentColor = Color(0xFFA78BFA), sensorTag = stringResource(R.string.sensor_clock), info = stringResource(R.string.mode_custom_info)),
                        FlashModeItem(FlashMode.SleepTimer, stringResource(R.string.mode_sleep), stringResource(R.string.mode_sleep_desc), "◌", accentColor = Color(0xFF4ADE80), sensorTag = stringResource(R.string.sensor_duty), info = stringResource(R.string.mode_sleep_info)),
                        FlashModeItem(FlashMode.Music, stringResource(R.string.mode_music), stringResource(R.string.mode_music_desc), "♩", accentColor = Color(0xFF60A5FA), sensorTag = stringResource(R.string.sensor_mic), info = stringResource(R.string.mode_music_info)),
                        FlashModeItem(FlashMode.Walk, stringResource(R.string.mode_walk), stringResource(R.string.mode_walk_desc), "◉", accentColor = Color(0xFF818CF8), sensorTag = stringResource(R.string.sensor_step), info = stringResource(R.string.mode_walk_info)),
                        FlashModeItem(FlashMode.Voice, stringResource(R.string.mode_voice), stringResource(R.string.mode_voice_desc), "◍", accentColor = Color(0xFFF472B6), sensorTag = stringResource(R.string.sensor_mic), info = stringResource(R.string.mode_voice_info)),
                    ),
                    currentMode = currentMode,
                    onSelect = onModeSelect,
                    onConfig = onModeConfig,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun FlashModeGrid(
    items: List<FlashModeItem>,
    currentMode: FlashMode,
    onSelect: (FlashMode) -> Unit,
    onConfig: (FlashMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        items.subList(0, 2),
        items.subList(2, 4),
        items.subList(4, 6), // Corregido: Ajustado para incluir todos los elementos de flashModeItems
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            if (row.size == 1) {
                FlashModeCard(
                    item = row[0],
                    isSelected = row[0].mode.id == currentMode.id,
                    onClick = { onSelect(row[0].mode) },
                    onConfig = { onConfig(row[0].mode) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { item ->
                        FlashModeCard(
                            item = item,
                            isSelected = item.mode.id == currentMode.id,
                            onClick = { onSelect(item.mode) },
                            onConfig = { onConfig(item.mode) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiModeGrid(
    items: List<FlashModeItem>,
    currentMode: FlashMode,
    onSelect: (FlashMode) -> Unit,
    onConfig: (FlashMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { item ->
                    AiModeCard(
                        item = item,
                        isSelected = item.mode.id == currentMode.id,
                        onClick = { onSelect(item.mode) },
                        onConfig = { onConfig(item.mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FlashModeCard(
    item: FlashModeItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onConfig: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var showInfo by remember { mutableStateOf(false) }

    if (showInfo && item.info.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(item.name, color = Color.White) },
            text = { Text(item.info, color = LumiColor.Gray400, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } },
            containerColor = LumiColor.Navy800,
        )
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) LumiColor.Navy700 else LumiColor.Navy800)
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) LumiColor.Amber400.copy(.7f) else LumiColor.Navy700,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = LumiColor.Amber400.copy(.1f)),
                onClick = onClick,
            )
            .padding(14.dp),
    ) {
        Column {
            Text(
                text = item.symbol,
                fontSize = 20.sp,
                color = if (isSelected) LumiColor.Amber400 else LumiColor.Gray500,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                color = if (isSelected) Color.White else LumiColor.Gray400,
                letterSpacing = 0.02.sp,
            )
            Text(
                text = item.desc,
                fontSize = 10.sp,
                color = LumiColor.Gray600,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ⚙ config button for configurable modes
            val configModes = setOf("strobe","disco","morse_custom","smart_brightness","sleep_timer","music","voice")
            if (item.mode.id in configModes) {
                Text(
                    "⚙",
                    fontSize = 11.sp,
                    color = LumiColor.Amber400.copy(.6f),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onConfig,
                    ),
                )
            }
            if (item.info.isNotBlank()) {
                Text(
                    "ⓘ",
                    fontSize = 11.sp,
                    color = LumiColor.Gray600,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showInfo = true },
                    ),
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(LumiColor.Amber400),
                )
            }
        }
    }
}

@Composable
private fun AiModeCard(
    item: FlashModeItem,
    isSelected: Boolean,
    isPro: Boolean = false,
    onClick: () -> Unit,
    onConfig: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val accent = item.accentColor
    var showInfo by remember { mutableStateOf(false) }

    if (showInfo && item.info.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(item.name, color = Color.White) },
            text = { Text(item.info, color = LumiColor.Gray400, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } },
            containerColor = LumiColor.Navy800,
        )
    }

    val isLocked = item.mode.isPro && !isPro

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) LumiColor.Navy700 else LumiColor.Navy800)
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) accent.copy(.8f) else LumiColor.Navy700,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = accent.copy(.1f)),
                onClick = onClick,
            )
            .padding(14.dp),
    ) {
        // PRO badge — top right, visible to Free users
        if (isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(LumiColor.Amber400.copy(alpha = 0.15f))
                    .border(0.5.dp, LumiColor.Amber400.copy(0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(
                    "PRO",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = 0.08.sp,
                    color = LumiColor.Amber400,
                )
            }
        }
        Column {
            Text(
                text = item.symbol,
                fontSize = 20.sp,
                color = if (isSelected) accent else accent.copy(.6f),
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                color = if (isSelected) Color.White else LumiColor.Gray400,
                letterSpacing = 0.02.sp,
            )
            Text(
                text = item.desc,
                fontSize = 10.sp,
                color = LumiColor.Gray600,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            item.sensorTag?.let { tag ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(accent.copy(if (isSelected) 1f else .4f)),
                    )
                    Text(
                        text = tag,
                        fontSize = 9.sp,
                        color = accent.copy(if (isSelected) .7f else .4f),
                        letterSpacing = 0.04.sp,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val aiConfigModes = setOf("smart_brightness","sleep_timer","music","voice","ambient_smart","custom_rhythm")
            if (item.mode.id in aiConfigModes) {
                Text(
                    "⚙",
                    fontSize = 11.sp,
                    color = accent.copy(.6f),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onConfig,
                    ),
                )
            }
            if (item.info.isNotBlank()) {
                Text(
                    "ⓘ",
                    fontSize = 11.sp,
                    color = accent.copy(.5f),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showInfo = true },
                    ),
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
        }
    }
}

@Composable
private fun ContextSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    format: (Float) -> String,
    onSettle: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var local by remember { mutableFloatStateOf(value) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!dragging) local = value }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LumiColor.Navy800)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                letterSpacing = 0.14.sp,
                color = LumiColor.Gray600,
                fontWeight = FontWeight.W500,
            )
            Text(
                text = format(local),
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                color = LumiColor.Amber400,
            )
        }
        Slider(
            value = local,
            onValueChange = { dragging = true; local = it },
            onValueChangeFinished = { dragging = false; onSettle(local) },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = LumiColor.Amber400,
                activeTrackColor = LumiColor.Amber400,
                inactiveTrackColor = LumiColor.Navy600,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

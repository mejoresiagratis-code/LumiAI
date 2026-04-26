package com.lumiai.flashlight.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
    [span_48](start_span)val info: String = "",[span_48](end_span)
)

val flashModeItems = listOf(
    FlashModeItem(FlashMode.Steady, "Steady", "Full brightness, continuous", "⚡",
        info = "Continuous full-brightness flash. Best for general lighting."),
    FlashModeItem(FlashMode.Screen, "Screen", "White screen, no flash", "▢",
        info = "Uses screen as white light source. Adjustable color and brightness."),
    [span_49](start_span)FlashModeItem(FlashMode.MorseCustom(), "Morse", "Text to flash", "—·",[span_49](end_span)
        info = "Type any text — flash transmits it in Morse code on loop."),
    FlashModeItem(FlashMode.Strobe(), "Strobe", "1–20 Hz pulse", "⊙",
        info = "Rapid 1–20 Hz pulse. Use with caution near photosensitive people."),
    [span_50](start_span)FlashModeItem(FlashMode.Sos, "SOS", "Morse · · · — — —", "◬",[span_50](end_span)
        info = "SOS signal: · · · — — — · · · repeating. International distress."),
    FlashModeItem(FlashMode.Disco(), "Disco", "Beat sync · 60–200 BPM", "◇",
        [span_51](start_span)info = "Beat-synced flash 60–200 BPM. Great for parties."),[span_51](end_span)
)

val aiModeItems = listOf(
    FlashModeItem(FlashMode.SmartBrightness, "Smart", "Adapts to ambient light", "◎",
        accentColor = Color(0xFFFFD84A), sensorTag = "Light sensor",
        info = "Reads light sensor and adjusts pulse speed automatically."),
    FlashModeItem(FlashMode.ReadingMode, "Read", "Warm pulse, auto-dims", "☽",
        [span_52](start_span)accentColor = Color(0xFFFF9A6C), sensorTag = "Timer curve",[span_52](end_span)
        info = "Steady warm light that dims over 20 min. Ideal for reading."),
    FlashModeItem(FlashMode.AmbientSmart, "Ambient", "Reads scene, picks pattern", "⬨",
        accentColor = Color(0xFF34D399), sensorTag = "Lux + ML Kit",
        info = "Detects scene brightness and picks the best light pattern."),
    FlashModeItem(FlashMode.CustomRhythm(), "Custom", "Pattern adapts to hour", "⬡",
        [span_53](start_span)accentColor = Color(0xFFA78BFA), sensorTag = "Clock + gen",[span_53](end_span)
        info = "Rhythm pattern changes automatically by time of day."),
    FlashModeItem(FlashMode.SleepTimer, "Sleep", "Fades out over 3 minutes", "◌",
        accentColor = Color(0xFF4ADE80), sensorTag = "Duty curve",
        info = "Gradually fades out over 3 minutes, then turns off."),
    FlashModeItem(FlashMode.Music, "Music", "Syncs flash to live beats", "♩",
        [span_54](start_span)accentColor = Color(0xFF60A5FA), sensorTag = "Microphone",[span_54](end_span)
        [span_55](start_span)info = "Syncs to audio beats via mic. Works with any music."),[span_55](end_span)
    FlashModeItem(FlashMode.Walk, "Walk", "Pulse on every step", "◉",
        accentColor = Color(0xFF818CF8), sensorTag = "Step detector",
        info = "Pulses on each step from step detector. Hands-free."),
    FlashModeItem(FlashMode.Voice, "Voice", "Reacts to voice and sound", "◍",
        [span_56](start_span)accentColor = Color(0xFFF472B6), sensorTag = "Microphone",[span_56](end_span)
        info = "Reacts to voice and sound spikes. Great for signaling."),
)

@Composable
fun ModePanel(
    currentMode: FlashMode,
    strobeHz: Float,
    discoBpm: Float,
    onModeSelect: (FlashMode) -> Unit,
    onStrobeHzChange: (Float) -> Unit,
    onDiscoBpmChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(if (currentMode.isPro) 1 else 0) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                [span_57](start_span).padding(bottom = 12.dp)[span_57](end_span)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LumiColor.Navy800),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            listOf("Flash", "AI Modes").forEachIndexed { idx, label ->
                [span_58](start_span)val active = selectedTab == idx[span_58](end_span)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        [span_59](start_span).clip(RoundedCornerShape(10.dp))[span_59](end_span)
                        .background(if (active) LumiColor.Navy700 else Color.Transparent)
                        .clickable(
                            [span_60](start_span)interactionSource = remember { MutableInteractionSource() },[span_60](end_span)
                            indication = null,
                            onClick = { selectedTab = idx },
                        )
                        [span_61](start_span).padding(vertical = 8.dp),[span_61](end_span)
                ) {
                    Text(
                        text = label,
                        [span_62](start_span)fontSize = 12.sp,[span_62](end_span)
                        fontWeight = if (active) FontWeight.W600 else FontWeight.W400,
                        color = when {
                            active && idx == 1 -> LumiColor.Amber400
                            [span_63](start_span)active -> LumiColor.White[span_63](end_span)
                            else -> LumiColor.Gray600
                        [span_64](start_span)},[span_64](end_span)
                        letterSpacing = 0.04.sp,
                    )
                }
            }
        }

        [span_65](start_span)val showSlider = selectedTab == 0 && (currentMode is FlashMode.Strobe || currentMode is FlashMode.Disco)[span_65](end_span)
        if (showSlider) {
            when (currentMode) {
                is FlashMode.Strobe -> key("strobe_slider") {
                    ContextSlider(
                        label = "FREQUENCY",
                        [span_66](start_span)value = strobeHz,[span_66](end_span)
                        range = 1f..20f,
                        steps = 18,
                        [span_67](start_span)format = { "${it.toInt()} Hz" },[span_67](end_span)
                        onSettle = { onStrobeHzChange(it) },
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                [span_68](start_span)}
                is FlashMode.Disco -> key("disco_slider") {
                    ContextSlider(
                        label = "TEMPO",
                        value = discoBpm,[span_68](end_span)
                        range = 60f..200f,
                        steps = 27,
                        format = { "${it.toInt()} BPM" },
                        [span_69](start_span)onSettle = { onDiscoBpmChange(it) },[span_69](end_span)
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                }
                [span_70](start_span)else -> {}[span_70](end_span)
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState)
                    [span_71](start_span)slideInHorizontally { it / 3 } + fadeIn(tween(160)) togetherWith[span_71](end_span)
                    slideOutHorizontally { -it / 3 } + fadeOut(tween(120))
                else
                    slideInHorizontally { -it / 3 } + fadeIn(tween(160)) togetherWith
                    [span_72](start_span)slideOutHorizontally { it / 3 } + fadeOut(tween(120))[span_72](end_span)
            },
            label = "tab_content",
        ) { tab ->
            if (tab == 0) {
                FlashModeGrid(
                    [span_73](start_span)items = flashModeItems,[span_73](end_span)
                    currentMode = currentMode,
                    onSelect = onModeSelect,
                    modifier = Modifier.padding(horizontal = 16.dp),
                [span_74](start_span))
            } else {
                AiModeGrid(
                    items = aiModeItems,
                    currentMode = currentMode,
                    onSelect = onModeSelect,[span_74](end_span)
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun FlashModeGrid(
    items: List<FlashModeItem>,
    [span_75](start_span)currentMode: FlashMode,[span_75](end_span)
    onSelect: (FlashMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        items.subList(0, 2),
        items.subList(2, 4),
        items.subList(4, 6), // Corregido: Ajustado para incluir todos los elementos de flashModeItems
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            [span_76](start_span)if (row.size == 1) {[span_76](end_span)
                FlashModeCard(
                    item = row[0],
                    isSelected = row[0].mode.id == currentMode.id,
                    onClick = { onSelect(row[0].mode) },
                    [span_77](start_span)modifier = Modifier.fillMaxWidth(),[span_77](end_span)
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { item ->
                        [span_78](start_span)FlashModeCard([span_78](end_span)
                            item = item,
                            isSelected = item.mode.id == currentMode.id,
                            [span_79](start_span)onClick = { onSelect(item.mode) },[span_79](end_span)
                            modifier = Modifier.weight(1f),
                        )
                    }
                [span_80](start_span)}
            }
        }
    }
}

@Composable
private fun AiModeGrid(
    items: List<FlashModeItem>,
    currentMode: FlashMode,
    onSelect: (FlashMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {[span_80](end_span)
                row.forEach { item ->
                    AiModeCard(
                        item = item,
                        [span_81](start_span)isSelected = item.mode.id == currentMode.id,[span_81](end_span)
                        onClick = { onSelect(item.mode) },
                        modifier = Modifier.weight(1f),
                    )
                [span_82](start_span)}
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
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }[span_82](end_span)
    var showInfo by remember { mutableStateOf(false) }

    if (showInfo && item.info.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            [span_83](start_span)title = { Text(item.name, color = Color.White) },[span_83](end_span)
            text = { Text(item.info, color = LumiColor.Gray400, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } },
            containerColor = LumiColor.Navy800,
        )
    }

    Box(
        modifier = modifier
            [span_84](start_span).defaultMinSize(minHeight = 80.dp)[span_84](end_span)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) LumiColor.Navy700 else LumiColor.Navy800)
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) LumiColor.Amber400.copy(.7f) else LumiColor.Navy700,
                [span_85](start_span)shape = RoundedCornerShape(14.dp),[span_85](end_span)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = LumiColor.Amber400.copy(.1f)),
                [span_86](start_span)onClick = onClick,[span_86](end_span)
            )
            .padding(14.dp),
    ) {
        Column {
            Text(
                text = item.symbol,
                fontSize = 20.sp,
                [span_87](start_span)color = if (isSelected) LumiColor.Amber400 else LumiColor.Gray500,[span_87](end_span)
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                text = item.name,
                [span_88](start_span)fontSize = 13.sp,[span_88](end_span)
                fontWeight = FontWeight.W600,
                color = if (isSelected) Color.White else LumiColor.Gray400,
                letterSpacing = 0.02.sp,
            )
            Text(
                [span_89](start_span)text = item.desc,[span_89](end_span)
                fontSize = 10.sp,
                color = LumiColor.Gray600,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 2.dp),
            [span_90](start_span))
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.info.isNotBlank()) {[span_90](end_span)
                Text(
                    "ⓘ",
                    fontSize = 11.sp,
                    color = LumiColor.Gray600,
                    [span_91](start_span)modifier = Modifier.clickable([span_91](end_span)
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showInfo = true },
                    [span_92](start_span)),[span_92](end_span)
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        [span_93](start_span).size(6.dp)[span_93](end_span)
                        .clip(CircleShape)
                        .background(LumiColor.Amber400),
                )
            }
        }
    [span_94](start_span)}
}

@Composable
private fun AiModeCard(
    item: FlashModeItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val accent = item.accentColor
    var showInfo by remember { mutableStateOf(false) }

    if (showInfo && item.info.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(item.name, color = Color.White) },[span_94](end_span)
            text = { Text(item.info, color = LumiColor.Gray400, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } },
            containerColor = LumiColor.Navy800,
        )
    }

    Box(
        [span_95](start_span)modifier = modifier[span_95](end_span)
            .defaultMinSize(minHeight = 80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) LumiColor.Navy700 else LumiColor.Navy800)
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) accent.copy(.8f) else LumiColor.Navy700,
                [span_96](start_span)shape = RoundedCornerShape(14.dp),[span_96](end_span)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = accent.copy(.1f)),
                [span_97](start_span)onClick = onClick,[span_97](end_span)
            )
            .padding(14.dp),
    ) {
        Column {
            Text(
                text = item.symbol,
                [span_98](start_span)fontSize = 20.sp,[span_98](end_span)
                color = if (isSelected) accent else accent.copy(.6f),
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                text = item.name,
                [span_99](start_span)fontSize = 13.sp,[span_99](end_span)
                fontWeight = FontWeight.W600,
                color = if (isSelected) Color.White else LumiColor.Gray400,
                letterSpacing = 0.02.sp,
            )
            [span_100](start_span)Text([span_100](end_span)
                text = item.desc,
                fontSize = 10.sp,
                color = LumiColor.Gray600,
                lineHeight = 14.sp,
                [span_101](start_span)modifier = Modifier.padding(top = 2.dp),[span_101](end_span)
            )
            item.sensorTag?.let { tag ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    [span_102](start_span)modifier = Modifier.padding(top = 6.dp),[span_102](end_span)
                ) {
                    Box(
                        modifier = Modifier
                            [span_103](start_span).size(4.dp)[span_103](end_span)
                            .clip(CircleShape)
                            .background(accent.copy(if (isSelected) 1f else .4f)),
                    )
                    [span_104](start_span)Text([span_104](end_span)
                        text = tag,
                        fontSize = 9.sp,
                        color = accent.copy(if (isSelected) .7f else .4f),
                        [span_105](start_span)letterSpacing = 0.04.sp,[span_105](end_span)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            [span_106](start_span)horizontalArrangement = Arrangement.spacedBy(4.dp),[span_106](end_span)
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.info.isNotBlank()) {
                Text(
                    "ⓘ",
                    [span_107](start_span)fontSize = 11.sp,[span_107](end_span)
                    color = accent.copy(.5f),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        [span_108](start_span)indication = null,[span_108](end_span)
                        onClick = { showInfo = true },
                    ),
                )
            }
            if (isSelected) {
                [span_109](start_span)Box([span_109](end_span)
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        [span_110](start_span).background(accent),[span_110](end_span)
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
    [span_111](start_span)var local by remember { mutableFloatStateOf(value) }[span_111](end_span)
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!dragging) local = value }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LumiColor.Navy800)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    [span_112](start_span)) {[span_112](end_span)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                [span_113](start_span)text = label,[span_113](end_span)
                fontSize = 9.sp,
                letterSpacing = 0.14.sp,
                color = LumiColor.Gray600,
                [span_114](start_span)fontWeight = FontWeight.W500,[span_114](end_span)
            )
            Text(
                text = format(local),
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                [span_115](start_span)color = LumiColor.Amber400,[span_115](end_span)
            )
        }
        Slider(
            value = local,
            [span_116](start_span)onValueChange = { dragging = true; local = it },[span_116](end_span)
            [span_117](start_span)onValueChangeFinished = { dragging = false; onSettle(local) },[span_117](end_span)
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = LumiColor.Amber400,
                [span_118](start_span)activeTrackColor = LumiColor.Amber400,[span_118](end_span)
                inactiveTrackColor = LumiColor.Navy600,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

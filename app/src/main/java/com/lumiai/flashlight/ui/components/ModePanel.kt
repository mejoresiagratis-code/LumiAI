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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.OutlinedTextField
import com.lumiai.flashlight.core.util.MorseEncoder
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.ui.theme.LumiColor

// ── Mode descriptors ──────────────────────────────────────────────────────────

data class FlashModeItem(
    val mode: FlashMode,
    val name: String,
    val desc: String,
    val symbol: String,
    val accentColor: Color = LumiColor.Amber400,
    val sensorTag: String? = null,  // shown on AI cards
)

val flashModeItems = listOf(
    FlashModeItem(FlashMode.Steady,   "Steady",  "Full brightness, continuous", "⚡"),
    FlashModeItem(FlashMode.Screen,   "Screen",  "White screen, no flash",      "▢"),
    FlashModeItem(FlashMode.MorseCustom(), "Morse",  "Text to flash",              "—·"),
    FlashModeItem(FlashMode.Strobe(), "Strobe",  "1–20 Hz pulse",               "⊙"),
    FlashModeItem(FlashMode.Sos,      "SOS",     "Morse · · · — — —",           "◬"),
    FlashModeItem(FlashMode.Disco(),  "Disco",   "Beat sync · 60–200 BPM",      "◇"),
)

val aiModeItems = listOf(
    FlashModeItem(FlashMode.SmartBrightness, "Smart",   "Adapts to ambient light",      "◎",
        accentColor = Color(0xFFFFD84A), sensorTag = "Light sensor"),
    FlashModeItem(FlashMode.ReadingMode,     "Read",    "Warm pulse, auto-dims",         "☽",
        accentColor = Color(0xFFFF9A6C), sensorTag = "Timer curve"),
    FlashModeItem(FlashMode.AmbientSmart,    "Ambient", "Reads scene, picks pattern",   "⬨",
        accentColor = Color(0xFF34D399), sensorTag = "Lux + ML Kit"),
    FlashModeItem(FlashMode.CustomRhythm(),  "Custom",  "Pattern adapts to hour",         "⬡",
        accentColor = Color(0xFFA78BFA), sensorTag = "Clock + gen"),
    FlashModeItem(FlashMode.SleepTimer,      "Sleep",   "Fades out over 3 minutes",      "◌",
        accentColor = Color(0xFF4ADE80), sensorTag = "Duty curve"),
    FlashModeItem(FlashMode.Music,           "Music",   "Syncs flash to live beats",      "♩",
        accentColor = Color(0xFF60A5FA), sensorTag = "Microphone"),
    FlashModeItem(FlashMode.Walk,            "Walk",    "Pulse on every step",           "◉",
        accentColor = Color(0xFF818CF8), sensorTag = "Step detector"),
    FlashModeItem(FlashMode.Voice,           "Voice",   "Reacts to voice and sound",     "◍",
        accentColor = Color(0xFFF472B6), sensorTag = "Microphone"),
)

// ── ModePanel — tabbed Flash / AI ─────────────────────────────────────────────
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

        // ── Contextual slider (always on top, appears when needed) ────────────
        // Only show slider on Flash tab, never on AI tab
        // ── Tab row ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LumiColor.Navy800),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            listOf("Flash", "AI Modes").forEachIndexed { idx, label ->
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
                            active             -> LumiColor.White
                            else               -> LumiColor.Gray600
                        },
                        letterSpacing = 0.04.sp,
                    )
                }
            }
        }

        // ── Slider BELOW tabs — can never visually overlap cards ─────────────
        val showSlider = selectedTab == 0 && (currentMode is FlashMode.Strobe || currentMode is FlashMode.Disco)
        if (showSlider) {
            when (currentMode) {
                is FlashMode.Strobe -> key("strobe_slider") {
                    ContextSlider(
                        label    = "FREQUENCY",
                        value    = strobeHz,
                        range    = 1f..20f,
                        format   = { "${it.toInt()} Hz" },
                        onSettle = onStrobeHzChange,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                }
                is FlashMode.Disco -> key("disco_slider") {
                    ContextSlider(
                        label    = "TEMPO",
                        value    = discoBpm,
                        range    = 60f..200f,
                        format   = { "${it.toInt()} BPM" },
                        onSettle = onDiscoBpmChange,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                }
                else -> {}
            }
        }

        // ── Tab content ───────────────────────────────────────────────────────
        AnimatedContent(
            targetState   = selectedTab,
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
                    items       = flashModeItems,
                    currentMode = currentMode,
                    onSelect    = onModeSelect,
                    modifier    = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                AiModeGrid(
                    items       = aiModeItems,
                    currentMode = currentMode,
                    onSelect    = onModeSelect,
                    modifier    = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

// ── Flash mode grid — 2 cols ──────────────────────────────────────────────────
@Composable
private fun FlashModeGrid(
    items: List<FlashModeItem>,
    currentMode: FlashMode,
    onSelect: (FlashMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pair items: [Steady, Screen], [Strobe, SOS], [Disco]
    val rows = listOf(
        items.subList(0, 2),
        items.subList(2, 4),
        items.subList(4, 5),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            if (row.size == 1) {
                FlashModeCard(
                    item       = row[0],
                    isSelected = row[0].mode.id == currentMode.id,
                    onClick    = { onSelect(row[0].mode) },
                    modifier   = Modifier.fillMaxWidth(),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { item ->
                        FlashModeCard(
                            item       = item,
                            isSelected = item.mode.id == currentMode.id,
                            onClick    = { onSelect(item.mode) },
                            modifier   = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

// ── AI mode grid — 2 cols ─────────────────────────────────────────────────────
@Composable
private fun AiModeGrid(
    items: List<FlashModeItem>,
    currentMode: FlashMode,
    onSelect: (FlashMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { item ->
                    AiModeCard(
                        item       = item,
                        isSelected = item.mode.id == currentMode.id,
                        onClick    = { onSelect(item.mode) },
                        modifier   = Modifier.weight(1f),
                    )
                }
                // Pad last row if odd count
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ── Flash card ────────────────────────────────────────────────────────────────
@Composable
private fun FlashModeCard(
    item: FlashModeItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        if (isSelected) 1.0f else 0.98f,
        spring(Spring.DampingRatioMediumBouncy), label = "fscale",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) LumiColor.Navy700 else LumiColor.Navy800)
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) LumiColor.Amber400.copy(.7f) else LumiColor.Navy700,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = rememberRipple(color = LumiColor.Amber400.copy(.1f)),
                onClick           = onClick,
            )
            .padding(14.dp),
    ) {

        Column {
            Text(
                text     = item.symbol,
                fontSize = 18.sp,
                color    = if (isSelected) LumiColor.Amber400 else LumiColor.Gray600,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text       = item.name,
                fontSize   = 13.sp,
                fontWeight = FontWeight.W600,
                color      = if (isSelected) LumiColor.White else LumiColor.Gray400,
                letterSpacing = 0.02.sp,
            )
            Text(
                text     = item.desc,
                fontSize = 10.sp,
                color    = LumiColor.Gray600,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        // Selection dot
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(LumiColor.Amber400)
                    .align(Alignment.TopEnd),
            )
        }
    }
}

// ── AI mode card ──────────────────────────────────────────────────────────────
@Composable
private fun AiModeCard(
    item: FlashModeItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val accent = item.accentColor

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) LumiColor.Navy700 else LumiColor.Navy800)
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) accent.copy(.8f) else LumiColor.Navy700,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = rememberRipple(color = accent.copy(.1f)),
                onClick           = onClick,
            )
            .padding(14.dp),
    ) {

        Column {
            Text(
                text     = item.symbol,
                fontSize = 18.sp,
                color    = if (isSelected) accent else accent.copy(.5f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text       = item.name,
                fontSize   = 13.sp,
                fontWeight = FontWeight.W600,
                color      = if (isSelected) LumiColor.White else LumiColor.Gray400,
                letterSpacing = 0.02.sp,
            )
            Text(
                text     = item.desc,
                fontSize = 10.sp,
                color    = LumiColor.Gray600,
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
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(accent.copy(if (isSelected) 1f else .4f)),
                    )
                    Text(
                        text     = tag,
                        fontSize = 9.sp,
                        color    = accent.copy(if (isSelected) .7f else .4f),
                        letterSpacing = 0.04.sp,
                    )
                }
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(accent)
                    .align(Alignment.TopEnd),
            )
        }
    }
}

// ── Contextual slider ─────────────────────────────────────────────────────────
@Composable
private fun ContextSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
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
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text          = label,
                fontSize      = 9.sp,
                letterSpacing = 0.14.sp,
                color         = LumiColor.Gray600,
                fontWeight    = FontWeight.W500,
            )
            Text(
                text       = format(local),
                fontSize   = 13.sp,
                fontWeight = FontWeight.W600,
                color      = LumiColor.Amber400,
            )
        }
        Slider(
            value             = local,
            onValueChange     = { dragging = true; local = it },
            onValueChangeFinished = { dragging = false; onSettle(local) },
            valueRange        = range,
            colors            = SliderDefaults.colors(
                thumbColor         = LumiColor.Amber400,
                activeTrackColor   = LumiColor.Amber400,
                inactiveTrackColor = LumiColor.Navy600,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

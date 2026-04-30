package com.lumiai.flashlight.ui.components

import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.Canvas
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

// Note: mode item lists are built inline inside ModePanel() with string resources.
// Do not add top-level lists here — they can't use @Composable stringResource().





/**
 * Canvas-drawn mode icons — consistent stroke width, style and weight.
 * All icons use 2dp stroke, StrokeCap.Round, same visual weight as the flash bolt.
 */
@Composable
fun LumiModeIcon(
    id: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val stroke = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f; val cy = h / 2f
        when (id) {
            "flash" -> {  // lightning bolt — sharp zigzag
                val p = Path()
                p.moveTo(cx + w * 0.10f, cy - h * 0.44f)
                p.lineTo(cx - w * 0.16f, cy + h * 0.04f)
                p.lineTo(cx + w * 0.06f, cy + h * 0.04f)
                p.lineTo(cx - w * 0.10f, cy + h * 0.44f)
                p.lineTo(cx + w * 0.20f, cy - h * 0.08f)
                p.lineTo(cx + w * 0.02f, cy - h * 0.08f)
                p.close()
                drawPath(p, tint)
            }
            "screen" -> {  // rounded rectangle outline
                drawRoundRect(tint, topLeft = Offset(w*0.08f, h*0.16f),
                    size = Size(w*0.84f, h*0.68f), cornerRadius = CornerRadius(w*0.12f), style = stroke)
            }
            "morse" -> {  // dash + dot
                drawLine(tint, Offset(cx - w*0.32f, cy - h*0.08f), Offset(cx + w*0.04f, cy - h*0.08f), strokeWidth = 5f, cap = StrokeCap.Round)
                drawCircle(tint, radius = w*0.09f, center = Offset(cx + w*0.28f, cy - h*0.08f))
                drawLine(tint, Offset(cx - w*0.32f, cy + h*0.20f), Offset(cx - w*0.02f, cy + h*0.20f), strokeWidth = 5f, cap = StrokeCap.Round)
                drawCircle(tint, radius = w*0.09f, center = Offset(cx + w*0.18f, cy + h*0.20f))
                drawCircle(tint, radius = w*0.09f, center = Offset(cx + w*0.36f, cy + h*0.20f))
            }
            "strobe" -> {  // circle with radial lines (burst)
                drawCircle(tint, radius = w*0.18f, center = Offset(cx, cy), style = stroke)
                val angles = listOf(0f,45f,90f,135f,180f,225f,270f,315f)
                angles.forEach { a ->
                    val rad = Math.toRadians(a.toDouble())
                    val cos = Math.cos(rad).toFloat(); val sin = Math.sin(rad).toFloat()
                    drawLine(tint,
                        Offset(cx + cos*w*0.24f, cy + sin*h*0.24f),
                        Offset(cx + cos*w*0.38f, cy + sin*h*0.38f),
                        strokeWidth = 4f, cap = StrokeCap.Round)
                }
            }
            "sos" -> {  // triangle with exclamation inside
                val p = Path()
                p.moveTo(cx, h*0.06f)
                p.lineTo(w*0.94f, h*0.88f)
                p.lineTo(w*0.06f, h*0.88f)
                p.close()
                drawPath(p, tint, style = stroke)
                drawLine(tint, Offset(cx, h*0.34f), Offset(cx, h*0.64f), strokeWidth = 5f, cap = StrokeCap.Round)
                drawCircle(tint, radius = 3f, center = Offset(cx, h*0.76f))
            }
            "disco" -> {  // music note
                drawLine(tint, Offset(cx + w*0.16f, cy - h*0.36f), Offset(cx + w*0.16f, cy + h*0.10f), strokeWidth = 5f, cap = StrokeCap.Round)
                drawLine(tint, Offset(cx + w*0.16f, cy - h*0.36f), Offset(cx + w*0.42f, cy - h*0.28f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawCircle(tint, radius = w*0.14f, center = Offset(cx, cy + h*0.14f), style = stroke)
            }
            "smart" -> {  // sun/light bulb — circle + rays
                drawCircle(tint, radius = w*0.22f, center = Offset(cx, cy), style = stroke)
                listOf(0f, 60f, 120f, 180f, 240f, 300f).forEach { a ->
                    val rad = Math.toRadians(a.toDouble())
                    val cos = Math.cos(rad).toFloat(); val sin = Math.sin(rad).toFloat()
                    drawLine(tint, Offset(cx+cos*w*0.3f, cy+sin*h*0.3f),
                        Offset(cx+cos*w*0.42f, cy+sin*h*0.42f), strokeWidth = 4f, cap = StrokeCap.Round)
                }
            }
            "read" -> {  // open book
                drawLine(tint, Offset(cx, cy - h*0.36f), Offset(cx, cy + h*0.36f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawArc(tint, startAngle = -90f, sweepAngle = -180f,
                    useCenter = false, topLeft = Offset(w*0.06f, cy - h*0.36f),
                    size = Size(w*0.44f, h*0.72f), style = stroke)
                drawArc(tint, startAngle = -90f, sweepAngle = 180f,
                    useCenter = false, topLeft = Offset(cx, cy - h*0.36f),
                    size = Size(w*0.44f, h*0.72f), style = stroke)
            }
            "ambient" -> {  // eye shape
                drawArc(tint, startAngle = 0f, sweepAngle = -180f, useCenter = false,
                    topLeft = Offset(w*0.08f, cy - h*0.24f), size = Size(w*0.84f, h*0.48f), style = stroke)
                drawArc(tint, startAngle = 0f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(w*0.08f, cy - h*0.24f), size = Size(w*0.84f, h*0.48f), style = stroke)
                drawCircle(tint, radius = w*0.12f, center = Offset(cx, cy), style = stroke)
            }
            "custom" -> {  // clock with hand
                drawCircle(tint, radius = w*0.38f, center = Offset(cx, cy), style = stroke)
                drawLine(tint, Offset(cx, cy), Offset(cx, cy - h*0.28f), strokeWidth = 4.5f, cap = StrokeCap.Round)
                drawLine(tint, Offset(cx, cy), Offset(cx + w*0.20f, cy + h*0.10f), strokeWidth = 4.5f, cap = StrokeCap.Round)
            }
            "sleep" -> {  // crescent moon
                val p = Path()
                p.addOval(androidx.compose.ui.geometry.Rect(cx - w*0.30f, cy - h*0.40f, cx + w*0.30f, cy + h*0.40f))
                drawPath(p, tint, style = stroke)
                drawArc(tint, startAngle = -60f, sweepAngle = -180f, useCenter = false,
                    topLeft = Offset(cx - w*0.04f, cy - h*0.38f), size = Size(w*0.40f, h*0.76f),
                    style = Fill, colorFilter = null)
                // Simpler: just draw crescent via two overlapping circles
            }
            "music" -> {  // waveform — 3 vertical bars
                listOf(-0.28f, 0f, 0.28f).forEachIndexed { i, xOff ->
                    val barH = if (i == 1) h*0.7f else h*0.45f
                    drawRoundRect(tint,
                        topLeft = Offset(cx + w*xOff - w*0.07f, cy - barH/2f),
                        size = Size(w*0.14f, barH),
                        cornerRadius = CornerRadius(w*0.07f))
                }
            }
            "walk" -> {  // footstep / person walking
                drawCircle(tint, radius = w*0.13f, center = Offset(cx, cy - h*0.35f), style = stroke)
                drawLine(tint, Offset(cx, cy - h*0.22f), Offset(cx, cy + h*0.10f), strokeWidth = 5f, cap = StrokeCap.Round)
                drawLine(tint, Offset(cx, cy - h*0.04f), Offset(cx - w*0.22f, cy + h*0.06f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(tint, Offset(cx, cy - h*0.04f), Offset(cx + w*0.18f, cy + h*0.06f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(tint, Offset(cx, cy + h*0.10f), Offset(cx - w*0.18f, cy + h*0.38f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(tint, Offset(cx, cy + h*0.10f), Offset(cx + w*0.20f, cy + h*0.38f), strokeWidth = 4f, cap = StrokeCap.Round)
            }
            "voice" -> {  // microphone
                drawRoundRect(tint, topLeft = Offset(cx - w*0.14f, cy - h*0.40f),
                    size = Size(w*0.28f, h*0.50f), cornerRadius = CornerRadius(w*0.14f), style = stroke)
                drawArc(tint, startAngle = 0f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(cx - w*0.28f, cy - h*0.10f),
                    size = Size(w*0.56f, h*0.44f), style = stroke)
                drawLine(tint, Offset(cx, cy + h*0.22f), Offset(cx, cy + h*0.44f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(tint, Offset(cx - w*0.16f, cy + h*0.44f), Offset(cx + w*0.16f, cy + h*0.44f), strokeWidth = 4f, cap = StrokeCap.Round)
            }
            else -> {  // fallback: simple circle
                drawCircle(tint, radius = w*0.30f, center = Offset(cx, cy), style = stroke)
            }
        }
    }
}

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
    onPaywall: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Only show the AI Modes tab if at least one Pro mode is visible (hidden=false).
    // During the free-only launch all Pro modes have hidden=true, so this tab disappears.
    val visibleAiItems = listOf(
        FlashModeItem(FlashMode.SmartBrightness, stringResource(R.string.mode_smart), stringResource(R.string.mode_smart_desc), "smart", accentColor = Color(0xFFFFD84AL), sensorTag = stringResource(R.string.sensor_light), info = stringResource(R.string.mode_smart_info)),
        FlashModeItem(FlashMode.ReadingMode, stringResource(R.string.mode_read), stringResource(R.string.mode_read_desc), "read", accentColor = Color(0xFFFF9A6CL), sensorTag = stringResource(R.string.sensor_timer), info = stringResource(R.string.mode_read_info)),
        FlashModeItem(FlashMode.AmbientSmart, stringResource(R.string.mode_ambient), stringResource(R.string.mode_ambient_desc), "ambient", accentColor = Color(0xFF34D399L), sensorTag = stringResource(R.string.sensor_lux_ml), info = stringResource(R.string.mode_ambient_info)),
        FlashModeItem(FlashMode.CustomRhythm(), stringResource(R.string.mode_custom), stringResource(R.string.mode_custom_desc), "custom", accentColor = Color(0xFFA78BFAL), sensorTag = stringResource(R.string.sensor_clock), info = stringResource(R.string.mode_custom_info)),
        FlashModeItem(FlashMode.SleepTimer, stringResource(R.string.mode_sleep), stringResource(R.string.mode_sleep_desc), "sleep", accentColor = Color(0xFF4ADE80L), sensorTag = stringResource(R.string.sensor_duty), info = stringResource(R.string.mode_sleep_info)),
        FlashModeItem(FlashMode.Music, stringResource(R.string.mode_music), stringResource(R.string.mode_music_desc), "music", accentColor = Color(0xFF60A5FAL), sensorTag = stringResource(R.string.sensor_mic), info = stringResource(R.string.mode_music_info)),
        FlashModeItem(FlashMode.Walk, stringResource(R.string.mode_walk), stringResource(R.string.mode_walk_desc), "walk", accentColor = Color(0xFF818CF8L), sensorTag = stringResource(R.string.sensor_step), info = stringResource(R.string.mode_walk_info)),
        FlashModeItem(FlashMode.Voice, stringResource(R.string.mode_voice), stringResource(R.string.mode_voice_desc), "voice", accentColor = Color(0xFFF472B6L), sensorTag = stringResource(R.string.sensor_mic), info = stringResource(R.string.mode_voice_info)),
    ).filter { !it.mode.hidden }

    val hasAiModes = visibleAiItems.isNotEmpty()

    // selectedTab: default to Flash (0). If current mode is a visible Pro mode, show AI tab.
    var selectedTab by remember {
        mutableIntStateOf(if (currentMode.isPro && !currentMode.hidden && hasAiModes) 1 else 0)
    }
    // Snap back to Flash tab if AI tab disappears (all modes hidden)
    if (!hasAiModes && selectedTab == 1) selectedTab = 0

    Column(modifier = modifier) {
        // Only render the tab row when AI modes are visible
        if (hasAiModes) {
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
        } // end if hasAiModes

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
            if (tab == 0 || !hasAiModes) {
                FlashModeGrid(
                    items = listOf(
                        FlashModeItem(FlashMode.Steady, stringResource(R.string.mode_steady), stringResource(R.string.mode_steady_desc), "flash", info = stringResource(R.string.mode_steady_info)),
                        FlashModeItem(FlashMode.Screen, stringResource(R.string.mode_screen), stringResource(R.string.mode_screen_desc), "screen", info = stringResource(R.string.mode_screen_info)),
                        FlashModeItem(FlashMode.MorseCustom(), stringResource(R.string.mode_morse), stringResource(R.string.mode_morse_desc), "morse", info = stringResource(R.string.mode_morse_info)),
                        FlashModeItem(FlashMode.Strobe(), stringResource(R.string.mode_strobe), stringResource(R.string.mode_strobe_desc), "strobe", info = stringResource(R.string.mode_strobe_info)),
                        FlashModeItem(FlashMode.Sos, stringResource(R.string.mode_sos), stringResource(R.string.mode_sos_desc), "sos", info = stringResource(R.string.mode_sos_info)),
                        FlashModeItem(FlashMode.Disco(), stringResource(R.string.mode_disco), stringResource(R.string.mode_disco_desc), "disco", info = stringResource(R.string.mode_disco_info)),
                    ),
                    currentMode = currentMode,
                    onSelect = onModeSelect,
                    onConfig = onModeConfig,
                    onPaywall = onPaywall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                AiModeGrid(
                    items = visibleAiItems,   // already filtered — no hidden modes
                    currentMode = currentMode,
                    isPro = isPro,
                    onSelect = onModeSelect,
                    onConfig = onModeConfig,
                    onPaywall = onPaywall,
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
    onPaywall: () -> Unit = {},
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
    isPro: Boolean = false,
    onSelect: (FlashMode) -> Unit,
    onConfig: (FlashMode) -> Unit = {},
    onPaywall: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { item ->
                    AiModeCard(
                        item = item,
                        isSelected = item.mode.id == currentMode.id,
                        isPro = isPro,
                        onClick = { onSelect(item.mode) },
                        onConfig = { onConfig(item.mode) },
                        onPaywall = onPaywall,
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
            .height(100.dp)
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
            LumiModeIcon(
                id = item.symbol,
                tint = if (isSelected) LumiColor.Amber400 else LumiColor.Gray500,
                modifier = Modifier.size(22.dp).padding(bottom = 6.dp),
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
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ⚙ config button — 40×40dp touch target (Material min 48dp, 40 acceptable for icon-in-card)
            val configModes = setOf("strobe","disco","morse_custom","smart_brightness","sleep_timer","music","voice")
            if (item.mode.id in configModes) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onConfig,
                        ),
                ) {
                    Text("⚙", fontSize = 11.sp, color = LumiColor.Amber400.copy(.6f))
                }
            }
            if (item.info.isNotBlank()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showInfo = true },
                        ),
                ) {
                    Text("ⓘ", fontSize = 11.sp, color = LumiColor.Gray600)
                }
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
    onPaywall: () -> Unit = {},
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
            .height(100.dp)
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
                onClick = { if (isLocked) onPaywall() else onClick() },
            )
            .padding(14.dp),
    ) {
        // PRO badge — centered on right edge
        if (isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LumiColor.Amber400)
                    .padding(horizontal = 4.dp, vertical = 3.dp),
            ) {
                Text(
                    "PRO",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = 0.1.sp,
                    color = LumiColor.Navy950,
                )
            }
        }
        Column {
            LumiModeIcon(
                id = item.symbol,
                tint = if (isSelected) accent else accent.copy(.55f),
                modifier = Modifier.size(22.dp).padding(bottom = 6.dp),
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
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val aiConfigModes = setOf("smart_brightness","sleep_timer","music","voice","ambient_smart","custom_rhythm")
            if (item.mode.id in aiConfigModes) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { if (isLocked) onPaywall() else onConfig() },
                        ),
                ) {
                    Text("⚙", fontSize = 11.sp, color = accent.copy(.6f))
                }
            }
            if (item.info.isNotBlank()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { if (isLocked) onPaywall() else showInfo = true },
                        ),
                ) {
                    Text("ⓘ", fontSize = 11.sp, color = accent.copy(.5f))
                }
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

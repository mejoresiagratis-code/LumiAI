package com.lumiai.flashlight.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.ui.theme.LumiColor

// ── Each AI mode has its own visual identity ──────────────────────────────────
data class AiModeStyle(
    val symbol: String,       // Unicode symbol shown above label
    val accentColor: Color,   // unique per mode
    val gradientEnd: Color,   // selected background gradient end
    val description: String,  // what it actually does
)

val aiModeStyles: Map<String, AiModeStyle> = mapOf(
    "smart_brightness" to AiModeStyle(
        symbol      = "◎",
        accentColor = Color(0xFFFBBF24),  // warm amber
        gradientEnd = Color(0xFFB45309),
        description = "Auto adjust",
    ),
    "reading_mode" to AiModeStyle(
        symbol      = "☽",
        accentColor = Color(0xFFFF9A6C),  // warm orange
        gradientEnd = Color(0xFFB45309),
        description = "Warm light",
    ),
    "ambient_smart" to AiModeStyle(
        symbol      = "◈",
        accentColor = Color(0xFF34D399),  // teal/green
        gradientEnd = Color(0xFF065F46),
        description = "Scene detect",
    ),
    "custom_rhythm" to AiModeStyle(
        symbol      = "⬡",
        accentColor = Color(0xFFA78BFA),  // light purple
        gradientEnd = Color(0xFF4C1D95),
        description = "AI rhythm",
    ),
    "sleep_timer" to AiModeStyle(
        symbol      = "◌",
        accentColor = Color(0xFF60A5FA),  // soft blue
        gradientEnd = Color(0xFF1E3A5F),
        description = "Fade out",
    ),
)

data class ModeItem(
    val mode: FlashMode,
    val label: String,
    val sublabel: String,
    val isAi: Boolean = false,
)

val freeModeItems = listOf(
    ModeItem(FlashMode.Steady,   "STEADY", "Continuous"),
    ModeItem(FlashMode.Screen,   "SCREEN", "White fill"),
    ModeItem(FlashMode.Sos,      "SOS",    "· · · — — —"),
    ModeItem(FlashMode.Strobe(), "STROBE", "1–20 Hz"),
    ModeItem(FlashMode.Disco(),  "DISCO",  "Beat sync"),
)

val proModeItems = listOf(
    ModeItem(FlashMode.SmartBrightness, "SMART",   "Auto adjust", isAi = true),
    ModeItem(FlashMode.ReadingMode,     "READ",    "Warm light",  isAi = true),
    ModeItem(FlashMode.AmbientSmart,    "AMBIENT", "Scene detect",isAi = true),
    ModeItem(FlashMode.CustomRhythm(),  "CUSTOM",  "AI rhythm",   isAi = true),
    ModeItem(FlashMode.SleepTimer,      "SLEEP",   "Fade out",    isAi = true),
)

@Composable
fun ModeSelector(
    currentMode: FlashMode,
    isPro: Boolean,
    onModeSelect: (FlashMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Free modes ─────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            freeModeItems.forEach { item ->
                FreeModeChip(
                    item       = item,
                    isSelected = item.mode.id == currentMode.id,
                    onClick    = { onModeSelect(item.mode) },
                )
            }
        }

        // ── AI modes — each visually unique ────────────────────────────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // "AI" label pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LumiColor.Purple900)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(
                    "AI",
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.W700,
                    letterSpacing = 0.06.sp,
                    color         = LumiColor.Purple300,
                )
            }

            proModeItems.forEach { item ->
                val style = aiModeStyles[item.mode.id]
                    ?: AiModeStyle("✦", LumiColor.Purple400, LumiColor.Purple600, item.sublabel)
                AiModeChip(
                    item       = item,
                    style      = style,
                    isSelected = item.mode.id == currentMode.id,
                    onClick    = { onModeSelect(item.mode) },
                )
            }
        }
    }
}

// ── Free mode chip — amber selection, simple layout ───────────────────────────
@Composable
private fun FreeModeChip(
    item: ModeItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.0f else 0.97f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "free_scale_${item.label}",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier
            .scale(scale)
            .defaultMinSize(minWidth = 64.dp, minHeight = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) LumiColor.Amber400 else LumiColor.Navy700)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) LumiColor.Amber500 else LumiColor.Navy600,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = rememberRipple(color = LumiColor.Amber400.copy(alpha = 0.2f)),
                onClick           = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics { contentDescription = "${item.label} mode" },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text          = item.label,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.W700,
                letterSpacing = 0.04.sp,
                color         = if (isSelected) LumiColor.Navy900 else LumiColor.Gray400,
                textAlign     = TextAlign.Center,
            )
            Text(
                text      = item.sublabel,
                fontSize  = 9.sp,
                color     = if (isSelected) LumiColor.Navy900.copy(0.65f) else LumiColor.Gray600,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── AI mode chip — each has unique symbol + accent color ─────────────────────
@Composable
private fun AiModeChip(
    item: ModeItem,
    style: AiModeStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.02f else 0.97f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "ai_scale_${item.label}",
    )

    val bgModifier = if (isSelected) {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(style.accentColor.copy(alpha = 0.85f), style.gradientEnd)
            )
        )
    } else {
        Modifier.background(LumiColor.Navy700)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier
            .scale(scale)
            .defaultMinSize(minWidth = 64.dp, minHeight = 60.dp)  // taller for symbol
            .clip(RoundedCornerShape(10.dp))
            .then(bgModifier)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) style.accentColor else style.accentColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = rememberRipple(color = style.accentColor.copy(alpha = 0.25f)),
                onClick           = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics { contentDescription = "${item.label} AI mode" },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            // Unique symbol — the main visual differentiator
            Text(
                text     = style.symbol,
                fontSize = 14.sp,
                color    = if (isSelected) Color.White else style.accentColor.copy(alpha = 0.7f),
            )
            Text(
                text          = item.label,
                fontSize      = 10.sp,
                fontWeight    = FontWeight.W700,
                letterSpacing = 0.04.sp,
                color         = if (isSelected) Color.White else style.accentColor,
                textAlign     = TextAlign.Center,
            )
            Text(
                text      = style.description,
                fontSize  = 8.5.sp,
                color     = if (isSelected) Color.White.copy(0.75f) else LumiColor.Gray500,
                textAlign = TextAlign.Center,
            )
        }
    }
}

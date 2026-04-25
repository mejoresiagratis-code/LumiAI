package com.lumiai.flashlight.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

data class ModeItem(
    val mode: FlashMode,
    val label: String,
    val sublabel: String,
    val isPro: Boolean = false,
)

val freeModeItems = listOf(
    ModeItem(FlashMode.Steady, "STEADY", "Continuous"),
    ModeItem(FlashMode.Screen, "SCREEN", "White fill"),
    ModeItem(FlashMode.Sos,    "SOS",    "Morse · · ·"),
    ModeItem(FlashMode.Strobe(), "STROBE", "1–20 Hz"),
    ModeItem(FlashMode.Disco(),  "DISCO",  "Beat sync"),
)

val proModeItems = listOf(
    ModeItem(FlashMode.SmartBrightness, "SMART",   "AI adaptive", isPro = true),
    ModeItem(FlashMode.ReadingMode,     "READ",    "Warm AI",      isPro = true),
    ModeItem(FlashMode.AmbientSmart,    "AMBIENT", "Scene detect", isPro = true),
    ModeItem(FlashMode.CustomRhythm(),  "CUSTOM",  "AI rhythm",    isPro = true),
    ModeItem(FlashMode.SleepTimer,      "SLEEP",   "Fade out",     isPro = true),
)

@Composable
fun ModeSelector(
    currentMode: FlashMode,
    isPro: Boolean,
    onModeSelect: (FlashMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Section label ──────────────────────────────────────────────────
        SectionLabel("MODES")

        // ── Free modes row ─────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            freeModeItems.forEach { item ->
                ModeChip(
                    item       = item,
                    isSelected = item.mode.id == currentMode.id,
                    locked     = false,
                    onClick    = { onModeSelect(item.mode) },
                )
            }
        }

        // ── Pro modes row ──────────────────────────────────────────────────
        SectionLabel("AI PRO", isProLabel = true)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            proModeItems.forEach { item ->
                ModeChip(
                    item       = item,
                    isSelected = item.mode.id == currentMode.id,
                    locked     = !isPro,
                    onClick    = { onModeSelect(item.mode) },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, isProLabel: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text       = text,
            fontSize   = 10.sp,
            fontWeight = FontWeight.W600,
            letterSpacing = 0.12.sp,
            color      = if (isProLabel) LumiColor.Purple400 else LumiColor.Gray500,
        )
        if (isProLabel) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(LumiColor.Purple900)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "ONE-TIME UNLOCK",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = 0.06.sp,
                    color = LumiColor.Purple300,
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    item: ModeItem,
    isSelected: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor = when {
        isSelected && item.isPro -> LumiColor.Purple500
        isSelected               -> LumiColor.Amber400
        else                     -> LumiColor.Navy700
    }
    val borderColor = when {
        isSelected && item.isPro -> LumiColor.Purple400
        isSelected               -> LumiColor.Amber400
        item.isPro               -> LumiColor.Purple500.copy(alpha = 0.3f)
        else                     -> LumiColor.Navy600
    }
    val labelColor = when {
        isSelected && item.isPro -> LumiColor.White
        isSelected               -> LumiColor.Navy900
        item.isPro               -> LumiColor.Purple300
        else                     -> LumiColor.Gray400
    }
    val sublabelColor = when {
        isSelected -> labelColor.copy(alpha = 0.7f)
        else       -> LumiColor.Gray500
    }

    val chipScale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.97f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_scale",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(72.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = LumiColor.Amber400.copy(alpha = 0.2f)),
                onClick = onClick,
            )
            .semantics {
                contentDescription = "${item.label} mode${if (locked) ", requires Pro" else ""}"
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Lock indicator for pro modes when locked
            if (locked) {
                Text("🔒", fontSize = 10.sp)
            }
            Text(
                text       = item.label,
                fontSize   = 11.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = 0.05.sp,
                color      = labelColor,
                textAlign  = TextAlign.Center,
            )
            Text(
                text       = item.sublabel,
                fontSize   = 9.sp,
                fontWeight = FontWeight.W400,
                color      = sublabelColor,
                textAlign  = TextAlign.Center,
                letterSpacing = 0.02.sp,
            )
        }
    }
}

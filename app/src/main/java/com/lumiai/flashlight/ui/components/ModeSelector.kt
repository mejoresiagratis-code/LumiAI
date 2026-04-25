package com.lumiai.flashlight.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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

data class ModeItem(
    val mode: FlashMode,
    val label: String,
    val sublabel: String,
    val isAi: Boolean = false,
)

val allModeItems = listOf(
    // Free modes
    ModeItem(FlashMode.Steady,       "STEADY",   "Continuous"),
    ModeItem(FlashMode.Screen,       "SCREEN",   "White fill"),
    ModeItem(FlashMode.Sos,          "SOS",      "· · · — — —"),
    ModeItem(FlashMode.Strobe(),     "STROBE",   "1–20 Hz"),
    ModeItem(FlashMode.Disco(),      "DISCO",    "Beat sync"),
    // AI modes (unlocked, marked visually)
    ModeItem(FlashMode.SmartBrightness, "SMART",   "AI adaptive", isAi = true),
    ModeItem(FlashMode.ReadingMode,     "READ",    "Warm AI",      isAi = true),
    ModeItem(FlashMode.AmbientSmart,    "AMBIENT", "Scene AI",     isAi = true),
    ModeItem(FlashMode.CustomRhythm(),  "CUSTOM",  "AI rhythm",    isAi = true),
    ModeItem(FlashMode.SleepTimer,      "SLEEP",   "Fade out",     isAi = true),
)

// Keep for backwards compatibility
val freeModeItems  = allModeItems.filter { !it.isAi }
val proModeItems   = allModeItems.filter { it.isAi }

@Composable
fun ModeSelector(
    currentMode: FlashMode,
    isPro: Boolean,          // kept for future use; currently all modes unlocked
    onModeSelect: (FlashMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Free modes — single scrolling row at top
        ModeRow(
            items       = freeModeItems,
            currentMode = currentMode,
            onSelect    = onModeSelect,
        )

        // AI modes row — visually distinct with subtle purple tint
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // AI label pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LumiColor.Purple900)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(
                    "AI",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = 0.08.sp,
                    color = LumiColor.Purple300,
                )
            }
            ModeRow(
                items       = proModeItems,
                currentMode = currentMode,
                onSelect    = onModeSelect,
                isAiRow     = true,
            )
        }
    }
}

@Composable
private fun ModeRow(
    items: List<ModeItem>,
    currentMode: FlashMode,
    onSelect: (FlashMode) -> Unit,
    isAiRow: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { item ->
            ModeChip(
                item       = item,
                isSelected = item.mode.id == currentMode.id,
                isAiRow    = isAiRow,
                onClick    = { onSelect(item.mode) },
            )
        }
    }
}

@Composable
private fun ModeChip(
    item: ModeItem,
    isSelected: Boolean,
    isAiRow: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue    = if (isSelected) 1.0f else 0.97f,
        animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label          = "chip_scale_${item.label}",
    )

    val bgColor = when {
        isSelected && isAiRow -> LumiColor.Purple500
        isSelected            -> LumiColor.Amber400
        isAiRow               -> LumiColor.Navy700
        else                  -> LumiColor.Navy700
    }
    val borderColor = when {
        isSelected && isAiRow -> LumiColor.Purple400
        isSelected            -> LumiColor.Amber500
        isAiRow               -> LumiColor.Purple500.copy(alpha = 0.25f)
        else                  -> LumiColor.Navy600
    }
    val labelColor = when {
        isSelected && isAiRow -> LumiColor.White
        isSelected            -> LumiColor.Navy900
        isAiRow               -> LumiColor.Purple300
        else                  -> LumiColor.Gray400
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .scale(scale)
            // Min 48dp height for touch target (Material guideline)
            .defaultMinSize(minWidth = 64.dp, minHeight = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = rememberRipple(
                    color = if (isAiRow) LumiColor.Purple300.copy(alpha = 0.2f)
                            else LumiColor.Amber400.copy(alpha = 0.2f),
                ),
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics {
                contentDescription = "${item.label} flash mode"
            },
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
                color         = labelColor,
                textAlign     = TextAlign.Center,
            )
            Text(
                text      = item.sublabel,
                fontSize  = 9.sp,
                color     = labelColor.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

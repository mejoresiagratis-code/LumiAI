package com.lumiai.flashlight.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.ui.theme.LumiColor

/**
 * Contextual controls that appear based on the active mode.
 * Strobe → Hz slider | Disco → BPM slider | Screen → brightness slider
 */
@Composable
fun ModeControls(
    currentMode: FlashMode,
    strobeHz: Float,
    discoBpm: Float,
    screenBrightness: Float,
    onStrobeHzChange: (Float) -> Unit,
    onDiscoBpmChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = currentMode is FlashMode.Strobe ||
                  currentMode is FlashMode.Disco  ||
                  currentMode is FlashMode.Screen,
        enter = fadeIn() + expandVertically(),
        exit  = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LumiColor.Navy800)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            when (currentMode) {
                is FlashMode.Strobe -> ControlSlider(
                    label    = "STROBE",
                    value    = strobeHz,
                    valueLabel = "${strobeHz.toInt()} Hz",
                    range    = 0.5f..20f,
                    accentColor = LumiColor.Amber400,
                    onChange = onStrobeHzChange,
                )
                is FlashMode.Disco  -> ControlSlider(
                    label    = "TEMPO",
                    value    = discoBpm,
                    valueLabel = "${discoBpm.toInt()} BPM",
                    range    = 60f..200f,
                    accentColor = LumiColor.Amber500,
                    onChange = onDiscoBpmChange,
                )
                is FlashMode.Screen -> ControlSlider(
                    label    = "BRIGHTNESS",
                    value    = screenBrightness,
                    valueLabel = "${(screenBrightness * 100).toInt()}%",
                    range    = 0.1f..1f,
                    accentColor = LumiColor.White,
                    onChange = onBrightnessChange,
                )
                else -> {}
            }
        }
    }
}

@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    valueLabel: String,
    range: ClosedFloatingPointRange<Float>,
    accentColor: androidx.compose.ui.graphics.Color,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = 0.1.sp,
                color = LumiColor.Gray500,
            )
            Text(
                text = valueLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                color = accentColor,
            )
        }
        Spacer(Modifier.height(6.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor        = accentColor,
                activeTrackColor  = accentColor,
                inactiveTrackColor = LumiColor.Navy600,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

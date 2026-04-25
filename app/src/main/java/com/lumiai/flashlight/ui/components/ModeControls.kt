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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.ui.theme.LumiColor

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
        visible  = currentMode is FlashMode.Strobe ||
                   currentMode is FlashMode.Disco  ||
                   currentMode is FlashMode.Screen,
        enter    = fadeIn() + expandVertically(),
        exit     = fadeOut() + shrinkVertically(),
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
                is FlashMode.Strobe -> key("strobe_slider") {
                    LiveSlider(
                        label         = "STROBE",
                        externalValue = strobeHz,
                        range         = 0.5f..20f,
                        accentColor   = LumiColor.Amber400,
                        formatValue   = { "${it.toInt()} Hz" },
                        onSettled     = onStrobeHzChange,
                    )
                }
                is FlashMode.Disco -> key("disco_slider") {
                    LiveSlider(
                        label         = "TEMPO",
                        externalValue = discoBpm,
                        range         = 60f..200f,
                        accentColor   = LumiColor.Amber500,
                        formatValue   = { "${it.toInt()} BPM" },
                        onSettled     = onDiscoBpmChange,
                    )
                }
                is FlashMode.Screen -> LiveSlider(
                    label        = "BRIGHTNESS",
                    externalValue = screenBrightness,
                    range        = 0.1f..1f,
                    accentColor  = LumiColor.White,
                    formatValue  = { "${(it * 100).toInt()}%" },
                    onSettled    = onBrightnessChange,
                )
                else -> {}
            }
        }
    }
}

/**
 * Slider que responde visualmente de forma inmediata mientras se arrastra
 * y propaga el valor final al ViewModel solo al soltar (onSettled).
 *
 * El truco: mantiene estado local [localValue] que se actualiza en cada frame
 * del drag. El [externalValue] (del StateFlow del ViewModel) solo se usa para
 * inicializar — no se reaplica durante el drag para evitar el efecto de
 * "freezing" causado por la latencia DataStore → StateFlow → recomposición.
 */
@Composable
private fun LiveSlider(
    label: String,
    externalValue: Float,
    range: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    formatValue: (Float) -> String,
    onSettled: (Float) -> Unit,
) {
    // Local state: initialized from external, updated instantly on drag
    var localValue by remember { mutableFloatStateOf(externalValue) }
    var isDragging by remember { mutableStateOf(false) }

    // Sync from external only when NOT dragging (avoids overwriting during drag)
    LaunchedEffect(externalValue) {
        if (!isDragging) localValue = externalValue
    }

    Column {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text          = label,
                fontSize      = 10.sp,
                fontWeight    = FontWeight.W600,
                letterSpacing = 0.1.sp,
                color         = LumiColor.Gray500,
            )
            Text(
                text       = formatValue(localValue),
                fontSize   = 13.sp,
                fontWeight = FontWeight.W700,
                color      = accentColor,
            )
        }
        Spacer(Modifier.height(6.dp))
        Slider(
            value    = localValue,
            onValueChange = { newValue ->
                isDragging   = true
                localValue   = newValue   // instant visual update
            },
            onValueChangeFinished = {
                isDragging = false
                onSettled(localValue)     // persist + apply to flash only on release
            },
            valueRange = range,
            colors     = SliderDefaults.colors(
                thumbColor         = accentColor,
                activeTrackColor   = accentColor,
                inactiveTrackColor = LumiColor.Navy600,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

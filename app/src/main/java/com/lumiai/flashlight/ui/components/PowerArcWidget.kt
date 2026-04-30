package com.lumiai.flashlight.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.core.util.EnergyEstimator
import com.lumiai.flashlight.ui.theme.LumiColor
import kotlin.math.*

/**
 * Power Arc Widget — replaces the FlashButton on the main screen.
 *
 * OFF state: Grey arc showing battery level. Center shows mode name.
 * ON state:  Amber arc lit up. Dragging the arc changes intensity.
 *            Center shows estimated runtime recalculated live.
 *
 * The arc runs from 150° to 390° (240° sweep) — bottom-weighted horseshoe.
 * Drag position maps linearly to intensity 0.1–1.0.
 */
@Composable
fun PowerArcWidget(
    isOn: Boolean,
    isScreenMode: Boolean,
    currentMode: FlashMode,
    batteryLevel: Float,          // 0.0–1.0
    isCharging: Boolean,
    intensity: Float,             // current torch/screen intensity 0.1–1.0
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Estimate runtime based on current settings
    val estimatedMinutes = remember(batteryLevel, currentMode, intensity) {
        EnergyEstimator.estimateMinutesRemaining(batteryLevel, currentMode, intensity)
    }
    val runtimeText = remember(estimatedMinutes) {
        EnergyEstimator.formatMinutes(estimatedMinutes)
    }

    // Arc constants
    val startAngle = 150f   // degrees (0 = right, clockwise)
    val sweepAngle = 240f   // total arc span
    val arcPadding = 28.dp

    // Animated fill — battery level (off) or intensity (on)
    val fillTarget = if (isOn) intensity else batteryLevel
    val animatedFill by animateFloatAsState(
        targetValue = fillTarget,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "arcFill",
    )

    // Glow pulse when ON
    val infiniteTransition = rememberInfiniteTransition(label = "arcGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha",
    )

    // Drag state — tracks finger position mapped to intensity
    var isDragging by remember { mutableStateOf(false) }
    var arcCenter by remember { mutableStateOf(Offset.Zero) }
    var arcRadius by remember { mutableStateOf(0f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.15f)   // slightly wider than tall
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(arcPadding)
                .pointerInput(isOn) {
                    if (!isOn) return@pointerInput
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd   = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, _ ->
                            change.consume()
                            val pos = change.position
                            // Map drag position to angle, then to intensity
                            val angle = atan2(
                                (pos.y - arcCenter.y).toDouble(),
                                (pos.x - arcCenter.x).toDouble()
                            ).let { Math.toDegrees(it).toFloat() }
                                .let { if (it < 0) it + 360f else it }

                            // Normalize angle within arc sweep (150°–390°)
                            var normalized = angle - startAngle
                            if (normalized < 0) normalized += 360f
                            if (normalized > sweepAngle) normalized = normalized.coerceIn(0f, sweepAngle)
                            val newIntensity = (normalized / sweepAngle)
                                .coerceIn(0.1f, 1.0f)
                            onIntensityChange(newIntensity)
                        }
                    )
                }
        ) {
            arcCenter = Offset(size.width / 2f, size.height / 2f)
            arcRadius = size.width / 2f

            val strokeWidth = 14.dp.toPx()
            val arcRect = Size(size.width, size.height)
            val topLeft = Offset(0f, 0f)

            // ── Track (background arc) ─────────────────────────────────────
            drawArc(
                color      = if (isOn) LumiColor.Navy700 else LumiColor.Navy600,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcRect,
                style      = Stroke(strokeWidth, cap = StrokeCap.Round),
            )

            // ── Glow halo when ON ──────────────────────────────────────────
            if (isOn) {
                drawArc(
                    color      = LumiColor.Amber400.copy(alpha = glowAlpha * animatedFill),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animatedFill,
                    useCenter  = false,
                    topLeft    = Offset(-strokeWidth * 0.8f, -strokeWidth * 0.8f),
                    size       = Size(arcRect.width + strokeWidth * 1.6f, arcRect.height + strokeWidth * 1.6f),
                    style      = Stroke(strokeWidth * 2.8f, cap = StrokeCap.Round),
                )
            }

            // ── Fill arc ───────────────────────────────────────────────────
            val fillColor = when {
                isCharging            -> Color(0xFF4ADE80L)   // green when charging
                isOn                  -> LumiColor.Amber400
                batteryLevel < 0.15f  -> Color(0xFFFF4444L)   // red low battery
                batteryLevel < 0.30f  -> Color(0xFFFF9500L)   // orange
                else                  -> LumiColor.Navy500
            }
            if (animatedFill > 0.01f) {
                drawArc(
                    color      = fillColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animatedFill,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcRect,
                    style      = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
            }

            // ── Thumb indicator (drag handle) when ON ──────────────────────
            if (isOn) {
                val thumbAngleRad = Math.toRadians(
                    (startAngle + sweepAngle * animatedFill).toDouble()
                )
                val thumbX = arcCenter.x + arcRadius * cos(thumbAngleRad).toFloat()
                val thumbY = arcCenter.y + arcRadius * sin(thumbAngleRad).toFloat()
                drawCircle(
                    color  = LumiColor.Navy950,
                    radius = strokeWidth * 0.55f,
                    center = Offset(thumbX, thumbY),
                )
                drawCircle(
                    color  = if (isDragging) LumiColor.Amber300 else LumiColor.Amber400,
                    radius = strokeWidth * 0.42f,
                    center = Offset(thumbX, thumbY),
                )
            }

            // ── Battery level ticks (5 major, every 20%) ──────────────────
            if (!isOn) {
                val tickCount = 5
                repeat(tickCount + 1) { i ->
                    val pct = i.toFloat() / tickCount
                    val tickAngleRad = Math.toRadians(
                        (startAngle + sweepAngle * pct).toDouble()
                    )
                    val innerR = arcRadius - strokeWidth * 0.9f
                    val outerR = arcRadius + strokeWidth * 0.9f
                    val tx1 = arcCenter.x + innerR * cos(tickAngleRad).toFloat()
                    val ty1 = arcCenter.y + innerR * sin(tickAngleRad).toFloat()
                    val tx2 = arcCenter.x + outerR * cos(tickAngleRad).toFloat()
                    val ty2 = arcCenter.y + outerR * sin(tickAngleRad).toFloat()
                    drawLine(
                        color       = LumiColor.Navy800,
                        start       = Offset(tx1, ty1),
                        end         = Offset(tx2, ty2),
                        strokeWidth = 2f,
                        cap         = StrokeCap.Round,
                    )
                }
            }
        }

        // ── Center text ────────────────────────────────────────────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isOn) {
                Text(
                    text       = runtimeText,
                    fontSize   = 36.sp,
                    fontWeight = FontWeight.W700,
                    color      = LumiColor.Amber400,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text     = "remaining",
                    fontSize = 11.sp,
                    color    = LumiColor.Gray600,
                    fontWeight = FontWeight.W400,
                    letterSpacing = 0.06.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = "${(intensity * 100).toInt()}% intensity",
                    fontSize = 10.sp,
                    color    = LumiColor.Amber400.copy(.5f),
                    fontWeight = FontWeight.W500,
                )
            } else {
                Text(
                    text       = "${(batteryLevel * 100).toInt()}%",
                    fontSize   = 40.sp,
                    fontWeight = FontWeight.W700,
                    color      = when {
                        isCharging           -> Color(0xFF4ADE80L)
                        batteryLevel < 0.15f -> Color(0xFFFF4444L)
                        else                 -> LumiColor.Gray500
                    },
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text     = if (isCharging) "charging" else "battery",
                    fontSize = 11.sp,
                    color    = LumiColor.Gray600,
                    letterSpacing = 0.06.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = "select a mode to start",
                    fontSize = 10.sp,
                    color    = LumiColor.Gray700,
                    fontWeight = FontWeight.W400,
                )
            }
        }
    }
}

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
 * Power Arc Widget — main screen hero element.
 *
 * Layout (top to bottom):
 *   "MODO: CONTINUO"   ← mode label
 *   [Arc + center time]
 *   "Intensidad: 50%"  ← intensity label below arc
 *
 * OFF state: grey arc = battery level. Center = battery %.
 * ON state:  amber arc = intensity. Center = HH:MM + "H RESTANTES".
 *            Drag on arc → changes intensity live.
 */
@Composable
fun PowerArcWidget(
    isOn: Boolean,
    isScreenMode: Boolean,
    currentMode: FlashMode,
    batteryLevel: Float,
    isCharging: Boolean,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estimatedMinutes = remember(batteryLevel, currentMode, intensity) {
        EnergyEstimator.estimateMinutesRemaining(batteryLevel, currentMode, intensity)
    }

    // Clock-style format: "01:30" (hours:minutes) or "45m" for under 1h
    val clockText = remember(estimatedMinutes) {
        when {
            estimatedMinutes <= 0   -> "< 1m"
            estimatedMinutes < 60   -> "${estimatedMinutes}m"
            else -> {
                val h = estimatedMinutes / 60
                val m = estimatedMinutes % 60
                "%02d:%02d".format(h, m)
            }
        }
    }
    val timeUnit = remember(estimatedMinutes) {
        if (estimatedMinutes < 60) "MIN RESTANTES" else "H RESTANTES"
    }

    // Mode name label
    val modeName = remember(currentMode.id) {
        when (currentMode.id) {
            "steady"           -> "CONTINUO"
            "screen"           -> "PANTALLA"
            "morse_custom"     -> "MORSE"
            "strobe"           -> "ESTROBOSCOPIO"
            "sos"              -> "SOS"
            "disco"            -> "DISCO"
            "smart_brightness" -> "INTELIGENTE"
            "reading_mode"     -> "LECTURA"
            "ambient_smart"    -> "AMBIENTAL"
            "custom_rhythm"    -> "PERSONALIZADO"
            "sleep_timer"      -> "SUEÑO"
            "music"            -> "MÚSICA"
            "walk"             -> "CAMINAR"
            "voice"            -> "VOZ"
            else               -> "MODO"
        }
    }

    // Arc geometry
    val startAngle = 150f
    val sweepAngle = 240f
    val arcPadding = 16.dp    // smaller padding = larger arc relative to box

    val fillTarget = if (isOn) intensity else batteryLevel
    val animatedFill by animateFloatAsState(
        targetValue = fillTarget,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "arcFill",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "arcGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.65f,  // stronger glow pulse
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha",
    )

    var isDragging by remember { mutableStateOf(false) }
    var arcCenter by remember { mutableStateOf(Offset.Zero) }
    var arcRadius by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Mode label ─────────────────────────────────────────────────────
        Text(
            text = "MODO: $modeName",
            fontSize = 11.sp,
            fontWeight = FontWeight.W500,
            letterSpacing = 0.12.sp,
            color = LumiColor.Gray600,
        )

        Spacer(Modifier.height(4.dp))

        // ── Arc + center ────────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f),   // nearly square — arc fills more of the screen
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
                                var angle = Math.toDegrees(
                                    atan2(
                                        (pos.y - arcCenter.y).toDouble(),
                                        (pos.x - arcCenter.x).toDouble()
                                    )
                                ).toFloat()
                                if (angle < 0) angle += 360f
                                var normalized = angle - startAngle
                                if (normalized < 0) normalized += 360f
                                normalized = normalized.coerceIn(0f, sweepAngle)
                                onIntensityChange((normalized / sweepAngle).coerceIn(0.1f, 1.0f))
                            }
                        )
                    }
            ) {
                arcCenter = Offset(size.width / 2f, size.height / 2f)
                arcRadius = size.width / 2f

                val strokeWidth = 20.dp.toPx()   // thicker — matches mockup
                val arcRect    = Size(size.width, size.height)
                val topLeft    = Offset(0f, 0f)

                // Track — very dark, almost invisible like mockup
                drawArc(
                    color      = LumiColor.Navy800,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcRect,
                    style      = Stroke(strokeWidth, cap = StrokeCap.Round),
                )

                // Outer glow halo when ON
                if (isOn) {
                    drawArc(
                        color      = LumiColor.Amber400.copy(alpha = glowAlpha * animatedFill * 0.6f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * animatedFill,
                        useCenter  = false,
                        topLeft    = Offset(-strokeWidth, -strokeWidth),
                        size       = Size(arcRect.width + strokeWidth * 2f, arcRect.height + strokeWidth * 2f),
                        style      = Stroke(strokeWidth * 2.5f, cap = StrokeCap.Round),
                    )
                }

                // Fill arc
                val fillColor = when {
                    isCharging            -> Color(0xFF4ADE80L)
                    isOn                  -> LumiColor.Amber400
                    batteryLevel < 0.15f  -> Color(0xFFFF4444L)
                    batteryLevel < 0.30f  -> Color(0xFFFF9500L)
                    else                  -> LumiColor.Navy600
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

                // Thumb when ON — sized proportional to stroke
                if (isOn) {
                    val thumbAngleRad = Math.toRadians(
                        (startAngle + sweepAngle * animatedFill).toDouble()
                    )
                    val tx = arcCenter.x + arcRadius * cos(thumbAngleRad).toFloat()
                    val ty = arcCenter.y + arcRadius * sin(thumbAngleRad).toFloat()
                    drawCircle(color = LumiColor.Navy950, radius = strokeWidth * 0.58f, center = Offset(tx, ty))
                    drawCircle(
                        color  = if (isDragging) LumiColor.Amber300 else LumiColor.Amber400,
                        radius = strokeWidth * 0.40f,
                        center = Offset(tx, ty),
                    )
                }

                // Battery ticks when OFF — slightly more visible
                if (!isOn) {
                    repeat(6) { i ->
                        val pct = i.toFloat() / 5f
                        val tickRad = Math.toRadians((startAngle + sweepAngle * pct).toDouble())
                        val inner = arcRadius - strokeWidth * 0.85f
                        val outer = arcRadius + strokeWidth * 0.85f
                        drawLine(
                            color       = LumiColor.Navy700,
                            start       = Offset(arcCenter.x + inner * cos(tickRad).toFloat(), arcCenter.y + inner * sin(tickRad).toFloat()),
                            end         = Offset(arcCenter.x + outer * cos(tickRad).toFloat(), arcCenter.y + outer * sin(tickRad).toFloat()),
                            strokeWidth = 2.5f,
                            cap         = StrokeCap.Round,
                        )
                    }
                }
            }

            // ── Center text ────────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isOn) {
                    Text(
                        text          = clockText,
                        fontSize      = 48.sp,
                        fontWeight    = FontWeight.W700,
                        color         = LumiColor.White,
                        letterSpacing = (-1).sp,
                    )
                    Text(
                        text          = timeUnit,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.W600,
                        letterSpacing = 0.10.sp,
                        color         = LumiColor.Amber400,
                    )
                } else {
                    Text(
                        text       = "${(batteryLevel * 100).toInt()}%",
                        fontSize   = 44.sp,
                        fontWeight = FontWeight.W700,
                        color      = when {
                            isCharging           -> Color(0xFF4ADE80L)
                            batteryLevel < 0.15f -> Color(0xFFFF4444L)
                            else                 -> LumiColor.Gray400
                        },
                        letterSpacing = (-0.5).sp,
                    )
                    Text(
                        text     = if (isCharging) "CARGANDO" else "BATERÍA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W600,
                        letterSpacing = 0.10.sp,
                        color    = LumiColor.Gray600,
                    )
                }
            }
        }

        // ── Intensity label below arc ───────────────────────────────────────
        Text(
            text = if (isOn)
                "Intensidad: ${(intensity * 100).toInt()}%"
            else
                "Selecciona un modo para empezar",
            fontSize   = 12.sp,
            fontWeight = FontWeight.W400,
            color      = if (isOn) LumiColor.Gray500 else LumiColor.Gray700,
            letterSpacing = 0.04.sp,
        )

        Spacer(Modifier.height(4.dp))
    }
}

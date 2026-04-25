package com.lumiai.flashlight.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumiai.flashlight.ui.theme.LumiColor
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FlashButton(
    isOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 172.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            isOn      -> 1.04f
            else      -> 1.00f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
        label = "btn_scale",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "glow")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue  = 0.55f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_radius",
    )

    val ringInfinite = rememberInfiniteTransition(label = "ring")
    val ringRotation by ringInfinite.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
        ),
        label = "ring_rotation",
    )

    val centerColor by animateColorAsState(
        if (isOn) LumiColor.Amber400 else LumiColor.Navy700, tween(300), label = "center"
    )
    val ringColor by animateColorAsState(
        if (isOn) LumiColor.Amber600 else LumiColor.Navy600, tween(300), label = "ring"
    )
    val iconTint by animateColorAsState(
        if (isOn) LumiColor.Navy900 else LumiColor.Gray400, tween(250), label = "icon_tint"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).scale(scale),
    ) {
        // Outer glow rings — only when ON
        if (isOn) {
            Canvas(modifier = Modifier.size(size * 2.2f)) {
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val baseR = size.toPx() / 2f

                drawCircle(
                    color  = LumiColor.Amber400.copy(alpha = glowAlpha * 0.5f),
                    radius = baseR * glowRadius * 1.15f,
                    center = Offset(cx, cy),
                    style  = Stroke(width = 1.5f),
                )
                drawCircle(
                    color  = LumiColor.Amber400.copy(alpha = glowAlpha * 0.25f),
                    radius = baseR * glowRadius * 1.35f,
                    center = Offset(cx, cy),
                    style  = Stroke(width = 1f),
                )
                drawCircle(
                    brush  = Brush.radialGradient(
                        colors = listOf(
                            LumiColor.Amber400.copy(alpha = glowAlpha * 0.12f),
                            Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = baseR * glowRadius * 1.6f,
                    ),
                    radius = baseR * glowRadius * 1.6f,
                    center = Offset(cx, cy),
                )
            }
        }

        // Rotating precision notch ring
        Canvas(modifier = Modifier.size(size + 24.dp)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r  = size.toPx() / 2f + 8.dp.toPx()
            val notchCount = 24
            val notchLen   = if (isOn) 6.dp.toPx() else 4.dp.toPx()
            val notchAlpha = if (isOn) 0.6f else 0.2f
            val rotation   = if (isOn) ringRotation else 0f

            repeat(notchCount) { i ->
                val angle = Math.toRadians((i * (360f / notchCount) + rotation).toDouble())
                val major = i % 6 == 0
                val len = if (major) notchLen * 1.5f else notchLen
                val x1 = cx + (r - len / 2) * cos(angle).toFloat()
                val y1 = cy + (r - len / 2) * sin(angle).toFloat()
                val x2 = cx + (r + len / 2) * cos(angle).toFloat()
                val y2 = cy + (r + len / 2) * sin(angle).toFloat()
                drawLine(
                    color       = (if (isOn) LumiColor.Amber400 else LumiColor.Navy500)
                                      .copy(alpha = if (major) notchAlpha * 1.5f else notchAlpha),
                    start       = Offset(x1, y1),
                    end         = Offset(x2, y2),
                    strokeWidth = if (major) 2f else 1f,
                    cap         = StrokeCap.Round,
                )
            }
            drawCircle(
                color  = ringColor.copy(alpha = 0.35f),
                radius = r,
                center = Offset(cx, cy),
                style  = Stroke(width = 0.5f),
            )
        }

        // Main circle button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    if (isOn)
                        Brush.radialGradient(
                            colors = listOf(LumiColor.Amber300, LumiColor.Amber600),
                            radius = size.value * 2.5f,
                        )
                    else
                        Brush.radialGradient(
                            colors = listOf(LumiColor.Navy700, LumiColor.Navy850),
                            radius = size.value * 2f,
                        )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication        = ripple(
                        color  = if (isOn) LumiColor.Navy900.copy(alpha = 0.2f)
                                 else LumiColor.Amber400.copy(alpha = 0.15f),
                        radius = size / 2,
                    ),
                    onClick = onClick,
                )
                .semantics {
                    contentDescription = if (isOn) "Apagar linterna" else "Encender linterna"
                    role = Role.Button
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val r  = this.size.width / 2f - 3f
                drawCircle(
                    color  = Color.Black.copy(alpha = if (isOn) 0.15f else 0.35f),
                    radius = r,
                    center = Offset(cx, cy),
                    style  = Stroke(width = 8f),
                )
            }
            Icon(
                imageVector        = if (isOn) LumiIcons.FlashOn else LumiIcons.FlashOff,
                contentDescription = null,
                tint               = iconTint,
                modifier           = Modifier.size(size * 0.42f),
            )
        }
    }
}

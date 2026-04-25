package com.lumiai.flashlight.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp

/**
 * Custom icon set for LumiAI — consistent 2px stroke, rounded caps.
 * Using PathBuilder DSL instead of internal addPathNodes.
 */
object LumiIcons {

    val FlashOn: ImageVector
        get() = ImageVector.Builder(
            name = "FlashOn",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFD84A)),
            ) {
                moveTo(13f, 2f)
                lineTo(7f, 13f)
                horizontalLineTo(12f)
                lineTo(11f, 22f)
                lineTo(17f, 11f)
                horizontalLineTo(13f)
                lineTo(13f, 2f)
                close()
            }
        }.build()

    val FlashOff: ImageVector
        get() = ImageVector.Builder(
            name = "FlashOff",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF52526A))) {
                moveTo(13f, 2f)
                lineTo(7f, 13f)
                horizontalLineTo(12f)
                lineTo(11f, 22f)
                lineTo(17f, 11f)
                horizontalLineTo(13f)
                lineTo(13f, 2f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFFF87171)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(3f, 3f)
                lineTo(21f, 21f)
            }
        }.build()

    val Settings: ImageVector
        get() = ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            // Gear circle
            path(
                stroke = SolidColor(Color(0xFF9CA3AF)),
                strokeLineWidth = 1.5f,
                fill = SolidColor(Color.Transparent),
            ) {
                moveTo(12f, 15f)
                curveTo(13.657f, 15f, 15f, 13.657f, 15f, 12f)
                curveTo(15f, 10.343f, 13.657f, 9f, 12f, 9f)
                curveTo(10.343f, 9f, 9f, 10.343f, 9f, 12f)
                curveTo(9f, 13.657f, 10.343f, 15f, 12f, 15f)
                close()
            }
            // Spokes
            path(
                stroke = SolidColor(Color(0xFF9CA3AF)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(12f, 2f); verticalLineTo(4f)
                moveTo(12f, 20f); verticalLineTo(22f)
                moveTo(4.93f, 4.93f); lineTo(6.34f, 6.34f)
                moveTo(17.66f, 17.66f); lineTo(19.07f, 19.07f)
                moveTo(2f, 12f); horizontalLineTo(4f)
                moveTo(20f, 12f); horizontalLineTo(22f)
                moveTo(4.93f, 19.07f); lineTo(6.34f, 17.66f)
                moveTo(17.66f, 6.34f); lineTo(19.07f, 4.93f)
            }
        }.build()

    val Star: ImageVector
        get() = ImageVector.Builder(
            name = "Star",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color(0xFFA78BFA)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent),
            ) {
                moveTo(12f, 2f)
                lineTo(15.09f, 8.26f)
                lineTo(22f, 9.27f)
                lineTo(17f, 14.14f)
                lineTo(18.18f, 21.02f)
                lineTo(12f, 17.77f)
                lineTo(5.82f, 21.02f)
                lineTo(7f, 14.14f)
                lineTo(2f, 9.27f)
                lineTo(8.91f, 8.26f)
                lineTo(12f, 2f)
                close()
            }
        }.build()
}

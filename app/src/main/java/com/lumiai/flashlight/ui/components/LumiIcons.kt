package com.lumiai.flashlight.ui.components

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp

/**
 * Custom icon set for LumiAI — all hand-crafted SVG paths.
 * No emoji, no random icon libs — consistent 2px stroke, rounded caps.
 */
object LumiIcons {

    val FlashOn: ImageVector = ImageVector.Builder(
        name = "FlashOn", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        // Flashlight body
        addPath(
            pathData = addPathNodes("M8 2 L16 2 L14 10 L18 10 L10 22 L12 14 L8 14 Z"),
            fill = SolidColor(Color.Unspecified),
            stroke = SolidColor(Color.Unspecified),
        )
        // Use filled bolt for ON state
        addPath(
            pathData = addPathNodes("M13 2L7 13H12L11 22L17 11H13L13 2Z"),
            fill = SolidColor(Color(0xFFFFD84A)),
        )
    }.build()

    val FlashOff: ImageVector = ImageVector.Builder(
        name = "FlashOff", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes("M13 2L7 13H12L11 22L17 11H13L13 2Z"),
            fill = SolidColor(Color(0xFF52526A)),
        )
        // Strike-through line
        addPath(
            pathData = addPathNodes("M3 3L21 21"),
            stroke = SolidColor(Color(0xFFF87171)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
        )
    }.build()

    val Settings: ImageVector = ImageVector.Builder(
        name = "Settings", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        // Gear outline — simplified
        addPath(
            pathData = addPathNodes(
                "M12 15C13.6569 15 15 13.6569 15 12C15 10.3431 13.6569 9 12 9C10.3431 9 9 10.3431 9 12C9 13.6569 10.3431 15 12 15Z"
            ),
            stroke = SolidColor(Color(0xFF9CA3AF)),
            strokeLineWidth = 1.5f,
            fill = SolidColor(Color.Transparent),
        )
        addPath(
            pathData = addPathNodes(
                "M12 2V4M12 20V22M4.93 4.93L6.34 6.34M17.66 17.66L19.07 19.07M2 12H4M20 12H22M4.93 19.07L6.34 17.66M17.66 6.34L19.07 4.93"
            ),
            stroke = SolidColor(Color(0xFF9CA3AF)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
        )
    }.build()

    val Star: ImageVector = ImageVector.Builder(
        name = "Star", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes(
                "M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"
            ),
            stroke = SolidColor(Color(0xFFA78BFA)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = SolidColor(Color.Transparent),
        )
    }.build()

    val Sos: ImageVector = ImageVector.Builder(
        name = "SOS", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        // Signal waves
        addPath(
            pathData = addPathNodes("M5.64 5.64C3.23 8.05 3.23 11.95 5.64 14.36"),
            stroke = SolidColor(Color(0xFFF87171)),
            strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round,
        )
        addPath(
            pathData = addPathNodes("M18.36 5.64C20.77 8.05 20.77 11.95 18.36 14.36"),
            stroke = SolidColor(Color(0xFFF87171)),
            strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round,
        )
        addPath(
            pathData = addPathNodes("M8.46 8.46C7.18 9.74 7.18 11.26 8.46 12.54"),
            stroke = SolidColor(Color(0xFFF87171)),
            strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round,
        )
        addPath(
            pathData = addPathNodes("M15.54 8.46C16.82 9.74 16.82 11.26 15.54 12.54"),
            stroke = SolidColor(Color(0xFFF87171)),
            strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round,
        )
        addPath(
            pathData = addPathNodes("M12 13C12.5523 13 13 12.5523 13 12C13 11.4477 12.5523 11 12 11C11.4477 11 11 11.4477 11 12C11 12.5523 11.4477 13 12 13Z"),
            fill = SolidColor(Color(0xFFF87171)),
        )
        // SOS text indicator dot row
        addPath(
            pathData = addPathNodes("M7 18H9M11 18H13M15 18H17"),
            stroke = SolidColor(Color(0xFFF87171)),
            strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round,
        )
    }.build()
}

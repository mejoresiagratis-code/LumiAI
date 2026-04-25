package com.lumiai.flashlight.feature.flash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.core.domain.model.ProStatus
import com.lumiai.flashlight.ui.theme.Amber400
import com.lumiai.flashlight.ui.theme.Navy700
import com.lumiai.flashlight.ui.theme.Purple400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashScreen(
    viewModel: FlashViewModel,
    onOpenSettings: () -> Unit,
    onOpenPro: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPro = uiState.proStatus == ProStatus.Pro

    // Screen-mode: full white background when Screen mode is active
    val bgColor = if (uiState.currentMode == FlashMode.Screen && uiState.isFlashOn)
        Color.White else MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animateColorAsState(bgColor, label = "bg").value)
    ) {
        // ── Top bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "LumiAI",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (bgColor.luminance() > 0.5f) Color.Black else Amber400,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isPro) {
                    FilledTonalButton(
                        onClick = onOpenPro,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Purple400.copy(alpha = 0.15f)),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(Icons.Outlined.Star, contentDescription = null, tint = Purple400, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pro", color = Purple400, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings",
                        tint = if (bgColor.luminance() > 0.5f) Color.Black else Color.White)
                }
            }
        }

        // ── Main flash button ──────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            FlashButton(
                isOn     = uiState.isFlashOn,
                onClick  = { viewModel.toggleFlash() },
            )

            Spacer(Modifier.height(48.dp))

            // ── Mode selector ──────────────────────────────────────────────
            ModeSelector(
                currentMode  = uiState.currentMode,
                isPro        = isPro,
                onModeSelect = { mode ->
                    if (mode.isPro && !isPro) onOpenPro()
                    else viewModel.activateMode(mode)
                },
            )
        }

        // ── Bottom safe area spacer ────────────────────────────────────────
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun FlashButton(isOn: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isOn) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn_scale",
    )
    val containerColor = if (isOn) Amber400 else MaterialTheme.colorScheme.surfaceVariant
    val iconColor = if (isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .scale(scale)
            .size(160.dp)
            .clip(CircleShape)
            .background(animateColorAsState(containerColor, label = "btn_color").value)
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (isOn) "Apagar linterna" else "Encender linterna" },
    ) {
        // Flashlight icon (simple SVG-style using Canvas or Icon)
        Icon(
            imageVector  = if (isOn) rememberFlashOnIcon() else rememberFlashOffIcon(),
            contentDescription = null,
            tint         = animateColorAsState(iconColor, label = "icon_color").value,
            modifier     = Modifier.size(72.dp),
        )
    }
}

@Composable
private fun ModeSelector(
    currentMode: FlashMode,
    isPro: Boolean,
    onModeSelect: (FlashMode) -> Unit,
) {
    val freeModes  = FlashMode.freeModes()
    val proModes   = FlashMode.proModes()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        // Free modes row
        ModeChipRow(
            modes       = freeModes,
            currentMode = currentMode,
            isPro       = true,  // always unlocked
            onSelect    = onModeSelect,
        )
        Spacer(Modifier.height(12.dp))
        // Pro modes row
        ModeChipRow(
            modes       = proModes,
            currentMode = currentMode,
            isPro       = isPro,
            onSelect    = onModeSelect,
            isProRow    = true,
        )
    }
}

@Composable
private fun ModeChipRow(
    modes: List<FlashMode>,
    currentMode: FlashMode,
    isPro: Boolean,
    onSelect: (FlashMode) -> Unit,
    isProRow: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        modes.forEach { mode ->
            val isSelected = mode.id == currentMode.id
            val locked     = isProRow && !isPro
            FilterChip(
                selected  = isSelected,
                onClick   = { onSelect(mode) },
                label     = {
                    Text(
                        text = modeName(mode) + if (locked) " 🔒" else "",
                        fontSize = 12.sp,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (isProRow) Purple400 else Amber400,
                    selectedLabelColor     = Color.White,
                ),
            )
        }
    }
}

private fun modeName(mode: FlashMode): String = when (mode) {
    is FlashMode.Steady          -> "Steady"
    is FlashMode.Screen          -> "Screen"
    is FlashMode.Sos             -> "SOS"
    is FlashMode.Strobe          -> "Strobe"
    is FlashMode.Disco           -> "Disco"
    is FlashMode.SmartBrightness -> "Smart"
    is FlashMode.ReadingMode     -> "Read"
    is FlashMode.AmbientSmart    -> "Ambient"
    is FlashMode.CustomRhythm    -> "Custom"
    is FlashMode.SleepTimer      -> "Sleep"
    else                         -> mode.id
}

// Placeholder icon composables — replace with proper vector assets
@Composable private fun rememberFlashOnIcon()  = Icons.Default.Settings  // TODO: real icon
@Composable private fun rememberFlashOffIcon() = Icons.Default.Settings  // TODO: real icon

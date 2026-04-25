package com.lumiai.flashlight.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumiai.flashlight.core.domain.model.ProStatus
import com.lumiai.flashlight.feature.flash.FlashViewModel
import com.lumiai.flashlight.ui.components.LumiIcons
import com.lumiai.flashlight.ui.theme.LumiColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPro: () -> Unit,
    viewModel: FlashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPro = uiState.proStatus == ProStatus.Pro

    Scaffold(
        containerColor = LumiColor.Navy950,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.W600,
                        fontSize = 18.sp,
                        color = LumiColor.White,
                    )
                },
                navigationIcon = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(LumiColor.Navy700)
                            .clickable(onClick = onBack),
                    ) {
                        // Back arrow via path
                        Icon(
                            imageVector = backArrowIcon(),
                            contentDescription = "Atrás",
                            tint = LumiColor.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LumiColor.Navy950,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            // ── FLASH ──────────────────────────────────────────────────────
            SettingsSection(title = "FLASH") {
                SettingsSliderRow(
                    label    = "Strobe frequency",
                    sublabel = "Hz",
                    value    = 5f,
                    range    = 0.5f..20f,
                    displayValue = { "${it.toInt()} Hz" },
                    onChange = { },
                )
                SettingsDivider()
                SettingsSliderRow(
                    label    = "Disco tempo",
                    sublabel = "BPM",
                    value    = 120f,
                    range    = 60f..200f,
                    displayValue = { "${it.toInt()} BPM" },
                    onChange = { },
                )
                SettingsDivider()
                SettingsSliderRow(
                    label    = "Screen brightness",
                    sublabel = "%",
                    value    = 1f,
                    range    = 0.1f..1f,
                    displayValue = { "${(it * 100).toInt()}%" },
                    onChange = { },
                )
            }

            // ── BEHAVIOUR ─────────────────────────────────────────────────
            SettingsSection(title = "BEHAVIOUR") {
                SettingsToggleRow(
                    label    = "Shake to toggle",
                    sublabel = "Shake phone to turn flash on/off",
                    checked  = true,
                    onChange = { },
                )
                SettingsDivider()
                SettingsToggleRow(
                    label    = "Keep screen on",
                    sublabel = "Prevent screen sleep while app is open",
                    checked  = true,
                    onChange = { },
                )
                SettingsDivider()
                SettingsToggleRow(
                    label    = "Dark theme",
                    sublabel = "Force dark UI regardless of system",
                    checked  = true,
                    onChange = { },
                )
            }

            // ── PRO ───────────────────────────────────────────────────────
            SettingsSection(title = "PRO") {
                if (isPro) {
                    SettingsInfoRow(
                        label    = "LumiAI Pro",
                        sublabel = "All AI features unlocked",
                        badge    = "ACTIVE",
                        badgeColor = LumiColor.Success,
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        label  = "Restore purchase",
                        sublabel = "Re-link existing purchase to this device",
                        onClick = { },
                    )
                } else {
                    SettingsProBanner(onOpenPro = onOpenPro)
                }
            }

            // ── ABOUT ─────────────────────────────────────────────────────
            SettingsSection(title = "ABOUT") {
                SettingsInfoRow(
                    label    = "Version",
                    sublabel = "LumiAI Flashlight",
                    badge    = "1.0.0",
                    badgeColor = LumiColor.Gray500,
                )
                SettingsDivider()
                SettingsActionRow(
                    label    = "Privacy Policy",
                    sublabel = "How we handle your data",
                    onClick  = { },
                )
                SettingsDivider()
                SettingsActionRow(
                    label    = "Rate on Play Store",
                    sublabel = "Help us grow with a review ⭐",
                    onClick  = { },
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Section container ─────────────────────────────────────────────────────────
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.W600,
            letterSpacing = 0.12.sp,
            color = LumiColor.Gray500,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LumiColor.Navy800),
                content = content,
            )
        }
    }
}

// ── Row types ─────────────────────────────────────────────────────────────────
@Composable
private fun SettingsToggleRow(
    label: String,
    sublabel: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.W500, color = LumiColor.White)
            Text(sublabel, fontSize = 12.sp, color = LumiColor.Gray500, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = LumiColor.Navy900,
                checkedTrackColor  = LumiColor.Amber400,
                uncheckedThumbColor = LumiColor.Gray400,
                uncheckedTrackColor = LumiColor.Navy600,
            ),
        )
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    sublabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    var current by remember { mutableFloatStateOf(value) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.W500, color = LumiColor.White)
            Text(
                displayValue(current),
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                color = LumiColor.Amber400,
            )
        }
        Slider(
            value = current,
            onValueChange = { current = it; onChange(it) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor        = LumiColor.Amber400,
                activeTrackColor  = LumiColor.Amber400,
                inactiveTrackColor = LumiColor.Navy600,
            ),
        )
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    sublabel: String,
    badge: String,
    badgeColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.W500, color = LumiColor.White)
            Text(sublabel, fontSize = 12.sp, color = LumiColor.Gray500, modifier = Modifier.padding(top = 2.dp))
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(badgeColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(badge, fontSize = 11.sp, fontWeight = FontWeight.W700, color = badgeColor)
        }
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    sublabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.W500, color = LumiColor.White)
            Text(sublabel, fontSize = 12.sp, color = LumiColor.Gray500, modifier = Modifier.padding(top = 2.dp))
        }
        Text("›", fontSize = 20.sp, color = LumiColor.Gray600)
    }
}

@Composable
private fun SettingsProBanner(onOpenPro: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPro)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Unlock Pro",
                fontSize = 14.sp, fontWeight = FontWeight.W600,
                color = LumiColor.Purple300,
            )
            Text(
                "AI features · No ads · One-time payment",
                fontSize = 12.sp, color = LumiColor.Gray500,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(LumiColor.Purple500)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text("View", fontSize = 12.sp, fontWeight = FontWeight.W600, color = LumiColor.White)
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = LumiColor.Navy700,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

// Simple back arrow icon
@Composable
private fun backArrowIcon(): ImageVector = androidx.compose.ui.graphics.vector.ImageVector.Builder(
    name = "Back", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f,
).apply {
    path(
        stroke = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
        strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
    ) {
        moveTo(19f, 12f); horizontalLineTo(5f)
        moveTo(12f, 19f); lineTo(5f, 12f); lineTo(12f, 5f)
    }
}.build()

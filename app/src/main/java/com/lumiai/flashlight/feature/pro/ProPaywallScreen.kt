package com.lumiai.flashlight.feature.pro

import androidx.compose.animation.*
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumiai.flashlight.core.domain.model.ProStatus
import com.lumiai.flashlight.feature.flash.FlashViewModel
import com.lumiai.flashlight.ui.theme.LumiColor

@Composable
fun ProPaywallScreen(
    onBack: () -> Unit,
    viewModel: FlashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPro = uiState.proStatus == ProStatus.Pro
    val context = LocalContext.current

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LumiColor.Navy950),
    ) {
        // Purple ambient top glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            LumiColor.Purple500.copy(alpha = 0.12f),
                            Color.Transparent,
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── Top bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LumiColor.Navy700)
                        .clickable(onClick = onBack),
                ) {
                    Text("×", fontSize = 20.sp, color = LumiColor.White, fontWeight = FontWeight.W300)
                }
                Text(
                    "LumiAI Pro",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    color = LumiColor.White,
                )
                Spacer(Modifier.size(40.dp))
            }

            Spacer(Modifier.height(8.dp))

            // ── Hero icon ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + scaleIn(tween(400, easing = FastOutSlowInEasing)),
            ) {
                ProHeroIcon()
            }

            Spacer(Modifier.height(24.dp))

            // ── Headline ───────────────────────────────────────────────────
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(500, delayMillis = 100))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Upgrade once.\nUnlock forever.",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.W700,
                        color = LumiColor.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "No subscription. No expiry. Pay once.",
                        fontSize = 14.sp,
                        color = LumiColor.Gray500,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Feature list ───────────────────────────────────────────────
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(600, delayMillis = 200))) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProFeatureRow(
                        emoji = "✦",
                        title = "No ads, ever",
                        desc  = "Clean experience with zero interruptions",
                        color = LumiColor.Amber400,
                    )
                    ProFeatureRow(
                        emoji = "⬡",
                        title = "Smart brightness",
                        desc  = "Gemini Nano adjusts intensity to your environment",
                        color = LumiColor.Purple400,
                    )
                    ProFeatureRow(
                        emoji = "◎",
                        title = "Reading mode",
                        desc  = "Warm AI-tuned light optimised for your eyes",
                        color = LumiColor.Purple400,
                    )
                    ProFeatureRow(
                        emoji = "◈",
                        title = "Ambient detection",
                        desc  = "Camera scene analysis picks the perfect mode",
                        color = LumiColor.Purple400,
                    )
                    ProFeatureRow(
                        emoji = "◉",
                        title = "Custom AI rhythms",
                        desc  = "Generate personalised strobe patterns with AI",
                        color = LumiColor.Purple400,
                    )
                    ProFeatureRow(
                        emoji = "◌",
                        title = "Sleep timer",
                        desc  = "Gradual AI-controlled fade-out for bedtime",
                        color = LumiColor.Purple400,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── CTA ────────────────────────────────────────────────────────
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(700, delayMillis = 300))) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (isPro) {
                        // Already Pro state
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(LumiColor.Success.copy(alpha = 0.15f))
                                .border(1.dp, LumiColor.Success.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        ) {
                            Text(
                                "✓  Pro is active",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W600,
                                color = LumiColor.Success,
                            )
                        }
                    } else {
                        // Purchase button
                        PurchaseButton(
                            onClick = {
                                (context as? android.app.Activity)?.let {
                                    viewModel.purchasePro(it)
                                }
                            }
                        )

                        // Restore
                        TextButton(onClick = { /* TODO restore */ }) {
                            Text(
                                "Restore purchase",
                                fontSize = 13.sp,
                                color = LumiColor.Gray500,
                            )
                        }
                    }

                    // Trust badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TrustBadge("One-time")
                        TrustBadge("No account")
                        TrustBadge("Offline AI")
                    }
                }
            }

            Spacer(Modifier.height(32.dp).navigationBarsPadding())
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun ProHeroIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(120.dp).scale(pulse),
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LumiColor.Purple500.copy(alpha = 0.3f),
                            LumiColor.Purple600.copy(alpha = 0.1f),
                            Color.Transparent,
                        )
                    )
                )
        )
        // Inner circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(LumiColor.Purple400, LumiColor.Purple600)
                    )
                ),
        ) {
            Text("✦", fontSize = 32.sp, color = LumiColor.White)
        }
    }
}

@Composable
private fun ProFeatureRow(emoji: String, title: String, desc: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LumiColor.Navy800)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
        ) {
            Text(emoji, fontSize = 16.sp, color = color)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.W600, color = LumiColor.White)
            Text(desc, fontSize = 12.sp, color = LumiColor.Gray500, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

@Composable
private fun PurchaseButton(onClick: () -> Unit) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cta_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(LumiColor.Purple400, LumiColor.Purple600)
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("✦", fontSize = 14.sp, color = LumiColor.White)
            Text(
                "Unlock Pro — €2.99",
                fontSize = 16.sp,
                fontWeight = FontWeight.W700,
                color = LumiColor.White,
                letterSpacing = (-0.2).sp,
            )
        }
    }
}

@Composable
private fun TrustBadge(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(LumiColor.Gray600)
        )
        Text(text, fontSize = 11.sp, color = LumiColor.Gray600)
    }
}


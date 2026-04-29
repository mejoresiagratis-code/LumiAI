package com.lumiai.flashlight.feature.pro

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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

    LaunchedEffect(isPro) { if (isPro) onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LumiColor.Navy950)
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Top bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(LumiColor.Navy800)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack,
                        ),
                    contentAlignment = Alignment.Center,
                ) { Text("✕", fontSize = 14.sp, color = LumiColor.Gray400) }
                Text("LumiAI Pro", fontSize = 15.sp, fontWeight = FontWeight.W600,
                    color = LumiColor.White, letterSpacing = 0.02.sp)
                Spacer(Modifier.size(32.dp))
            }

            // ── Icon + headline ────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(LumiColor.Purple500.copy(0.9f)),
                contentAlignment = Alignment.Center,
            ) { Text("✦", fontSize = 22.sp, color = Color.White) }
            Spacer(Modifier.height(10.dp))
            Text("Pago único.\nTuyo para siempre.",
                fontSize = 22.sp, fontWeight = FontWeight.W700,
                color = LumiColor.White, textAlign = TextAlign.Center, lineHeight = 28.sp)
            Text("Sin suscripción · Sin caducidad · Pago único",
                fontSize = 11.sp, color = LumiColor.Gray500,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))

            // ── Feature list ───────────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            val features = listOf(
                Triple("✦", "Sin anuncios", "Experiencia limpia sin interrupciones"),
                Triple("◎", "Brillo inteligente", "Gemini Nano adapta la intensidad al entorno"),
                Triple("◑", "Modo lectura", "Luz cálida ajustada para tus ojos"),
                Triple("⬨", "Detección ambiental", "La cámara analiza la escena y elige el modo"),
                Triple("⬡", "Ritmos personalizados", "Patrones estroboscópicos adaptativos"),
                Triple("◌", "Temporizador de sueño", "Fade-out gradual controlado por IA"),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LumiColor.Navy800)
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                features.forEach { (icon, title, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(LumiColor.Purple500.copy(0.15f)),
                            contentAlignment = Alignment.Center,
                        ) { Text(icon, fontSize = 13.sp, color = LumiColor.Purple300) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontSize = 12.sp, fontWeight = FontWeight.W600,
                                color = LumiColor.White)
                            Text(desc, fontSize = 10.sp, color = LumiColor.Gray500,
                                lineHeight = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── CTA button ─────────────────────────────────────────────────
            val btnSource = remember { MutableInteractionSource() }
            val isPressed by btnSource.collectIsPressedAsState()
            val btnScale by animateFloatAsState(if (isPressed) 0.96f else 1f,
                animationSpec = spring(stiffness = 400f), label = "btn")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(btnScale)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LumiColor.Purple500)
                    .clickable(
                        interactionSource = btnSource,
                        indication = null,
                        onClick = {
                            val activity = context as? android.app.Activity
                            if (activity != null) {
                                viewModel.purchasePro(activity)
                            }
                        },
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("✦  Desbloquear Pro — €2,99",
                    fontSize = 16.sp, fontWeight = FontWeight.W700,
                    color = Color.White, letterSpacing = 0.02.sp)
            }

            // ── Footer ─────────────────────────────────────────────────────
            Text("Restaurar compra",
                fontSize = 12.sp, color = LumiColor.Gray600,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.restorePurchases() },
                    ))
            Row(
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf("Pago único", "Sin cuenta", "IA offline").forEach { label ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(Modifier.size(4.dp).clip(CircleShape).background(LumiColor.Gray600))
                        Text(label, fontSize = 10.sp, color = LumiColor.Gray600)
                    }
                }
            }
        }
    }
}

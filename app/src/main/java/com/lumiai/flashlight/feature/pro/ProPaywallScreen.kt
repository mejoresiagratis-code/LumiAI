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
import androidx.compose.material3.LinearProgressIndicator
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
import com.lumiai.flashlight.BuildConfig
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.core.domain.model.ProStatus
import com.lumiai.flashlight.core.domain.model.isProActive
import com.lumiai.flashlight.feature.flash.DevProMode
import com.lumiai.flashlight.feature.flash.FlashViewModel
import com.lumiai.flashlight.ui.theme.LumiColor

@Composable
fun ProPaywallScreen(
    onBack: () -> Unit,
    viewModel: FlashViewModel = hiltViewModel(),
) {
    val uiState        by viewModel.uiState.collectAsState()
    val rewardedState  by viewModel.rewardedState.collectAsState()
    val adLoading      by viewModel.rewardedAdLoading.collectAsState()
    val context        = LocalContext.current

    val isPro          = uiState.proStatus.isProActive
    val isRewardedActive = uiState.proStatus is ProStatus.ProRewarded && isPro

    // Logo tap counter for dev mode (7 taps, debug only)
    var logoTapCount by remember { mutableIntStateOf(0) }

    // Auto-dismiss when Pro becomes active (permanent purchase or rewarded granted)
    LaunchedEffect(isPro) { if (isPro && !isRewardedActive) onBack() }

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
                // Dev mode badge (debug only)
                if (BuildConfig.IS_DEBUG && uiState.devMode != DevProMode.NONE) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LumiColor.Amber400.copy(0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = when (uiState.devMode) {
                                DevProMode.FREE_OVERRIDE     -> "DEV:FREE"
                                DevProMode.REWARDED_OVERRIDE -> "DEV:REWARDED"
                                DevProMode.PRO_OVERRIDE      -> "DEV:PRO"
                                else -> ""
                            },
                            fontSize = 10.sp, color = LumiColor.Amber400,
                            fontWeight = FontWeight.W700,
                        )
                    }
                } else {
                    Spacer(Modifier.size(32.dp))
                }
            }

            // ── Rewarded active banner ─────────────────────────────────────
            if (isRewardedActive) {
                val remaining = (uiState.proStatus as ProStatus.ProRewarded).let {
                    ((it.expiresAt - System.currentTimeMillis()) / 60_000L).toInt().coerceAtLeast(0)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LumiColor.Green500.copy(0.15f))
                        .border(1.dp, LumiColor.Green500.copy(0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Column {
                        Text("✓ Pro activo — $remaining min restantes",
                            fontSize = 13.sp, fontWeight = FontWeight.W600,
                            color = LumiColor.Green400)
                        Text("Vuelve aquí cuando expire para renovar viendo más anuncios.",
                            fontSize = 11.sp, color = LumiColor.Gray500,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── Icon + headline ────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(LumiColor.Purple500.copy(0.9f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (!BuildConfig.IS_DEBUG) return@clickable
                            logoTapCount++
                            if (logoTapCount >= 7) {
                                logoTapCount = 0
                                viewModel.cycleDevMode()
                            }
                        },
                    ),
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
            // Synced with the AI Modes grid: derived from the same visible Pro
            // modes (hidden=false in release, all in debug) so the paywall can
            // never advertise a mode the user can't actually see. "Sin anuncios"
            // is always first since ad-removal is part of Pro regardless of modes.
            Spacer(Modifier.height(12.dp))
            val features = remember(uiState.proStatus) {
                buildList {
                    add(Triple("✦", "Sin anuncios", "Experiencia limpia, sin interrupciones"))
                    com.lumiai.flashlight.core.domain.model.FlashMode.proModes()
                        .filter { BuildConfig.IS_DEBUG || !it.hidden }
                        .forEach { mode -> add(paywallFeatureFor(mode)) }
                }
            }
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

            // ── Rewarded ad section ────────────────────────────────────────
            val adsNeeded   = rewardedState.adsNeeded
            val adsPending  = rewardedState.adsPending
            val nextCost    = rewardedState.nextCost
            val progress    = if (nextCost > 0) adsPending.toFloat() / nextCost else 0f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LumiColor.Navy800)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Probar Pro gratis — 1 hora",
                        fontSize = 13.sp, fontWeight = FontWeight.W600,
                        color = LumiColor.White)
                    Text("$adsPending/$nextCost anuncios",
                        fontSize = 11.sp, color = LumiColor.Gray500)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = LumiColor.Purple400,
                    trackColor = LumiColor.Navy700,
                )
                Spacer(Modifier.height(10.dp))

                // Watch ad button
                val rewardedSource = remember { MutableInteractionSource() }
                val isRewardedPressed by rewardedSource.collectIsPressedAsState()
                val rewardedScale by animateFloatAsState(
                    if (isRewardedPressed) 0.97f else 1f,
                    animationSpec = spring(stiffness = 400f), label = "reward_btn"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(rewardedScale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                adLoading -> LumiColor.Navy700
                                isRewardedActive -> LumiColor.Green500.copy(0.3f)
                                else -> LumiColor.Purple500.copy(0.25f)
                            }
                        )
                        .border(1.dp,
                            when {
                                adLoading -> LumiColor.Navy600
                                isRewardedActive -> LumiColor.Green500.copy(0.5f)
                                else -> LumiColor.Purple400.copy(0.5f)
                            },
                            RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = rewardedSource,
                            indication = null,
                            enabled = !adLoading,
                            onClick = { viewModel.requestRewardedAd() },
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            adLoading        -> "Cargando anuncio…"
                            isRewardedActive -> "✓ Pro activo — ver más para renovar"
                            adsNeeded == 1   -> "▶  Ver 1 anuncio más — desbloquear"
                            else             -> "▶  Ver anuncio ($adsPending/$nextCost)"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W600,
                        color = when {
                            adLoading        -> LumiColor.Gray600
                            isRewardedActive -> LumiColor.Green400
                            else             -> LumiColor.Purple300
                        },
                    )
                }

                if (nextCost > RewardedProRepository.BASE_AD_COST) {
                    Text(
                        "Cada desbloqueo diario cuesta el doble. Se reinicia a medianoche.",
                        fontSize = 10.sp, color = LumiColor.Gray600,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp).fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Purchase CTA ───────────────────────────────────────────────
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
                            if (activity != null) viewModel.purchasePro(activity)
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
            val isRestoringPurchases by viewModel.isRestoringPurchases.collectAsState()
            Text(
                if (isRestoringPurchases) "Buscando compra…" else "Restaurar compra",
                fontSize = 12.sp,
                color = if (isRestoringPurchases) LumiColor.Amber400.copy(.6f) else LumiColor.Gray600,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isRestoringPurchases,
                        onClick = { viewModel.restorePurchases() },
                    )
            )
            Row(
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf("Pago único", "Sin cuenta", "Sin conexión").forEach { label ->
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

// Companion reference for BASE_AD_COST in the UI
private val RewardedProRepository = com.lumiai.flashlight.core.data.repository.RewardedProRepository

/**
 * Maps a Pro [FlashMode] to its paywall row (icon, title, description).
 *
 * Kept in sync with the AI Modes grid copy in ModePanel.kt. When a new Pro mode
 * is added or its grid copy changes, update the matching case here so the paywall
 * and the grid never describe the same mode differently. The icons mirror the
 * glyphs ModePanel draws so the two surfaces read as the same feature set.
 */
private fun paywallFeatureFor(
    mode: FlashMode,
): Triple<String, String, String> {
    return when (mode) {
        is FlashMode.SmartBrightness -> Triple("◎", "Brillo adaptativo", "Ajusta la intensidad según el sensor de luz")
        is FlashMode.ReadingMode     -> Triple("◑", "Modo lectura", "Luz cálida y estable, cómoda para los ojos")
        is FlashMode.AmbientSmart    -> Triple("⬨", "Modo ambiental", "Elige el brillo según la luz del entorno")
        is FlashMode.CustomRhythm    -> Triple("⬡", "Ritmos personalizados", "Patrones de parpadeo configurables")
        is FlashMode.SleepTimer      -> Triple("◌", "Temporizador de sueño", "Atenuación gradual hasta apagarse")
        is FlashMode.Music           -> Triple("♫", "Sincronización musical", "El flash sigue el ritmo de la música")
        is FlashMode.Voice           -> Triple("●", "Reactivo a voz", "Reacciona a la voz y los sonidos")
        is FlashMode.Walk            -> Triple("↹", "Modo caminar", "Pulso de luz con cada paso")
        else                         -> Triple("✦", mode.id, "Función Pro")
    }
}

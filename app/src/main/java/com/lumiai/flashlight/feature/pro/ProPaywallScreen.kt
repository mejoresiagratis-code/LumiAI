package com.lumiai.flashlight.feature.pro

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumiai.flashlight.feature.flash.FlashViewModel
import com.lumiai.flashlight.ui.theme.Amber400
import com.lumiai.flashlight.ui.theme.Purple400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPaywallScreen(
    onBack: () -> Unit,
    viewModel: FlashViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unlock Pro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text("LumiAI Pro", style = MaterialTheme.typography.headlineMedium)
            Text(
                "One-time purchase. No subscription. Unlock all AI features forever.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(16.dp))
            // Feature list
            listOf(
                "Smart brightness (AI adapts to your environment)",
                "Reading mode (warm tint + auto-dim)",
                "Ambient scene detection",
                "Custom AI-generated rhythm patterns",
                "Sleep timer with gradual power-off",
                "No ads. Ever.",
            ).forEach { feature ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Text("✦  $feature", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { /* TODO: viewModel.purchasePro(activity) */ },
                colors  = ButtonDefaults.buttonColors(containerColor = Purple400),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Unlock Pro — €2.99", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = { /* TODO: viewModel restore */ }) {
                Text("Restore purchase")
            }
        }
    }
}

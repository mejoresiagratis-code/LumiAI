package com.lumiai.flashlight.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    // TODO: implement onboarding slides (permission request, feature tour)
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Welcome to LumiAI", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onFinished) { Text("Get Started") }
    }
}

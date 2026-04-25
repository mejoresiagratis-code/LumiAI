package com.lumiai.flashlight.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lumiai.flashlight.feature.flash.FlashScreen
import com.lumiai.flashlight.feature.flash.FlashViewModel
import com.lumiai.flashlight.feature.onboarding.OnboardingScreen
import com.lumiai.flashlight.feature.pro.ProPaywallScreen
import com.lumiai.flashlight.feature.settings.SettingsScreen

@Composable
fun LumiNavHost() {
    val navController = rememberNavController()
    val flashViewModel: FlashViewModel = hiltViewModel()

    NavHost(
        navController    = navController,
        startDestination = NavRoute.Flash.route,
    ) {
        composable(NavRoute.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(NavRoute.Flash.route) {
                        popUpTo(NavRoute.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(NavRoute.Flash.route) {
            FlashScreen(
                viewModel    = flashViewModel,
                onOpenSettings = { navController.navigate(NavRoute.Settings.route) },
                onOpenPro      = { navController.navigate(NavRoute.Pro.route) },
            )
        }
        composable(NavRoute.Settings.route) {
            SettingsScreen(
                onBack    = { navController.popBackStack() },
                onOpenPro = { navController.navigate(NavRoute.Pro.route) },
            )
        }
        composable(NavRoute.Pro.route) {
            ProPaywallScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

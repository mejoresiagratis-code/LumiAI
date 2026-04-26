package com.lumiai.flashlight.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lumiai.flashlight.core.data.repository.SettingsRepository
import com.lumiai.flashlight.core.domain.model.UserSettings
import com.lumiai.flashlight.feature.flash.FlashScreen
import com.lumiai.flashlight.feature.flash.FlashViewModel
import com.lumiai.flashlight.feature.onboarding.OnboardingScreen
import com.lumiai.flashlight.feature.pro.ProPaywallScreen
import com.lumiai.flashlight.feature.settings.SettingsScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Lightweight ViewModel just for nav-level settings (onboarding + dark theme)
@HiltViewModel
class NavViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    fun markOnboardingSeen() {
        viewModelScope.launch { settingsRepository.markOnboardingSeen() }
    }
}

@Composable
fun LumiNavHost() {
    val navController  = rememberNavController()
    val flashViewModel: FlashViewModel = hiltViewModel()
    val navViewModel: NavViewModel     = hiltViewModel()

    val settings by navViewModel.settings.collectAsState()

    // Determine start destination once settings are loaded
    // Use Flash as default while loading (hasSeenOnboarding defaults to false → shows onboarding)
    val startDest = remember(settings.hasSeenOnboarding) {
        if (settings.hasSeenOnboarding) NavRoute.Flash.route
        else NavRoute.Onboarding.route
    }

    NavHost(
        navController    = navController,
        startDestination = startDest,
    ) {
        composable(NavRoute.Onboarding.route) {
            OnboardingScreen(
                onMarkSeen = { navViewModel.markOnboardingSeen() },
                onFinished = {
                    navController.navigate(NavRoute.Flash.route) {
                        popUpTo(NavRoute.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }
        composable(NavRoute.Flash.route) {
            FlashScreen(
                viewModel      = flashViewModel,
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
            ProPaywallScreen(onBack = { navController.popBackStack() })
        }
    }
}

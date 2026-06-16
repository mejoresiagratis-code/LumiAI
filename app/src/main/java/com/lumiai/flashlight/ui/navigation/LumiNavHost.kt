package com.lumiai.flashlight.ui.navigation

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lumiai.flashlight.core.data.repository.SettingsRepository
import com.lumiai.flashlight.core.domain.model.UserSettings
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.lumiai.flashlight.feature.flash.FlashScreen
import com.lumiai.flashlight.feature.flash.ModeConfigScreen
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

    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
    val context = LocalContext.current

    // BUG-5: wait for the persisted settings before choosing the start destination.
    // NavHost reads startDestination only once; starting before DataStore loads
    // would use the default (hasSeenOnboarding=false) and show onboarding to
    // returning users on every launch. Render nothing until settings arrive
    // (the splash screen is still up during this sub-frame gap).
    val loaded = settings ?: return

    val startDest = if (loaded.hasSeenOnboarding) NavRoute.Flash.route
                    else NavRoute.Onboarding.route

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
                onEnableNotifications = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    )
                },
            )
        }
        composable(NavRoute.Flash.route) {
            FlashScreen(
                viewModel      = flashViewModel,
                onOpenSettings = { navController.navigate(NavRoute.Settings.route) },
                onOpenPro      = { navController.navigate(NavRoute.Pro.route) },
                onOpenModeConfig = { modeId ->
                    navController.navigate(NavRoute.ModeConfig.route(modeId))
                },
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
        composable(
            route = NavRoute.ModeConfig.route,
            arguments = listOf(navArgument("modeId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val modeId = backStackEntry.arguments?.getString("modeId") ?: "steady"
            ModeConfigScreen(
                modeId    = modeId,
                viewModel = flashViewModel,
                onBack    = { navController.popBackStack() },
            )
        }
    }
}

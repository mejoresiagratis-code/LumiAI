package com.lumiai.flashlight.ui.navigation

sealed class NavRoute(val route: String) {
    object Onboarding : NavRoute("onboarding")
    object Flash      : NavRoute("flash")
    object Settings   : NavRoute("settings")
    object Pro        : NavRoute("pro_paywall")
    // ModeConfig: modeId as path arg — navigate via ModeConfig.route("strobe")
    object ModeConfig : NavRoute("mode_config/{modeId}") {
        fun route(modeId: String) = "mode_config/$modeId"
    }
}

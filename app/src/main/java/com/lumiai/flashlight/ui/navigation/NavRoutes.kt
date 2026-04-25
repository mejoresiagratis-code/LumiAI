package com.lumiai.flashlight.ui.navigation

sealed class NavRoute(val route: String) {
    object Onboarding : NavRoute("onboarding")
    object Flash      : NavRoute("flash")
    object Settings   : NavRoute("settings")
    object Pro        : NavRoute("pro_paywall")
}

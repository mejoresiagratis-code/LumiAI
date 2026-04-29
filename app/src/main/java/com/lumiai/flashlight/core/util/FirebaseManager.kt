package com.lumiai.flashlight.core.util

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.lumiai.flashlight.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralises Firebase Remote Config and Performance Monitoring.
 * Uses direct APIs (no ktx extensions) for BOM 33+ compatibility.
 */
@Singleton
class FirebaseManager @Inject constructor() {

    companion object {
        const val RC_INTERSTITIAL_EVERY_N = "interstitial_every_n_modes"
        const val RC_SLEEP_MAX_MINUTES    = "sleep_max_minutes"
        const val RC_FX_POLICE_ENABLED    = "fx_police_enabled"
        const val RC_PRO_PRICE_LABEL      = "pro_price_label"
        const val RC_CANDELA_SPEED_MS     = "candela_min_interval_ms"
    }

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance()
    }

    private val performance: FirebasePerformance by lazy {
        FirebasePerformance.getInstance()
    }

    fun init() {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(
                if (isDebugBuild()) 300L else 3600L
            )
            .build()
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate()
    }

    // ── Typed Remote Config accessors ─────────────────────────────────────────
    fun interstitialEveryN(): Int  = remoteConfig.getLong(RC_INTERSTITIAL_EVERY_N).toInt()
    fun sleepMaxMinutes(): Int     = remoteConfig.getLong(RC_SLEEP_MAX_MINUTES).toInt()
    fun fxPoliceEnabled(): Boolean = remoteConfig.getBoolean(RC_FX_POLICE_ENABLED)
    fun proPriceLabel(): String    = remoteConfig.getString(RC_PRO_PRICE_LABEL)
    fun candelaSpeedMs(): Long     = remoteConfig.getLong(RC_CANDELA_SPEED_MS)

    // ── Performance trace helpers ─────────────────────────────────────────────
    fun <T> traceBindCamera(block: () -> T): T {
        val trace = performance.newTrace("bind_camera")
        trace.start()
        return try { block() } finally { trace.stop() }
    }

    fun <T> traceFirstTorch(block: () -> T): T {
        val trace = performance.newTrace("first_torch_on")
        trace.start()
        return try { block() } finally { trace.stop() }
    }

    fun <T> traceActivateMode(modeId: String, block: () -> T): T {
        val trace = performance.newTrace("activate_mode")
        trace.putAttribute("mode_id", modeId)
        trace.start()
        return try { block() } finally { trace.stop() }
    }

    private fun isDebugBuild(): Boolean = try {
        Class.forName("com.lumiai.flashlight.BuildConfig")
            .getField("DEBUG").getBoolean(null)
    } catch (e: Exception) { false }
}

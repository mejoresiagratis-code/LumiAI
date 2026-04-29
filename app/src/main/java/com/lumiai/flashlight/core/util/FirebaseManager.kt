package com.lumiai.flashlight.core.util

import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import com.google.firebase.perf.ktx.trace
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.lumiai.flashlight.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralises Firebase Remote Config and Performance Monitoring.
 *
 * Remote Config keys are defined as constants here.
 * Default values are in res/xml/remote_config_defaults.xml.
 * Performance traces wrap critical camera and mode operations.
 */
@Singleton
class FirebaseManager @Inject constructor() {

    // ── Remote Config keys ────────────────────────────────────────────────────
    companion object {
        const val RC_INTERSTITIAL_EVERY_N    = "interstitial_every_n_modes"  // default 5
        const val RC_SLEEP_MAX_MINUTES       = "sleep_max_minutes"           // default 10
        const val RC_FX_POLICE_ENABLED       = "fx_police_enabled"           // default true
        const val RC_PRO_PRICE_LABEL         = "pro_price_label"             // default "2.99"
        const val RC_CANDELA_SPEED_MS        = "candela_min_interval_ms"     // default 80
    }

    private val remoteConfig = Firebase.remoteConfig

    fun init() {
        val settings = remoteConfigSettings {
            // Fetch every hour in prod, every 5 min in debug
            minimumFetchIntervalInSeconds = if (BuildConfigHelper.isDebug) 300L else 3600L
        }
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate()
    }

    // ── Typed accessors ───────────────────────────────────────────────────────
    fun interstitialEveryN(): Int   = remoteConfig.getLong(RC_INTERSTITIAL_EVERY_N).toInt()
    fun sleepMaxMinutes(): Int      = remoteConfig.getLong(RC_SLEEP_MAX_MINUTES).toInt()
    fun fxPoliceEnabled(): Boolean  = remoteConfig.getBoolean(RC_FX_POLICE_ENABLED)
    fun proPriceLabel(): String     = remoteConfig.getString(RC_PRO_PRICE_LABEL)
    fun candelaSpeedMs(): Long      = remoteConfig.getLong(RC_CANDELA_SPEED_MS)

    // ── Performance traces ────────────────────────────────────────────────────
    /** Wraps camera bind — call from FlashRepositoryImpl.bindCamera() */
    fun <T> traceBindCamera(block: () -> T): T =
        Firebase.performance.newTrace("bind_camera").let { t ->
            t.start(); val result = block(); t.stop(); result
        }

    /** Wraps first torch activation after cold start */
    fun <T> traceFirstTorch(block: () -> T): T =
        Firebase.performance.newTrace("first_torch_on").let { t ->
            t.start(); val result = block(); t.stop(); result
        }

    /** Wraps mode activation (all 14 modes — tag added automatically) */
    fun <T> traceActivateMode(modeId: String, block: () -> T): T =
        Firebase.performance.newTrace("activate_mode").let { t ->
            t.putAttribute("mode_id", modeId)
            t.start(); val result = block(); t.stop(); result
        }
}

/** Helper to check build type without BuildConfig (avoids Hilt circular dep) */
private object BuildConfigHelper {
    val isDebug: Boolean get() = try {
        Class.forName("com.lumiai.flashlight.BuildConfig")
            .getField("DEBUG").getBoolean(null)
    } catch (e: Exception) { false }
}

package com.lumiai.flashlight.core.util

import com.lumiai.flashlight.R

/**
 * Firebase Remote Config and Performance Monitoring.
 * Plain object — Firebase SDKs are already singletons, no DI needed.
 * Call FirebaseManager.init() once from Application.onCreate().
 */
object FirebaseManager {

    // ── Remote Config keys ────────────────────────────────────────────────────
    const val RC_INTERSTITIAL_EVERY_N = "interstitial_every_n_modes"
    const val RC_SLEEP_MAX_MINUTES    = "sleep_max_minutes"
    const val RC_FX_POLICE_ENABLED    = "fx_police_enabled"
    const val RC_PRO_PRICE_LABEL      = "pro_price_label"
    const val RC_CANDELA_SPEED_MS     = "candela_min_interval_ms"

    // Lazy to avoid initializing Firebase before Application is ready
    private val remoteConfig by lazy {
        try {
            val cls = Class.forName("com.google.firebase.remoteconfig.FirebaseRemoteConfig")
            cls.getMethod("getInstance").invoke(null)
        } catch (e: Exception) { null }
    }

    private val performance by lazy {
        try {
            val cls = Class.forName("com.google.firebase.perf.FirebasePerformance")
            cls.getMethod("getInstance").invoke(null)
        } catch (e: Exception) { null }
    }

    fun init(context: android.content.Context) {
        try {
            val rc = Class.forName("com.google.firebase.remoteconfig.FirebaseRemoteConfig")
                .getMethod("getInstance").invoke(null) ?: return
            val settingsBuilder = Class.forName("com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings\$Builder")
                .getDeclaredConstructor().newInstance()
            val isDebug = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
            val intervalMethod = settingsBuilder.javaClass.getMethod("setMinimumFetchIntervalInSeconds", Long::class.java)
            intervalMethod.invoke(settingsBuilder, if (isDebug) 300L else 3600L)
            val settings = settingsBuilder.javaClass.getMethod("build").invoke(settingsBuilder)
            rc.javaClass.getMethod("setConfigSettingsAsync", settings.javaClass).invoke(rc, settings)
            rc.javaClass.getMethod("setDefaultsAsync", Int::class.java).invoke(rc, R.xml.remote_config_defaults)
            rc.javaClass.getMethod("fetchAndActivate").invoke(rc)
        } catch (e: Exception) { /* Firebase not initialized yet — ok */ }
    }

    // ── Typed Remote Config accessors ─────────────────────────────────────────
    fun interstitialEveryN(): Int = rcLong(RC_INTERSTITIAL_EVERY_N, 5L).toInt()
    fun sleepMaxMinutes(): Int    = rcLong(RC_SLEEP_MAX_MINUTES, 10L).toInt()
    fun fxPoliceEnabled(): Boolean = rcBool(RC_FX_POLICE_ENABLED, true)
    fun proPriceLabel(): String   = rcString(RC_PRO_PRICE_LABEL, "2.99")
    fun candelaSpeedMs(): Long    = rcLong(RC_CANDELA_SPEED_MS, 80L)

    // ── Performance traces ────────────────────────────────────────────────────
    fun <T> traceBindCamera(block: () -> T): T = withTrace("bind_camera", block)
    fun <T> traceFirstTorch(block: () -> T): T = withTrace("first_torch_on", block)
    @Suppress("UNUSED_PARAMETER")
    fun <T> traceActivateMode(modeId: String, block: () -> T): T =
        withTrace("activate_mode") { block() }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun <T> withTrace(name: String, block: () -> T): T {
        val trace = try {
            performance?.javaClass?.getMethod("newTrace", String::class.java)
                ?.invoke(performance, name)
        } catch (e: Exception) { null }
        try { trace?.javaClass?.getMethod("start")?.invoke(trace) } catch (_: Exception) {}
        return try { block() } finally {
            try { trace?.javaClass?.getMethod("stop")?.invoke(trace) } catch (_: Exception) {}
        }
    }

    private fun rcLong(key: String, default: Long): Long = try {
        remoteConfig?.javaClass?.getMethod("getLong", String::class.java)
            ?.invoke(remoteConfig, key) as? Long ?: default
    } catch (e: Exception) { default }

    private fun rcBool(key: String, default: Boolean): Boolean = try {
        remoteConfig?.javaClass?.getMethod("getBoolean", String::class.java)
            ?.invoke(remoteConfig, key) as? Boolean ?: default
    } catch (e: Exception) { default }

    private fun rcString(key: String, default: String): String = try {
        remoteConfig?.javaClass?.getMethod("getString", String::class.java)
            ?.invoke(remoteConfig, key) as? String ?: default
    } catch (e: Exception) { default }
}

package com.lumiai.flashlight.core.util

import com.lumiai.flashlight.core.domain.model.FlashMode

/**
 * Estimates remaining torch runtime based on mode, intensity and battery level.
 *
 * Drain rates are empirical averages across mid-range Android devices.
 * They deliberately underestimate to avoid user disappointment.
 *
 * All values in % battery per hour at intensity=1.0.
 */
object EnergyEstimator {

    // Base drain rate (% battery/hour) per mode at intensity 1.0.
    // Calibrated to realistic phone-LED magnitudes: a steady torch lasts
    // ~3-4h, not the ~28h the previous (10x too low) values implied.
    private fun baseDrainPctPerHour(mode: FlashMode): Float = when (mode) {
        is FlashMode.Steady          -> 30f    // LED always on, highest drain (~3.3h)
        is FlashMode.Screen          -> 22f    // screen at full brightness
        is FlashMode.AmbientSmart    -> 26f    // steady + light sensor
        is FlashMode.ReadingMode     -> 24f    // steady + gradual dim
        is FlashMode.SleepTimer      -> 20f    // fading torch
        is FlashMode.SmartBrightness -> 16f    // pulsing, sensor active
        is FlashMode.Strobe          -> 15f    // ~50% duty cycle average
        is FlashMode.CustomRhythm    -> 13f    // slow pulsing
        is FlashMode.Disco           -> 12f    // random ~40% duty cycle
        is FlashMode.MorseCustom     -> 11f    // similar to SOS
        is FlashMode.Sos             -> 10f    // ~35% duty cycle ITU pattern
        is FlashMode.Music           -> 8f     // mic-triggered bursts
        is FlashMode.Voice           -> 7f     // voice-triggered bursts
        is FlashMode.Walk            -> 6f     // step-triggered bursts
    }

    /**
     * Returns estimated remaining minutes of runtime.
     *
     * @param batteryLevel 0.0–1.0 (current battery %)
     * @param mode current FlashMode
     * @param intensity torch intensity 0.1–1.0 (affects LED modes only)
     * @return minutes remaining, or null if mode doesn't use battery noticeably
     */
    fun estimateMinutesRemaining(
        batteryLevel: Float,
        mode: FlashMode,
        intensity: Float = 1.0f,
    ): Int {
        val pctAvailable = (batteryLevel * 100f).coerceIn(0f, 100f)

        // Intensity scales drain for hardware-LED modes; Screen brightness is handled separately
        val intensityMultiplier = when (mode) {
            is FlashMode.Screen -> 1.0f   // screen brightness already in baseDrain
            else                -> intensity.coerceIn(0.1f, 1.0f)
        }

        val drainPerHour = baseDrainPctPerHour(mode) * intensityMultiplier
        if (drainPerHour <= 0f) return Int.MAX_VALUE

        val hoursRemaining = pctAvailable / drainPerHour
        return (hoursRemaining * 60f).toInt().coerceAtLeast(0)
    }

    /** Human-readable time string: "2h 15m", "45m", "< 5m" */
    fun formatMinutes(minutes: Int): String = when {
        minutes <= 0    -> "< 1m"
        minutes < 5     -> "< 5m"
        minutes < 60    -> "${minutes}m"
        minutes < 120   -> "${minutes / 60}h ${minutes % 60}m"
        else            -> { val h = minutes / 60; val m = minutes % 60; if (m > 0) "${h}h ${m}m" else "${h}h" }
    }
}

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

    // Base drain rate (% battery/hour) per mode at intensity 1.0
    private fun baseDrainPctPerHour(mode: FlashMode): Float = when (mode) {
        is FlashMode.Steady          -> 3.5f   // LED always on, highest drain
        is FlashMode.Strobe          -> 1.8f   // 50% duty cycle average
        is FlashMode.Disco           -> 1.5f   // random ~40% duty cycle
        is FlashMode.Sos             -> 1.2f   // ~35% duty cycle ITU pattern
        is FlashMode.MorseCustom     -> 1.4f   // similar to SOS
        is FlashMode.Screen          -> 6.5f   // screen at full brightness
        is FlashMode.SmartBrightness -> 2.0f   // pulsing, sensor active
        is FlashMode.ReadingMode     -> 3.0f   // steady + gradual dim
        is FlashMode.AmbientSmart    -> 3.2f   // steady + light sensor
        is FlashMode.CustomRhythm    -> 1.6f   // slow pulsing
        is FlashMode.SleepTimer      -> 2.5f   // fading torch
        is FlashMode.Music           -> 1.0f   // mic-triggered bursts
        is FlashMode.Walk            -> 0.8f   // step-triggered bursts
        is FlashMode.Voice           -> 0.9f   // voice-triggered bursts
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
        else            -> "${minutes / 60}h ${(minutes % 60).let { if (it > 0) " ${it}m" else "" }}"
    }
}

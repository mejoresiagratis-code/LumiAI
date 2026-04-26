package com.lumiai.flashlight.core.domain.model

/**
 * Sealed hierarchy of all supported flashlight modes.
 * FREE modes are always available.
 * PRO modes require [isPro] check before activation.
 */
sealed class FlashMode(val id: String, val isPro: Boolean = false) {

    // ── Free modes ────────────────────────────────────────────────────────
    object Steady    : FlashMode("steady")
    object Screen    : FlashMode("screen")          // White screen fallback (no camera flash)
    object Sos       : FlashMode("sos")
    data class MorseCustom(val text: String = "") : FlashMode("morse_custom")
    data class Strobe(val hz: Float = 5f) : FlashMode("strobe")
    data class Disco  (val bpm: Float = 120f) : FlashMode("disco")

    // ── Pro modes (IA) ────────────────────────────────────────────────────
    object SmartBrightness : FlashMode("smart_brightness", isPro = true)   // Gemini Nano adapts
    object ReadingMode     : FlashMode("reading_mode",     isPro = true)   // Warm tint + auto-dim
    object AmbientSmart    : FlashMode("ambient_smart",    isPro = true)   // MLKit detects scene
    data class CustomRhythm(val pattern: LongArray = longArrayOf()) : FlashMode("custom_rhythm", isPro = true)
    object SleepTimer      : FlashMode("sleep_timer",      isPro = true)   // Gradual power-off AI
    object Music           : FlashMode("music",            isPro = true)   // Beat detection via mic
    object Walk            : FlashMode("walk",             isPro = true)   // Pulse on each step
    object Voice           : FlashMode("voice",            isPro = true)   // React to voice/sound level

    companion object {
        fun freeModes(): List<FlashMode> = listOf(Steady, Screen, Sos, MorseCustom(), Strobe(), Disco())
        fun proModes(): List<FlashMode>  = listOf(SmartBrightness, ReadingMode, AmbientSmart, CustomRhythm(), SleepTimer, Music, Walk, Voice)
        fun all(): List<FlashMode>       = freeModes() + proModes()
    }
}

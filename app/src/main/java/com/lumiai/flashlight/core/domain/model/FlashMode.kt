package com.lumiai.flashlight.core.domain.model

/**
 * Sealed hierarchy of all supported flashlight modes.
 *
 * [isPro]    — requires Pro subscription to activate.
 * [hidden]   — excluded from all UI grids. Used during staged rollout:
 *              set hidden=false when a Pro mode is ready for users to see.
 *              Activation is still Pro-gated — hidden is purely a UI flag.
 */
sealed class FlashMode(
    val id: String,
    val isPro: Boolean = false,
    val hidden: Boolean = false,
) {

    // ── Free modes — always visible ───────────────────────────────────────────
    object Steady    : FlashMode("steady")
    object Screen    : FlashMode("screen")
    object Sos       : FlashMode("sos")
    data class MorseCustom(val text: String = "") : FlashMode("morse_custom")
    data class Strobe(val hz: Float = 5f)         : FlashMode("strobe")
    data class Disco(val bpm: Float = 120f)        : FlashMode("disco")

    // ── Pro modes — hidden until explicitly enabled per mode ──────────────────
    // To reveal a mode: change hidden = true → hidden = false, bump versionCode.
    // Activation remains Pro-gated regardless of hidden value.
    object SmartBrightness : FlashMode("smart_brightness", isPro = true, hidden = true)
    object ReadingMode     : FlashMode("reading_mode",     isPro = true, hidden = true)
    object AmbientSmart    : FlashMode("ambient_smart",    isPro = true, hidden = true)
    data class CustomRhythm(val pattern: LongArray = longArrayOf())
                           : FlashMode("custom_rhythm",   isPro = true, hidden = true) {
        // LongArray uses reference identity in the generated data-class equals/hashCode,
        // so two CustomRhythm with the same pattern compared unequal (breaking state
        // de-duplication / recomposition). Compare by content instead.
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CustomRhythm) return false
            return pattern.contentEquals(other.pattern)
        }
        override fun hashCode(): Int = pattern.contentHashCode()
    }
    object SleepTimer      : FlashMode("sleep_timer",      isPro = true, hidden = false)
    object Music           : FlashMode("music",            isPro = true, hidden = false)
    object Walk            : FlashMode("walk",             isPro = true, hidden = true)
    object Voice           : FlashMode("voice",            isPro = true, hidden = false)

    companion object {
        fun freeModes(): List<FlashMode>    = listOf(Steady, Screen, Sos, MorseCustom(), Strobe(), Disco())
        fun proModes(): List<FlashMode>     = listOf(SmartBrightness, ReadingMode, AmbientSmart, CustomRhythm(), SleepTimer, Music, Walk, Voice)
        fun visibleProModes(): List<FlashMode> = proModes().filter { !it.hidden }
        fun all(): List<FlashMode>          = freeModes() + proModes()
        fun allVisible(): List<FlashMode>   = freeModes() + visibleProModes()
    }
}

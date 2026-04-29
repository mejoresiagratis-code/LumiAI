package com.lumiai.flashlight.core.util

import com.lumiai.flashlight.core.util.MorseEncoder
import kotlinx.coroutines.*
import javax.inject.Singleton
import kotlin.math.roundToLong

/** Flash pattern per strobe cycle */
enum class StrobePattern { SINGLE, DOUBLE, TRIPLE }

/**
 * Coroutine-based controller for timed flash patterns.
 * All patterns run on a dedicated IO dispatcher and are cancellable.
 */
@Singleton
class StrobeController constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null
    @Volatile private var active = false   // guards against post-cancel setTorch calls

    fun stop(setTorch: ((Boolean) -> Unit)? = null) {
        val wasActive = active     // capture before clearing — only emit OFF if was running
        active = false             // FIRST: prevent any new setTorch calls from the loop
        activeJob?.cancel()
        activeJob = null
        if (wasActive) setTorch?.invoke(false)    // only emit OFF if something was actually running
    }

    /** Wraps setTorch to be a no-op after stop() is called */
    private fun guarded(on: Boolean, setTorch: (Boolean) -> Unit) {
        if (active) setTorch(on)
    }

    /** Simple on/off strobe at [hz] cycles per second (max 20 Hz) */
    fun startStrobe(
        hz: Float,
        setTorch: (Boolean) -> Unit,
        intensity: Float = 1.0f,
        pattern: StrobePattern = StrobePattern.SINGLE,
    ) {
        stop(); active = true
        val clampedHz  = hz.coerceIn(0.5f, 20f)
        val periodMs   = (1000f / clampedHz).roundToLong()
        val pulseMs    = (periodMs * intensity.coerceIn(0.1f, 0.9f)).toLong()
        val burstGapMs = 30L   // gap between pulses in a burst (fast enough to look linked)
        activeJob = scope.launch {
            while (isActive) {
                val pulseCount = when (pattern) {
                    StrobePattern.SINGLE -> 1
                    StrobePattern.DOUBLE -> 2
                    StrobePattern.TRIPLE -> 3
                }
                val burstDuration = pulseMs * pulseCount + burstGapMs * (pulseCount - 1)
                val offMs = (periodMs - burstDuration).coerceAtLeast(20L)
                repeat(pulseCount) { i ->
                    guarded(true,  setTorch); delay(pulseMs)
                    guarded(false, setTorch)
                    if (i < pulseCount - 1) delay(burstGapMs)
                }
                delay(offMs)
            }
        }
    }

    /** SOS pattern: · · ·  — — —  · · · (Morse) */
    fun startSos(setTorch: (Boolean) -> Unit, speed: Float = 1.0f) {
        stop(); active = true
        val dit     = (200L / speed).toLong().coerceAtLeast(50L)
        val dah     = (600L / speed).toLong().coerceAtLeast(150L)
        val gap     = (200L / speed).toLong().coerceAtLeast(50L)
        val wordGap = (1400L / speed).toLong().coerceAtLeast(300L)
        val pattern = buildList {
            repeat(3) { add(dit); add(gap) }    // S ...
            add(gap)
            repeat(3) { add(dah); add(gap) }    // O ---
            add(gap)
            repeat(3) { add(dit); add(gap) }    // S ...
            add(wordGap)
        }
        activeJob = scope.launch {
            while (isActive) {
                pattern.forEachIndexed { i, duration ->
                    guarded(i % 2 == 0, setTorch)
                    delay(duration)
                }
            }
        }
    }

    /** Disco mode: random on/off within BPM tempo */
    fun startDisco(bpm: Float, setTorch: (Boolean) -> Unit, intensity: Float = 1.0f) {
        stop(); active = true
        val beatMs = (60_000f / bpm).toLong()
        activeJob = scope.launch {
            while (isActive) {
                // intensity scales the max on-time fraction
                val maxFraction = (0.2f + intensity * 0.5f).coerceIn(0.1f, 0.7f)
                val onMs = (beatMs * (0.1f + Math.random().toFloat() * maxFraction)).toLong()
                guarded(true, setTorch)
                delay(onMs)
                guarded(false, setTorch)
                delay(beatMs - onMs)
            }
        }
    }


    /** Custom Morse: encodes arbitrary text and flashes it in a loop */
    fun startMorse(text: String, setTorch: (Boolean) -> Unit, speed: Float = 1.0f) {
        stop(); active = true
        if (text.isBlank()) return
        val pattern = MorseEncoder.encode(text, speed)
        if (pattern.isEmpty()) return
        activeJob = scope.launch {
            while (isActive) {
                pattern.forEach { (onMs, offMs) ->
                    if (onMs > 0)  { guarded(true,  setTorch); delay(onMs)  }
                    if (offMs > 0) { guarded(false, setTorch); delay(offMs) }
                }
            }
        }
    }

    /** Custom pattern: array of [on_ms, off_ms, on_ms, off_ms, ...] */
    fun startCustom(pattern: LongArray, setTorch: (Boolean) -> Unit) {
        stop(); active = true
        activeJob = scope.launch {
            while (isActive) {
                pattern.forEachIndexed { i, duration ->
                    guarded(i % 2 == 0, setTorch)
                    delay(duration)
                }
            }
        }
    }
}

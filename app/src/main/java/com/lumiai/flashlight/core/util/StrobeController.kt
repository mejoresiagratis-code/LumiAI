package com.lumiai.flashlight.core.util

import kotlinx.coroutines.*
import javax.inject.Singleton
import kotlin.math.roundToLong

/**
 * Coroutine-based controller for timed flash patterns.
 * All patterns run on a dedicated IO dispatcher and are cancellable.
 */
@Singleton
class StrobeController constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    fun stop() {
        activeJob?.cancel()
        activeJob = null
    }

    /** Simple on/off strobe at [hz] cycles per second (max 20 Hz) */
    fun startStrobe(hz: Float, setTorch: (Boolean) -> Unit) {
        val clampedHz = hz.coerceIn(0.5f, 20f)
        val halfPeriodMs = (1000f / (clampedHz * 2)).roundToLong()
        activeJob = scope.launch {
            var on = true
            while (isActive) {
                setTorch(on)
                on = !on
                delay(halfPeriodMs)
            }
        }
    }

    /** SOS pattern: · · ·  — — —  · · · (Morse) */
    fun startSos(setTorch: (Boolean) -> Unit) {
        // dit=200ms, dah=600ms, gap=200ms, word gap=1400ms
        val dit = 200L
        val dah = 600L
        val gap = 200L
        val wordGap = 1400L
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
                    setTorch(i % 2 == 0)
                    delay(duration)
                }
            }
        }
    }

    /** Disco mode: random on/off within BPM tempo */
    fun startDisco(bpm: Float, setTorch: (Boolean) -> Unit) {
        val beatMs = (60_000f / bpm).toLong()
        activeJob = scope.launch {
            while (isActive) {
                val onMs = (beatMs * (0.2f + Math.random().toFloat() * 0.5f)).toLong()
                setTorch(true)
                delay(onMs)
                setTorch(false)
                delay(beatMs - onMs)
            }
        }
    }

    /** Custom pattern: array of [on_ms, off_ms, on_ms, off_ms, ...] */
    fun startCustom(pattern: LongArray, setTorch: (Boolean) -> Unit) {
        activeJob = scope.launch {
            while (isActive) {
                pattern.forEachIndexed { i, duration ->
                    setTorch(i % 2 == 0)
                    delay(duration)
                }
            }
        }
    }
}

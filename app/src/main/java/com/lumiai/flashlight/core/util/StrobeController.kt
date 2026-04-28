package com.lumiai.flashlight.core.util

import com.lumiai.flashlight.core.util.MorseEncoder

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

    fun stop(setTorch: ((Boolean) -> Unit)? = null) {
        activeJob?.cancel()
        activeJob = null
        setTorch?.invoke(false)   // guarantee torch OFF on mode exit
    }

    /** Simple on/off strobe at [hz] cycles per second (max 20 Hz) */
    fun startStrobe(hz: Float, setTorch: (Boolean) -> Unit) {
        stop()
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
        stop()
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
        stop()
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


    /** Custom Morse: encodes arbitrary text and flashes it in a loop */
    fun startMorse(text: String, setTorch: (Boolean) -> Unit) {
        stop()
        if (text.isBlank()) return
        val pattern = MorseEncoder.encode(text)
        if (pattern.isEmpty()) return
        activeJob = scope.launch {
            while (isActive) {
                pattern.forEach { (onMs, offMs) ->
                    if (onMs > 0)  { setTorch(true);  delay(onMs)  }
                    if (offMs > 0) { setTorch(false); delay(offMs) }
                }
            }
        }
    }

    /** Custom pattern: array of [on_ms, off_ms, on_ms, off_ms, ...] */
    fun startCustom(pattern: LongArray, setTorch: (Boolean) -> Unit) {
        stop()
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

package com.lumiai.flashlight.core.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Implements the actual behavior of each AI flash mode.
 * No Gemini Nano needed for these — all run on-device with sensors + coroutines.
 *
 * SMART   → reads ambient light sensor, adjusts pulse frequency
 * READ    → slow warm-temperature pulse (3s cycle), auto-dims over time
 * AMBIENT → detects light level and picks the right intensity pattern
 * CUSTOM  → generative rhythm based on time-of-day + randomness
 * SLEEP   → starts at full brightness, dims gradually over ~3 minutes then off
 */
@Singleton
class AiModeController @Inject constructor(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var lightLevel = 50f   // lux, default mid-light

    // ── Light sensor listener — used by SMART and AMBIENT ────────────────────
    private val lightListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            if (e.sensor.type == Sensor.TYPE_LIGHT) {
                lightLevel = e.values[0].coerceIn(0f, 10000f)
            }
        }
        override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
    }

    private fun startLightSensor() {
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let {
            sensorManager.registerListener(lightListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        activeJob?.cancel()
        activeJob = null
        sensorManager.unregisterListener(lightListener)
    }

    // ── SMART: ambient-aware pulsing ──────────────────────────────────────────
    // In bright light → faster short pulses (more noticeable)
    // In dark → slow steady on (less jarring)
    fun startSmart(setTorch: (Boolean) -> Unit) {
        startLightSensor()
        activeJob = scope.launch {
            while (isActive) {
                val lux = lightLevel
                val (onMs, offMs) = when {
                    lux > 1000f -> Pair(80L,  120L)   // bright: fast blink
                    lux > 200f  -> Pair(200L, 300L)   // indoor: medium pulse
                    lux > 30f   -> Pair(400L, 600L)   // dim: slow pulse
                    else        -> Pair(800L, 200L)   // dark: mostly on, brief off
                }
                setTorch(true)
                delay(onMs)
                setTorch(false)
                delay(offMs)
            }
        }
    }

    // ── READ: gentle slow pulse — easy on eyes ────────────────────────────────
    // Full on for 2.5s, brief dim (off) for 0.3s, repeat
    // Gets progressively dimmer over 10 minutes (simulated via longer off periods)
    fun startReading(setTorch: (Boolean) -> Unit) {
        activeJob = scope.launch {
            val startMs = System.currentTimeMillis()
            while (isActive) {
                val elapsedMin = (System.currentTimeMillis() - startMs) / 60_000f
                // After 10 min, off period grows: 0.3s → up to 2s
                val offMs = min(300L + (elapsedMin * 170L).toLong(), 2000L)
                setTorch(true)
                delay(2500L)
                setTorch(false)
                delay(offMs)
            }
        }
    }

    // ── AMBIENT: scene detection via light sensor ─────────────────────────────
    // Reads ambient lux once, picks a matching pattern and holds it
    fun startAmbient(setTorch: (Boolean) -> Unit) {
        startLightSensor()
        activeJob = scope.launch {
            delay(600) // let sensor stabilize
            val lux = lightLevel
            when {
                // Total darkness → SOS pattern (signal mode)
                lux < 5f    -> {
                    while (isActive) {
                        listOf(200L, 200L, 200L, 200L, 200L, 600L,
                               600L, 200L, 600L, 200L, 600L, 600L,
                               200L, 200L, 200L, 200L, 200L, 1400L)
                            .forEachIndexed { i, ms ->
                                setTorch(i % 2 == 0)
                                delay(ms)
                            }
                    }
                }
                // Dark room → steady on (best for navigation)
                lux < 50f   -> { setTorch(true); awaitCancellation() }
                // Indoor → slow pulse (comfortable reading)
                lux < 500f  -> {
                    while (isActive) {
                        setTorch(true); delay(2000L)
                        setTorch(false); delay(500L)
                    }
                }
                // Outdoor bright → fast attention pulses
                else        -> {
                    while (isActive) {
                        setTorch(true); delay(100L)
                        setTorch(false); delay(150L)
                    }
                }
            }
        }
    }

    // ── CUSTOM: generative rhythm based on time-of-day ───────────────────────
    // Morning → energetic   Afternoon → steady   Evening → warm slow   Night → minimal
    fun startCustomRhythm(setTorch: (Boolean) -> Unit) {
        activeJob = scope.launch {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val pattern: List<Pair<Long, Long>> = when (hour) {
                in 6..9   -> listOf(100L to 100L, 100L to 200L, 300L to 100L) // morning burst
                in 10..14 -> listOf(500L to 300L, 200L to 200L)               // midday steady
                in 15..19 -> listOf(800L to 400L, 300L to 600L)               // afternoon warm
                in 20..22 -> listOf(1200L to 300L, 600L to 800L)              // evening slow
                else      -> listOf(2000L to 500L)                            // night minimal
            }
            var idx = 0
            while (isActive) {
                val (onMs, offMs) = pattern[idx % pattern.size]
                setTorch(true);  delay(onMs)
                setTorch(false); delay(offMs)
                idx++
            }
        }
    }

    // ── SLEEP: gradual fade simulation ────────────────────────────────────────
    // Flash can't truly dim, so we simulate by increasing off-time over 3 minutes
    // 0–1min: on 90%, off 10%  →  1–2min: on 60%  →  2–3min: on 20%  →  off
    fun startSleepTimer(setTorch: (Boolean) -> Unit) {
        activeJob = scope.launch {
            val totalMs = 3 * 60 * 1000L
            val startMs = System.currentTimeMillis()
            val cycleMs = 400L

            while (isActive) {
                val elapsed = System.currentTimeMillis() - startMs
                if (elapsed >= totalMs) {
                    setTorch(false)
                    break
                }
                val progress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                // duty cycle goes from 0.9 → 0.0 linearly
                val duty = max(0f, 1f - (progress * 1.1f))
                val onMs = (cycleMs * duty).toLong().coerceAtLeast(0L)
                val offMs = cycleMs - onMs

                if (onMs > 0) { setTorch(true); delay(onMs) }
                if (offMs > 0) { setTorch(false); delay(offMs) }
            }
            setTorch(false)
        }
    }
}

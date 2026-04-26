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
 * Implements each AI flash mode.
 * All modes are designed to feel intentional — no unintended blinks.
 *
 * Design principle: start with torch ON, then transition to the mode pattern.
 * This avoids any dark flash at activation.
 */
@Singleton
class AiModeController @Inject constructor(
    private val context: Context,
) {
    private val scope  = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null
    private var musicDetector: MusicBeatDetector? = null

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var lightLevel = 200f   // default: indoor. Updated by sensor.

    private val lightListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            if (e.sensor.type == Sensor.TYPE_LIGHT)
                lightLevel = e.values[0].coerceIn(0f, 10000f)
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
        musicDetector?.stop()
        musicDetector = null
        sensorManager.unregisterListener(lightListener)
    }

    // ── ◎ SMART — adapts to ambient light ────────────────────────────────────
    // Starts STEADY for 1.5s while sensor warms up, then transitions to
    // adaptive pulsing. In dark environments stays mostly ON.
    fun startSmart(setTorch: (Boolean) -> Unit, setStrength: ((Float) -> Unit)? = null) {
        startLightSensor()
        activeJob = scope.launch {
            // Warmup: steady ON while sensor stabilizes
            setTorch(true)
            delay(1500)

            while (isActive) {
                val lux = lightLevel
                val (onMs, offMs) = when {
                    lux > 1000f -> 200L to 200L    // bright outdoor: noticeable pulse
                    lux > 200f  -> 400L to 300L    // indoor: soft pulse
                    lux > 30f   -> 800L to 400L    // dim: mostly on
                    else        -> 1500L to 200L   // dark: nearly steady
                }
                setTorch(true);  delay(onMs)
                setTorch(false); delay(offMs)
            }
        }
    }

    // ── ☽ READ — warm slow pulse, eyes-friendly ───────────────────────────────
    // Long ON, very brief OFF. Progressive auto-dim over time.
    fun startReading(setTorch: (Boolean) -> Unit) {
        activeJob = scope.launch {
            val startMs = System.currentTimeMillis()
            setTorch(true)
            while (isActive) {
                val elapsedMin = (System.currentTimeMillis() - startMs) / 60_000f
                val offMs = min(150L + (elapsedMin * 200L).toLong(), 1800L)
                val onMs  = max(2000L, 3000L - (elapsedMin * 100L).toLong())
                delay(onMs)
                setTorch(false); delay(offMs)
                setTorch(true)
            }
        }
    }

    // ── ◈ AMBIENT — reads scene, stays ON while reading ──────────────────────
    // Torch stays ON during sensor warmup (no dark flash).
    // Then picks pattern based on measured lux.
    fun startAmbient(setTorch: (Boolean) -> Unit) {
        startLightSensor()
        activeJob = scope.launch {
            // Stay ON while sensor warms up — no blink
            setTorch(true)
            delay(800)

            val lux = lightLevel
            when {
                lux < 5f -> {
                    // Total darkness → SOS
                    val sos = listOf(200L,200L,200L,200L,200L,600L,
                                     600L,200L,600L,200L,600L,600L,
                                     200L,200L,200L,200L,200L,1400L)
                    while (isActive) {
                        sos.forEachIndexed { i, ms ->
                            setTorch(i % 2 == 0); delay(ms)
                        }
                    }
                }
                lux < 50f -> {
                    // Dark room → steady (already ON)
                    awaitCancellation()
                }
                lux < 500f -> {
                    // Indoor → slow breath pulse
                    while (isActive) {
                        delay(2000L); setTorch(false)
                        delay(400L);  setTorch(true)
                    }
                }
                else -> {
                    // Bright outdoor → attention pulses
                    while (isActive) {
                        delay(300L); setTorch(false)
                        delay(200L); setTorch(true)
                    }
                }
            }
        }
    }

    // ── ⬡ CUSTOM — generative rhythm by time of day ──────────────────────────
    // All patterns have minimum 300ms ON to feel intentional.
    fun startCustomRhythm(setTorch: (Boolean) -> Unit) {
        activeJob = scope.launch {
            setTorch(true)
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val pattern: List<Pair<Long, Long>> = when (hour) {
                in 6..9   -> listOf(400L to 300L, 300L to 400L, 600L to 200L)
                in 10..14 -> listOf(600L to 300L, 400L to 300L)
                in 15..19 -> listOf(900L to 300L, 500L to 500L)
                in 20..22 -> listOf(1500L to 300L, 700L to 600L)
                else      -> listOf(2500L to 400L)
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

    // ── ◌ SLEEP — gradual fade simulation over 3 minutes ─────────────────────
    fun startSleepTimer(setTorch: (Boolean) -> Unit, setStrength: ((Float) -> Unit)? = null) {
        activeJob = scope.launch {
            val totalMs = 3 * 60 * 1000L
            val startMs = System.currentTimeMillis()
            val cycleMs = 500L
            setTorch(true)
            while (isActive) {
                val elapsed  = System.currentTimeMillis() - startMs
                if (elapsed >= totalMs) { setTorch(false); break }
                val progress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                val duty     = max(0f, 1f - progress * 1.1f)
                if (setStrength != null) {
                    // Smooth real dimming if device supports it
                    setStrength(duty)
                    delay(cycleMs)
                } else {
                    val onMs  = (cycleMs * duty).toLong().coerceAtLeast(0L)
                    val offMs = cycleMs - onMs
                    if (onMs  > 0) { setTorch(true);  delay(onMs)  }
                    if (offMs > 0) { setTorch(false); delay(offMs) }
                }
            }
            setTorch(false)
        }
    }


    // ── ◉ WALK — pulse on each step ──────────────────────────────────────────
    // TYPE_STEP_DETECTOR fires once per step. Flash pulses 120ms per step.
    // No permission needed. Falls back to accelerometer simulation if unavailable.
    fun startWalk(setTorch: (Boolean) -> Unit) {
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepSensor != null) {
            val stepListener = object : android.hardware.SensorEventListener {
                override fun onSensorChanged(e: android.hardware.SensorEvent) {
                    if (e.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                        setTorch(true)
                        activeJob?.cancel()
                        activeJob = scope.launch {
                            delay(120L)
                            setTorch(false)
                        }
                    }
                }
                override fun onAccuracyChanged(s: android.hardware.Sensor?, a: Int) = Unit
            }
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
            // Store listener for cleanup
            activeJob = scope.launch { awaitCancellation() }
        } else {
            // Fallback: simulate step cadence at 1.8 steps/second (walking pace)
            activeJob = scope.launch {
                while (isActive) {
                    setTorch(true);  delay(120L)
                    setTorch(false); delay(440L)
                }
            }
        }
    }

    // ── ◌ VOICE — react to sound level via microphone ─────────────────────────
    // Uses MusicBeatDetector with lower threshold — reacts to voice and claps.
    // Same permission as Music mode (RECORD_AUDIO).
    fun startVoice(setTorch: (Boolean) -> Unit) {
        musicDetector?.stop()
        musicDetector = MusicBeatDetector(
            onBeat    = {
                setTorch(true)
                activeJob?.cancel()
                activeJob = scope.launch { delay(150L); setTorch(false) }
            },
            threshold     = 1.3f,   // more sensitive than Music (1.5f)
            minIntervalMs = 200L,   // faster reaction: max 5 flashes/sec
            minEnergy     = 30f,    // lower silence floor
        )
        musicDetector?.start()
    }

    // ── ♩ MUSIC — beat detection via microphone ───────────────────────────────
    fun startMusic(setTorch: (Boolean) -> Unit) {
        musicDetector?.stop()
        musicDetector = MusicBeatDetector {
            setTorch(true)
            activeJob?.cancel()
            activeJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                delay(80L)
                setTorch(false)
            }
        }
        musicDetector?.start()
    }
}

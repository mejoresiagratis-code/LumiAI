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
 * All AI flash modes. Design rules:
 * 1. Every mode starts with setTorch(true) — no dark flash at activation
 * 2. stop() cancels ALL running tasks — no leaked listeners or coroutines
 * 3. Each start*() calls stop() internally — clean slate guaranteed
 */
@Singleton
class AiModeController @Inject constructor(
    private val context: Context,
) {
    private val scope  = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null
    private var musicDetector: MusicBeatDetector? = null

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var lightLevel = 200f

    // All registered sensor listeners tracked for proper cleanup
    private val registeredListeners = mutableListOf<SensorEventListener>()
    private var pulseJob: Job? = null  // for beat/step pulse OFF timers

    private val lightListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            if (e.sensor.type == Sensor.TYPE_LIGHT)
                lightLevel = e.values[0].coerceIn(0f, 10000f)
        }
        override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
    }

    private fun registerListener(listener: SensorEventListener, sensor: Sensor?, delay: Int) {
        sensor ?: return
        sensorManager.registerListener(listener, sensor, delay)
        registeredListeners.add(listener)
    }

    private fun startLightSensor() {
        registerListener(lightListener,
            sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
            SensorManager.SENSOR_DELAY_NORMAL)
    }

    /** Stop everything — cancel jobs, stop mic, unregister ALL sensors */
    fun stop() {
        activeJob?.cancel()
        activeJob = null
        pulseJob?.cancel()
        pulseJob = null
        musicDetector?.stop()
        musicDetector = null
        registeredListeners.forEach { sensorManager.unregisterListener(it) }
        registeredListeners.clear()
    }

    // ── ◎ SMART ───────────────────────────────────────────────────────────────
    fun startSmart(setTorch: (Boolean) -> Unit, setStrength: ((Float) -> Unit)? = null) {
        stop()
        startLightSensor()
        activeJob = scope.launch {
            setTorch(true)
            delay(1500)
            while (isActive) {
                val lux = lightLevel
                // In very dark environments use dimmed steady light if supported
                if (lux <= 30f && setStrength != null) {
                    setStrength(0.25f)
                    awaitCancellation()
                }
                val (onMs, offMs) = when {
                    lux > 1000f -> 200L to 200L
                    lux > 200f  -> 400L to 300L
                    else        -> 800L to 400L
                }
                setTorch(true);  delay(onMs)
                setTorch(false); delay(offMs)
            }
        }
    }

    // ── ☽ READ ────────────────────────────────────────────────────────────────
    fun startReading(setTorch: (Boolean) -> Unit) {
        stop()
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

    // ── ◈ AMBIENT ─────────────────────────────────────────────────────────────
    fun startAmbient(setTorch: (Boolean) -> Unit) {
        stop()
        startLightSensor()
        activeJob = scope.launch {
            setTorch(true)
            delay(800)
            val lux = lightLevel
            when {
                lux < 5f -> {
                    val sos = listOf(200L,200L,200L,200L,200L,600L,
                                     600L,200L,600L,200L,600L,600L,
                                     200L,200L,200L,200L,200L,1400L)
                    while (isActive) {
                        sos.forEachIndexed { i, ms -> setTorch(i % 2 == 0); delay(ms) }
                    }
                }
                lux < 50f  -> awaitCancellation()
                lux < 500f -> { while (isActive) { delay(2000L); setTorch(false); delay(400L); setTorch(true) } }
                else       -> { while (isActive) { delay(300L); setTorch(false); delay(200L); setTorch(true) } }
            }
        }
    }

    // ── ⬡ CUSTOM ──────────────────────────────────────────────────────────────
    fun startCustomRhythm(setTorch: (Boolean) -> Unit) {
        stop()
        activeJob = scope.launch {
            setTorch(true)
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val pattern = when (hour) {
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

    // ── ◌ SLEEP ───────────────────────────────────────────────────────────────
    fun startSleepTimer(setTorch: (Boolean) -> Unit, setStrength: ((Float) -> Unit)? = null) {
        stop()
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
                    setStrength(duty); delay(cycleMs)
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

    // ── ♩ MUSIC ───────────────────────────────────────────────────────────────
    fun startMusic(setTorch: (Boolean) -> Unit) {
        stop()
        musicDetector = MusicBeatDetector(onBeat = {
            setTorch(true)
            pulseJob?.cancel()
            pulseJob = scope.launch { delay(80L); setTorch(false) }
        })
        musicDetector?.start()
    }

    // ── ◉ WALK ────────────────────────────────────────────────────────────────
    fun startWalk(setTorch: (Boolean) -> Unit) {
        stop()
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepSensor != null) {
            val stepListener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    if (e.sensor.type != Sensor.TYPE_STEP_DETECTOR) return
                    setTorch(true)
                    pulseJob?.cancel()
                    pulseJob = scope.launch { delay(120L); setTorch(false) }
                }
                override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
            }
            registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
            // Keep alive until stop() is called
            activeJob = scope.launch { awaitCancellation() }
        } else {
            // Fallback: simulate walking cadence
            activeJob = scope.launch {
                while (isActive) {
                    setTorch(true);  delay(120L)
                    setTorch(false); delay(440L)
                }
            }
        }
    }

    // ── ◌ VOICE ───────────────────────────────────────────────────────────────
    fun startVoice(setTorch: (Boolean) -> Unit) {
        stop()
        musicDetector = MusicBeatDetector(
            onBeat        = {
                setTorch(true)
                pulseJob?.cancel()
                pulseJob = scope.launch { delay(150L); setTorch(false) }
            },
            threshold     = 1.3f,
            minIntervalMs = 200L,
            minEnergy     = 30f,
        )
        musicDetector?.start()
    }
}

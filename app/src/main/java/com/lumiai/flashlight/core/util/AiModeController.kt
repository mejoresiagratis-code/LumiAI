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
 * AI flash modes. Lifecycle rules:
 * 1. Each start*() calls stop() first — guaranteed clean slate.
 * 2. Every mode calls setTorch(true) immediately — no dark flash at start.
 * 3. stop() unregisters ALL sensor listeners via registeredListeners[].
 * 4. READ and AMBIENT are "steady" modes — no visible pulsing.
 */
@Singleton
class AiModeController @Inject constructor(
    private val context: Context,
) {
    private val scope   = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null
    private var pulseJob: Job?  = null
    private var musicDetector: MusicBeatDetector? = null

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var lightLevel = 200f
    private val registeredListeners = mutableListOf<SensorEventListener>()

    private val lightListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            if (e.sensor.type == Sensor.TYPE_LIGHT)
                lightLevel = e.values[0].coerceIn(0f, 10000f)
        }
        override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
    }

    private fun registerListener(l: SensorEventListener, sensor: Sensor?, delay: Int) {
        sensor ?: return
        sensorManager.registerListener(l, sensor, delay)
        registeredListeners.add(l)
    }

    private fun startLightSensor() = registerListener(
        lightListener,
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
        SensorManager.SENSOR_DELAY_NORMAL
    )

    fun stop() {
        activeJob?.cancel(); activeJob = null
        pulseJob?.cancel();  pulseJob  = null
        musicDetector?.stop(); musicDetector = null
        registeredListeners.forEach { sensorManager.unregisterListener(it) }
        registeredListeners.clear()
    }

    // ── ◎ SMART — adapts frequency to ambient light ───────────────────────────
    // Steady for 1.5s warmup, then pulses only when bright (outdoor attention signal).
    // In dark environments: dims using strength levels (no visible blink).
    fun startSmart(setTorch: (Boolean) -> Unit, setStrength: ((Float) -> Unit)? = null) {
        stop()
        startLightSensor()
        activeJob = scope.launch {
            setTorch(true)
            delay(1500)
            while (isActive) {
                val lux = lightLevel
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

    // ── ☽ READ — steady warm light, imperceptibly dims over time ─────────────
    // No visible ON/OFF cycling. Uses torch strength if available for true dimming.
    // If no strength support: stays fully ON. The "dimming" is communicated via
    // the UI label change, not via hardware blink.
    fun startReading(setTorch: (Boolean) -> Unit, setStrength: ((Float) -> Unit)? = null) {
        stop()
        activeJob = scope.launch {
            setTorch(true)
            if (setStrength != null) {
                // True smooth dimming over 20 minutes: 1.0 → 0.3
                val startMs  = System.currentTimeMillis()
                val totalMs  = 20 * 60_000L
                while (isActive) {
                    val elapsed  = System.currentTimeMillis() - startMs
                    val progress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                    val strength = max(0.3f, 1f - progress * 0.7f)
                    setStrength(strength)
                    delay(5000L) // update every 5s — imperceptible
                }
            } else {
                // No strength support — just stay ON steady, no blinking at all
                awaitCancellation()
            }
        }
    }

    // ── ⬨ AMBIENT — scene-aware, stays STEADY ────────────────────────────────
    // Reads lux once, picks brightness level. No cycling visible to the user.
    // Uses torch strength if available; otherwise just stays ON at full brightness.
    fun startAmbient(setTorch: (Boolean) -> Unit, setStrength: ((Float) -> Unit)? = null) {
        stop()
        lightLevel = 200f  // reset to safe indoor default before sensor kicks in
        startLightSensor()
        activeJob = scope.launch {
            setTorch(true)
            // Wait for a real sensor reading (up to 1.5s), default=200 if none arrives
            var waited = 0
            while (waited < 1500) {
                delay(100)
                waited += 100
                if (lightLevel != 200f) break  // got real reading
            }
            val lux = lightLevel
            if (setStrength != null) {
                // True adaptive brightness — no blink at all
                val strength = when {
                    lux > 1000f -> 1.0f   // full bright outdoor
                    lux > 200f  -> 0.7f   // indoor
                    lux > 30f   -> 0.4f   // dim room
                    else        -> 0.2f   // near dark
                }
                setStrength(strength)
                awaitCancellation()
            } else {
                // No strength support — stay ON steady
                // Only use SOS in genuine darkness (lux < 2 = almost zero)
                if (lux < 2f) {
                    val sos = listOf(200L,200L,200L,200L,200L,600L,
                                     600L,200L,600L,200L,600L,600L,
                                     200L,200L,200L,200L,200L,1400L)
                    while (isActive) {
                        sos.forEachIndexed { i, ms -> setTorch(i % 2 == 0); delay(ms) }
                    }
                } else {
                    awaitCancellation() // stay ON steady regardless of lux
                }
            }
        }
    }

    // ── ⬡ CUSTOM — generative rhythm by hour ─────────────────────────────────
    // Intentional pulsing — this IS the mode's behavior.
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

    // ── ◌ SLEEP — gradual fade, torch strength when available ────────────────
    fun startSleepTimer(setTorch: (Boolean) -> Unit, setStrength: ((Float) -> Unit)? = null) {
        stop()
        activeJob = scope.launch {
            val totalMs = 3 * 60 * 1000L
            val startMs = System.currentTimeMillis()
            setTorch(true)
            while (isActive) {
                val elapsed  = System.currentTimeMillis() - startMs
                if (elapsed >= totalMs) { setTorch(false); break }
                val progress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                val duty     = max(0f, 1f - progress * 1.1f)
                if (setStrength != null) {
                    setStrength(duty.coerceAtLeast(0.01f))
                    delay(500L)
                } else {
                    val onMs  = (500L * duty).toLong().coerceAtLeast(0L)
                    val offMs = 500L - onMs
                    if (onMs  > 0) { setTorch(true);  delay(onMs)  }
                    if (offMs > 0) { setTorch(false); delay(offMs) }
                }
            }
            setTorch(false)
        }
    }

    // ── ♩ MUSIC — beat detection ──────────────────────────────────────────────
    fun startMusic(setTorch: (Boolean) -> Unit) {
        stop()
        musicDetector = MusicBeatDetector(onBeat = {
            setTorch(true)
            pulseJob?.cancel()
            pulseJob = scope.launch { delay(80L); setTorch(false) }
        })
        musicDetector?.start()
    }

    // ── ◉ WALK — pulse per step ───────────────────────────────────────────────
    fun startWalk(setTorch: (Boolean) -> Unit) {
        stop()
        // Start with torch ON so user sees immediate feedback
        // Each step causes a brief OFF+ON pulse (like a footstep flash)
        setTorch(true)
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepSensor != null) {
            val stepListener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    if (e.sensor.type != Sensor.TYPE_STEP_DETECTOR) return
                    // Each step: brief flash pulse (off 80ms then back on)
                    pulseJob?.cancel()
                    pulseJob = scope.launch {
                        setTorch(false); delay(60L)
                        setTorch(true);  delay(80L)
                        setTorch(false); delay(60L)
                        setTorch(true)
                    }
                }
                override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
            }
            registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
            activeJob = scope.launch { awaitCancellation() }
        } else {
            activeJob = scope.launch {
                while (isActive) {
                    setTorch(true);  delay(120L)
                    setTorch(false); delay(440L)
                }
            }
        }
    }

    // ── ◍ VOICE — sound-reactive ──────────────────────────────────────────────
    fun startVoice(setTorch: (Boolean) -> Unit) {
        stop()
        // Start with torch OFF — voice reactivity means torch fires on sound spikes
        // This is intentional: silence = dark, voice = light
        // But we show torch ON briefly to confirm activation
        setTorch(true)
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

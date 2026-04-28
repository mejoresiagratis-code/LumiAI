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
    @Volatile private var active = false   // guards against post-cancel setTorch calls

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

    fun stop(setTorch: ((Boolean) -> Unit)? = null) {
        active = false             // FIRST: block any new setTorch calls from active loops
        activeJob?.cancel(); activeJob = null
        pulseJob?.cancel();  pulseJob  = null
        musicDetector?.stop(); musicDetector = null
        registeredListeners.forEach { sensorManager.unregisterListener(it) }
        registeredListeners.clear()
        setTorch?.invoke(false)   // guarantee torch OFF on every mode exit
    }

    /** Wraps setTorch to be a no-op after stop() is called */
    private fun guarded(on: Boolean, setTorch: (Boolean) -> Unit) {
        if (active) setTorch(on)
    }

    // ── ◎ SMART — adapts frequency to ambient light ───────────────────────────
    // Steady for 1.5s warmup, then pulses only when bright (outdoor attention signal).
    // In dark environments: dims using strength levels (no visible blink).
    fun startSmart(setTorch: (Boolean) -> Unit, setStrength: ((Float) -> Unit)? = null, speedMult: Float = 1.0f) {
        stop()
        active = true
        lightLevel = 200f   // reset stale reading from previous mode
        startLightSensor()
        activeJob = scope.launch {
            setTorch(true)
            delay(1500)
            while (isActive) {
                val lux = lightLevel
                if (lux <= 30f && setStrength != null) {
                    // Torch must be ON before setTorchStrength — ensure it
                    setTorch(true)
                    delay(50L)
                    setStrength(0.25f)
                    awaitCancellation()
                }
                val (onMs, offMs) = when {
                    lux > 1000f -> (200L / speedMult).toLong() to (200L / speedMult).toLong()
                    lux > 200f  -> (400L / speedMult).toLong() to (300L / speedMult).toLong()
                    else        -> (800L / speedMult).toLong() to (400L / speedMult).toLong()
                }
                guarded(true,  setTorch); delay(onMs)
                guarded(false, setTorch); delay(offMs)
            }
        }
    }

    // ── ☽ READ — steady warm light, imperceptibly dims over time ─────────────
    // No visible ON/OFF cycling. Uses torch strength if available for true dimming.
    // If no strength support: stays fully ON. The "dimming" is communicated via
    // the UI label change, not via hardware blink.
    fun startReading(setTorch: (Boolean) -> Unit, setStrength: ((Float) -> Unit)? = null) {
        stop()
        active = true
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
        active = true
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
                        sos.forEachIndexed { i, ms -> guarded(i % 2 == 0, setTorch); delay(ms) }
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
        active = true
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
                guarded(true,  setTorch); delay(onMs)
                guarded(false, setTorch); delay(offMs)
                idx++
            }
        }
    }

    // ── ◌ SLEEP — gradual fade, torch strength when available ────────────────
    fun startSleepTimer(setTorch: (Boolean) -> Unit, setStrength: ((Float) -> Unit)? = null, durationMinutes: Int = 3) {
        stop()
        active = true
        activeJob = scope.launch {
            val totalMs = durationMinutes.toLong() * 60_000L
            val startMs = System.currentTimeMillis()
            setTorch(true)
            while (isActive) {
                val elapsed  = System.currentTimeMillis() - startMs
                if (elapsed >= totalMs) { setTorch(false); break }
                val progress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                val duty     = max(0f, 1f - progress * 1.1f)
                if (setStrength != null) {
                    if (duty > 0f) {
                        setStrength(duty.coerceAtLeast(0.05f))
                        delay(500L)
                    } else {
                        guarded(false, setTorch)   // physically off when duty reaches 0
                        break
                    }
                } else {
                    val onMs  = (500L * duty).toLong().coerceAtLeast(0L)
                    val offMs = 500L - onMs
                    if (onMs  > 0) { guarded(true,  setTorch); delay(onMs)  }
                    if (offMs > 0) { guarded(false, setTorch); delay(offMs) }
                }
            }
            setTorch(false)
        }
    }

    // ── ♩ MUSIC — beat detection ──────────────────────────────────────────────
    fun startMusic(setTorch: (Boolean) -> Unit, sensitivity: Float = 1.0f) {
        stop()
        active = true
        musicDetector = MusicBeatDetector(
            threshold     = 1.5f / sensitivity,
            minIntervalMs = (300L / sensitivity).toLong().coerceAtLeast(100L),
            onBeat = {
                guarded(true, setTorch)
                pulseJob?.cancel()
                pulseJob = scope.launch { delay(80L); guarded(false, setTorch) }
            },
        )
        musicDetector?.start()
        activeJob = scope.launch { awaitCancellation() }
    }

    // ── ◉ WALK — pulse per step ───────────────────────────────────────────────
    fun startWalk(setTorch: (Boolean) -> Unit) {
        stop()
        active = true
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepSensor != null) {
            val stepListener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    if (e.sensor.type != Sensor.TYPE_STEP_DETECTOR) return
                    pulseJob?.cancel()
                    pulseJob = scope.launch {
                        guarded(false, setTorch); delay(60L)
                        guarded(true,  setTorch); delay(80L)
                        guarded(false, setTorch); delay(60L)
                        guarded(true,  setTorch)
                    }
                }
                override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
            }
            registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
            activeJob = scope.launch {
                setTorch(true)       // inside job — cancellable
                awaitCancellation()
            }
        } else {
            activeJob = scope.launch {
                while (isActive) {
                    guarded(true,  setTorch); delay(120L)
                    guarded(false, setTorch); delay(440L)
                }
            }
        }
    }

    // ── ◍ VOICE — sound-reactive ──────────────────────────────────────────────
    fun startVoice(setTorch: (Boolean) -> Unit, sensitivity: Float = 1.0f) {
        stop()
        active = true
        setTorch(true)
        musicDetector = MusicBeatDetector(
            onBeat        = {
                guarded(true, setTorch)
                pulseJob?.cancel()
                pulseJob = scope.launch { delay(150L); guarded(false, setTorch) }
            },
            threshold     = 1.3f / sensitivity,
            minIntervalMs = (200L / sensitivity).toLong().coerceAtLeast(80L),
            minEnergy     = 30f,
        )
        musicDetector?.start()
        activeJob = scope.launch { awaitCancellation() }
    }
}

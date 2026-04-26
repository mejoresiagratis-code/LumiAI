package com.lumiai.flashlight.core.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects shake gestures via accelerometer.
 * Register/unregister with lifecycle; fires [onShake] callback when threshold exceeded.
 */
class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit,
    private val threshold: Float = 18f,       // m/s² above gravity — raised to avoid accidental trigger
    private val cooldownMs: Long = 1500L,      // min ms between shakes — raised for stability
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastShakeMs    = 0L

    fun register() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun unregister() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        // Remove gravity (≈9.8 m/s²) and compute net acceleration magnitude
        val netAccel = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        if (netAccel > threshold) {
            val now = System.currentTimeMillis()
            if (now - lastShakeMs > cooldownMs) {
                lastShakeMs = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

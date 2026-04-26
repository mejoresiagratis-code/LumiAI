package com.lumiai.flashlight.service

import android.content.Context
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls flash pulses for notification alerts.
 * Uses camera2 directly — works even when the app is in background.
 */
@Singleton
class NotificationFlashController @Inject constructor(
    context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var flashJob: Job? = null
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    var isEnabled: Boolean = false
    var enabledForCalls: Boolean    = true
    var enabledForMessages: Boolean = true
    var enabledForOther: Boolean    = false

    private val backCameraId: String? by lazy {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.LENS_FACING) ==
                android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
        }
    }

    fun flash(pattern: NotificationPattern) {
        // Check per-type toggle
        val allowed = when (pattern) {
            NotificationPattern.CALL    -> enabledForCalls
            NotificationPattern.MESSAGE -> enabledForMessages
            NotificationPattern.OTHER   -> enabledForOther
        }
        if (!allowed) return

        // Pulses: CALL=3 fast, MESSAGE=2 medium, OTHER=1 short
        val pulses: List<Pair<Long, Long>> = when (pattern) {
            NotificationPattern.CALL    -> listOf(150L to 150L, 150L to 150L, 150L to 300L)
            NotificationPattern.MESSAGE -> listOf(200L to 200L, 200L to 500L)
            NotificationPattern.OTHER   -> listOf(150L to 50L)
        }

        flashJob?.cancel()
        flashJob = scope.launch {
            try {
                pulses.forEach { (onMs, offMs) ->
                    torch(true);  delay(onMs)
                    torch(false); if (offMs > 0) delay(offMs)
                }
            } catch (e: Exception) {
                runCatching { torch(false) }
            }
        }
    }

    private fun torch(on: Boolean) {
        try { backCameraId?.let { cameraManager.setTorchMode(it, on) } }
        catch (e: Exception) { /* camera busy */ }
    }
}

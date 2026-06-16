package com.lumiai.flashlight.core.torch

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Real [TorchHardware] backed by camera2 [CameraManager].
 *
 * Uses [CameraManager.registerTorchCallback] as the source of truth for the LED state:
 * onTorchModeChanged fires for ANY torch change in the process — including the app's own
 * CameraX enableTorch() in the foreground — so observers always see the real state.
 */
class Camera2TorchHardware(context: Context) : TorchHardware {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val handler = Handler(Looper.getMainLooper())
    private var torchCallback: CameraManager.TorchCallback? = null

    override val backCameraId: String? by lazy {
        runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                val c = cameraManager.getCameraCharacteristics(id)
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK &&
                    (c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true)
            }
        }.getOrNull()
    }

    override val maxStrengthLevel: Int by lazy {
        if (Build.VERSION.SDK_INT < 33) return@lazy 1
        runCatching {
            backCameraId?.let { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
            } ?: 1
        }.getOrDefault(1)
    }

    override fun setTorchMode(on: Boolean) {
        runCatching { backCameraId?.let { cameraManager.setTorchMode(it, on) } }
    }

    override fun setTorchStrength(level: Int) {
        if (Build.VERSION.SDK_INT < 33 || maxStrengthLevel <= 1) {
            setTorchMode(level > 0)
            return
        }
        runCatching {
            backCameraId?.let { id ->
                cameraManager.turnOnTorchWithStrengthLevel(id, level.coerceIn(1, maxStrengthLevel))
            }
        }.onFailure { setTorchMode(level > 0) }
    }

    override fun registerCallback(onChanged: (String, Boolean) -> Unit) {
        unregisterCallback()
        val cb = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == backCameraId) onChanged(cameraId, enabled)
            }
        }
        torchCallback = cb
        // Handler overload works from minSdk 23; OS delivers current state on register.
        runCatching { cameraManager.registerTorchCallback(cb, handler) }
    }

    override fun unregisterCallback() {
        torchCallback?.let { cb -> runCatching { cameraManager.unregisterTorchCallback(cb) } }
        torchCallback = null
    }
}

package com.lumiai.flashlight.core.data.repository

import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.core.util.StrobeController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class FlashRepositoryImpl constructor(
    @ApplicationContext private val context: Context,
    private val strobeController: StrobeController,
) : FlashRepository {

    private val _isFlashOn     = MutableStateFlow(false)
    private val _currentMode   = MutableStateFlow<FlashMode>(FlashMode.Steady)
    private val _hasHardwareFlash = MutableStateFlow(false)

    override val isFlashOn: StateFlow<Boolean>       = _isFlashOn
    override val currentMode: StateFlow<FlashMode>   = _currentMode
    override val hasHardwareFlash: StateFlow<Boolean> = _hasHardwareFlash

    private var camera: androidx.camera.core.Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    init {
        _hasHardwareFlash.value = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    /**
     * Bind CameraX. Must be called once from a LifecycleOwner (MainActivity).
     */
    suspend fun bindCamera(lifecycleOwner: LifecycleOwner) {
        val provider = getCameraProvider()
        val imageCapture = ImageCapture.Builder().build()
        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(lifecycleOwner, selector, imageCapture)
        } catch (e: Exception) {
            // Device has no back camera — Screen mode will be used as fallback
        }
    }

    override suspend fun activateMode(mode: FlashMode): Result<Unit> = runCatching {
        strobeController.stop()
        _currentMode.value = mode

        when (mode) {
            is FlashMode.Steady -> setTorch(true)
            is FlashMode.Screen -> { setTorch(false); _isFlashOn.value = true } // Screen handled in UI layer
            is FlashMode.Sos    -> strobeController.startSos { setTorch(it) }
            is FlashMode.Strobe -> strobeController.startStrobe(mode.hz) { setTorch(it) }
            is FlashMode.Disco  -> strobeController.startDisco(mode.bpm) { setTorch(it) }
            else                -> setTorch(true) // Pro modes activate torch + AI layer handles rest
        }
    }

    override suspend fun turnOff(): Result<Unit> = runCatching {
        strobeController.stop()
        setTorch(false)
        _isFlashOn.value = false
        _currentMode.value = FlashMode.Steady
    }

    override fun release() {
        strobeController.stop()
        cameraProvider?.unbindAll()
        camera = null
    }

    private fun setTorch(on: Boolean) {
        camera?.cameraControl?.enableTorch(on)
        _isFlashOn.value = on
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider =
        cameraProvider ?: suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                val provider = future.get()
                cameraProvider = provider
                cont.resume(provider)
            }, ContextCompat.getMainExecutor(context))
        }
}

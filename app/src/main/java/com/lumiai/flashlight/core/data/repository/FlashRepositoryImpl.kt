package com.lumiai.flashlight.core.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.core.util.AiModeController
import com.lumiai.flashlight.core.util.StrobeController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FlashRepositoryImpl constructor(
    private val context: Context,
    private val strobeController: StrobeController,
    private val aiController: AiModeController,
) : FlashRepository {

    private val _isFlashOn        = MutableStateFlow(false)
    private val _currentMode      = MutableStateFlow<FlashMode>(FlashMode.Steady)
    private val _hasHardwareFlash = MutableStateFlow(false)
    private val _isCameraReady    = MutableStateFlow(false)

    override val isFlashOn:        StateFlow<Boolean>   = _isFlashOn
    override val currentMode:      StateFlow<FlashMode> = _currentMode
    override val hasHardwareFlash: StateFlow<Boolean>   = _hasHardwareFlash
    val isCameraReady:             StateFlow<Boolean>   = _isCameraReady

    private var cameraXCamera:   androidx.camera.core.Camera? = null
    private var cameraProvider:  ProcessCameraProvider? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Fallback: CameraManager (camera2) for torch without preview
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val backCameraId: String? by lazy {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.LENS_FACING) ==
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
        }
    }

    init {
        _hasHardwareFlash.value = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    /**
     * Bind CameraX to the Activity lifecycle.
     * Called from MainActivity.onCreate — makes torch available via CameraControl.
     * Falls back gracefully if binding fails (e.g. camera in use by another app).
     */
    suspend fun bindCamera(lifecycleOwner: LifecycleOwner) {
        try {
            val provider = getCameraProvider()
            val imageCapture = ImageCapture.Builder().build()
            provider.unbindAll()
            cameraXCamera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                imageCapture,
            )
            _isCameraReady.value = true
        } catch (e: Exception) {
            // CameraX failed — camera2 fallback still available
            _isCameraReady.value = false
        }
    }

    override suspend fun activateMode(mode: FlashMode): Result<Unit> = runCatching {
        strobeController.stop()
        aiController.stop()
        _currentMode.value = mode

        when (mode) {
            is FlashMode.Steady -> setTorch(true)
            is FlashMode.Screen -> {
                // Screen mode: no hardware flash, just signal UI to go white
                setTorch(false)
                _isFlashOn.value = true
            }
            is FlashMode.Sos    -> strobeController.startSos { setTorch(it) }
            is FlashMode.Strobe -> strobeController.startStrobe(mode.hz) { setTorch(it) }
            is FlashMode.Disco  -> strobeController.startDisco(mode.bpm) { setTorch(it) }
            is FlashMode.SmartBrightness -> {
                setTorch(false) // aiController manages torch directly
                aiController.startSmart { setTorch(it) }
            }
            is FlashMode.ReadingMode -> {
                setTorch(false)
                aiController.startReading { setTorch(it) }
            }
            is FlashMode.AmbientSmart -> {
                setTorch(false)
                aiController.startAmbient { setTorch(it) }
            }
            is FlashMode.CustomRhythm -> {
                setTorch(false)
                aiController.startCustomRhythm { setTorch(it) }
            }
            is FlashMode.SleepTimer -> {
                setTorch(false)
                aiController.startSleepTimer { setTorch(it) }
            }
            else -> setTorch(true)
        }
    }

    override suspend fun turnOff(): Result<Unit> = runCatching {
        strobeController.stop()
        aiController.stop()
        setTorch(false)
        _isFlashOn.value = false
        _currentMode.value = FlashMode.Steady
    }

    override fun release() {
        strobeController.stop()
        aiController.stop()
        runCatching { setTorchCamera2(false) }
        cameraProvider?.unbindAll()
        cameraXCamera = null
        _isCameraReady.value = false
    }

    /**
     * Set torch on/off. Must always run on main thread for CameraX.
     * Falls back to CameraManager (camera2) if CameraX not bound.
     */
    private fun setTorch(on: Boolean) {
        _isFlashOn.value = on
        if (Looper.myLooper() == Looper.getMainLooper()) {
            setTorchOnMain(on)
        } else {
            mainHandler.post { setTorchOnMain(on) }
        }
    }

    private fun setTorchOnMain(on: Boolean) {
        val cameraX = cameraXCamera
        if (cameraX != null) {
            // CameraX path — preferred
            cameraX.cameraControl.enableTorch(on)
        } else {
            // camera2 fallback — works even without CameraX binding
            setTorchCamera2(on)
        }
    }

    private fun setTorchCamera2(on: Boolean) {
        try {
            backCameraId?.let { id -> cameraManager.setTorchMode(id, on) }
        } catch (e: Exception) {
            // Camera in use or no flash — silent fail
        }
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider =
        cameraProvider ?: suspendCancellableCoroutine { cont ->
            ProcessCameraProvider.getInstance(context).addListener({
                runCatching {
                    val provider = ProcessCameraProvider.getInstance(context).get()
                    cameraProvider = provider
                    cont.resume(provider)
                }
            }, ContextCompat.getMainExecutor(context))
        }
}

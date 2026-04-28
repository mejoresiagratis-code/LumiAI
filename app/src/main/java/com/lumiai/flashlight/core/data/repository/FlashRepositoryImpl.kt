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

    // AI config providers — set by FlashViewModel after construction
    var torchIntensityProvider: (() -> Float)? = null
    var smartSpeedProvider:    (() -> Float)? = null
    var sleepMinutesProvider:  (() -> Int)?   = null
    var micSensitivityProvider:(() -> Float)? = null

    // Torch strength support (API 33+)
    val maxTorchStrength: Int get() = if (android.os.Build.VERSION.SDK_INT >= 33) {
        try {
            backCameraId?.let { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
            } ?: 1
        } catch (e: Exception) { 1 }
    } else 1

    val supportsTorchStrength: Boolean get() = maxTorchStrength > 1
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
            // Only rebind if not already bound — unbindAll kills the torch
            if (cameraXCamera == null) {
                val imageCapture = ImageCapture.Builder().build()
                provider.unbindAll()
                cameraXCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    imageCapture,
                )
            }
            _isCameraReady.value = true
        } catch (e: Exception) {
            _isCameraReady.value = false
        }
    }

    override suspend fun activateMode(mode: FlashMode): Result<Unit> {
        val result = runCatching {
        strobeController.stop { on -> if (on) setTorch(true) else mainHandler.post { setTorchOnMain(false) }; Unit }
        aiController.stop { on -> if (on) setTorch(true) else mainHandler.post { setTorchOnMain(false) }; Unit }
        _currentMode.value = mode

        when (mode) {
            is FlashMode.Steady -> {
                val intensity = torchIntensityProvider?.invoke() ?: 1.0f
                if (intensity >= 0.99f || !supportsTorchStrength) {
                    setTorch(true)
                } else {
                    _isFlashOn.value = true
                    mainHandler.post { setTorchStrength(intensity) }
                }
            }
            is FlashMode.Screen -> {
                // Screen mode: UI-only flash — torch hardware stays OFF.
                // Do NOT call setTorch() at all — it overwrites _isFlashOn.
                // Just set the flag directly; the UI reacts to isFlashOn=true.
                _isFlashOn.value = true
                // Ensure torch is physically off without touching _isFlashOn
                mainHandler.post { setTorchOnMain(false) }
            }
            is FlashMode.Sos        -> strobeController.startSos { setTorch(it) }
            is FlashMode.MorseCustom -> {
                if (mode.text.isNotBlank())
                    strobeController.startMorse(mode.text) { setTorch(it) }
                else
                    setTorch(true) // no text yet — steady until user types
            }
            is FlashMode.Strobe -> strobeController.startStrobe(mode.hz, { setTorch(it) }, torchIntensityProvider?.invoke() ?: 1.0f)
            is FlashMode.Disco  -> strobeController.startDisco(mode.bpm, { setTorch(it) }, torchIntensityProvider?.invoke() ?: 1.0f)
            is FlashMode.SmartBrightness -> {
                _isFlashOn.value = true
                aiController.startSmart(
                    setTorch    = { setTorch(it) },
                    setStrength = if (supportsTorchStrength) { level -> setTorchStrength(level) } else null,
                    speedMult   = smartSpeedProvider?.invoke() ?: 1.0f,
                )
            }
            is FlashMode.ReadingMode -> {
                _isFlashOn.value = true
                aiController.startReading(
                    setTorch    = { setTorch(it) },
                    setStrength = if (supportsTorchStrength) { level -> setTorchStrength(level) } else null,
                )
            }
            is FlashMode.AmbientSmart -> {
                _isFlashOn.value = true
                aiController.startAmbient(
                    setTorch    = { setTorch(it) },
                    setStrength = if (supportsTorchStrength) { level -> setTorchStrength(level) } else null,
                )
            }
            is FlashMode.CustomRhythm -> {
                _isFlashOn.value = true
                aiController.startCustomRhythm { setTorch(it) }
            }
            is FlashMode.SleepTimer -> {
                _isFlashOn.value = true
                aiController.startSleepTimer(
                    setTorch      = { setTorch(it) },
                    setStrength   = if (supportsTorchStrength) { level -> setTorchStrength(level) } else null,
                    durationMinutes = sleepMinutesProvider?.invoke() ?: 3,
                )
            }
            is FlashMode.Music -> {
                _isFlashOn.value = true
                aiController.startMusic(
                    setTorch    = { setTorch(it) },
                    sensitivity = micSensitivityProvider?.invoke() ?: 1.0f,
                )
            }
            is FlashMode.Walk -> {
                _isFlashOn.value = true
                aiController.startWalk { setTorch(it) }
            }
            is FlashMode.Voice -> {
                _isFlashOn.value = true
                aiController.startVoice(
                    setTorch    = { setTorch(it) },
                    sensitivity = micSensitivityProvider?.invoke() ?: 1.0f,
                )
            }
        }
        Unit   // explicit Unit return — ensures Result<Unit> not Result<Any>
        }   // end runCatching
        if (result.isFailure) {
            // Controller threw — guarantee clean state so UI isn't stuck ON
            _isFlashOn.value = false
            mainHandler.post { setTorchOnMain(false) }
        }
        return result
    }

    override suspend fun turnOff(): Result<Unit> = runCatching {
        strobeController.stop()   // no setTorch here — setTorch(false) follows immediately
        aiController.stop()       // same — avoids double hardware write
        setTorch(false)
        _isFlashOn.value = false
        // NOTE: do NOT reset _currentMode — user's selection persists after OFF
    }

    /** Change the current mode without activating flash (used when flash is OFF) */
    /**
     * Sets torch to a specific brightness level (0.0–1.0).
     * Only available on API 33+ devices with multi-level torch support.
     * Falls back to regular on/off on unsupported devices.
     */
    fun setTorchStrength(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        if (android.os.Build.VERSION.SDK_INT >= 33 && supportsTorchStrength) {
            try {
                backCameraId?.let { id ->
                    val strength = (clamped * maxTorchStrength).toInt().coerceAtLeast(1)
                    cameraManager.turnOnTorchWithStrengthLevel(id, strength)
                    _isFlashOn.value = clamped > 0f
                }
            } catch (e: Exception) {
                setTorch(clamped > 0.5f) // fallback
            }
        } else {
            setTorch(clamped > 0.5f)
        }
    }

    /**
     * Re-applies the torch ON/OFF state after a camera lifecycle interruption.
     * Called from MainActivity.onResume to restore steady modes (Ambient, Read, Smart)
     * that were killed by bindCamera/unbindAll during a pause cycle.
     */
    fun restoreTorchIfNeeded() {
        if (_isFlashOn.value) {
            val mode = _currentMode.value
            when (mode) {
                is FlashMode.Screen -> { /* screen mode — no torch needed */ }
                else -> {
                    // For all torch modes: re-enable hardware torch
                    // The AiModeController job is still running (awaitCancellation),
                    // we just need to re-enable the hardware torch.
                    mainHandler.post { setTorchOnMain(true) }
                }
            }
        }
    }

    override fun setCurrentMode(mode: FlashMode) {
        _currentMode.value = mode
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
            // CameraX path — use exclusively when session is open
            // camera2.setTorchMode conflicts with open CameraX session on Samsung
            cameraX.cameraControl.enableTorch(on)
        } else {
            // CameraX not bound yet (first launch race condition) — retry after bind
            // Only fall back to camera2 if CameraX never binds
            mainHandler.postDelayed({
                val cameraX2 = cameraXCamera
                if (cameraX2 != null) {
                    cameraX2.cameraControl.enableTorch(on)
                } else {
                    // True fallback: CameraX unavailable, use camera2 directly
                    setTorchCamera2(on)
                }
            }, 300L)
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

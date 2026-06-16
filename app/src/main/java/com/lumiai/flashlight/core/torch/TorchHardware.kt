package com.lumiai.flashlight.core.torch

/**
 * Thin abstraction over the camera2 torch surface so [TorchController]'s
 * state/serialization logic can be unit-tested with a fake (no real CameraManager).
 */
interface TorchHardware {

    /** Back-camera id that has a flash unit, or null if none is available. */
    val backCameraId: String?

    /** Max discrete strength level (1 == device has no multi-level support). */
    val maxStrengthLevel: Int

    /** Turn the LED on/off (camera2 setTorchMode). Implementations must not throw. */
    fun setTorchMode(on: Boolean)

    /** Set absolute strength level in 1..maxStrengthLevel. Implementations must not throw. */
    fun setTorchStrength(level: Int)

    /**
     * Register an OS torch-state listener. The callback fires with the CURRENT state
     * immediately on registration, and again whenever the LED changes by ANY actor
     * (this app via CameraX/camera2, the widget, the system camera, etc.).
     */
    fun registerCallback(onChanged: (cameraId: String, enabled: Boolean) -> Unit)

    fun unregisterCallback()
}

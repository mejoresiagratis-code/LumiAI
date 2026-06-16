package com.lumiai.flashlight.core.torch

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Single source of truth for the PHYSICAL torch state across the whole process.
 *
 * - [torchState] mirrors the OS torch callback (the real LED) — never a cached boolean
 *   or a request value. This is what fixes the widget desync (REL-T2).
 * - All actuations go through a [Mutex] so concurrent callers (app, widget, notifications)
 *   can't race the camera (REL-T3).
 * - [dispatcher] is a constructor parameter (defaulted) so the logic stays unit-testable
 *   with Dispatchers.Unconfined (android-kotlin: inject dispatchers).
 */
class TorchController(
    private val hardware: TorchHardware,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val mutex = Mutex()
    private val _torchState = MutableStateFlow(false)
    val torchState: StateFlow<Boolean> = _torchState.asStateFlow()

    init {
        // Fires immediately with the current state, then on every real change by any actor.
        hardware.registerCallback { _, enabled -> _torchState.value = enabled }
    }

    suspend fun setEnabled(on: Boolean) = mutex.withLock {
        withContext(dispatcher) { hardware.setTorchMode(on) }
    }

    suspend fun setStrength(level: Float) = mutex.withLock {
        withContext(dispatcher) {
            val scaled = (level.coerceIn(0f, 1f) * hardware.maxStrengthLevel)
                .toInt().coerceAtLeast(1)
            hardware.setTorchStrength(scaled)
        }
    }
}

package com.lumiai.flashlight.core.data.repository

import com.lumiai.flashlight.core.domain.model.FlashMode
import kotlinx.coroutines.flow.StateFlow

interface FlashRepository {
    val isFlashOn: StateFlow<Boolean>
    val currentMode: StateFlow<FlashMode>
    val hasHardwareFlash: StateFlow<Boolean>

    suspend fun activateMode(mode: FlashMode): Result<Unit>
    suspend fun turnOff(): Result<Unit>
    fun setCurrentMode(mode: FlashMode)
    fun release()
}

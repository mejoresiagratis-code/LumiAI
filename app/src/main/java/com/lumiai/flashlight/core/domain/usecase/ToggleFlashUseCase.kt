package com.lumiai.flashlight.core.domain.usecase

import com.lumiai.flashlight.core.data.repository.FlashRepository
import com.lumiai.flashlight.core.domain.model.FlashMode
import javax.inject.Inject

/**
 * Activates or deactivates a flash mode.
 * Validates Pro restriction before delegating to repository.
 */
class ToggleFlashUseCase @Inject constructor(
    private val flashRepository: FlashRepository,
) {
    suspend operator fun invoke(mode: FlashMode, isPro: Boolean): Result<Unit> {
        if (mode.isPro && !isPro) {
            return Result.failure(ProRequiredException(mode))
        }
        return flashRepository.activateMode(mode)
    }

    suspend fun turnOff() = flashRepository.turnOff()
}

class ProRequiredException(val mode: FlashMode) : Exception("Mode ${mode.id} requires Pro")

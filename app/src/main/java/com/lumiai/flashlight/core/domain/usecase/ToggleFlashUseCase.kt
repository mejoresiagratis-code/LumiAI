package com.lumiai.flashlight.core.domain.usecase

import com.lumiai.flashlight.core.data.repository.FlashRepository
import com.lumiai.flashlight.core.domain.model.FlashMode
import javax.inject.Inject

/**
 * Activates or deactivates a flash mode.
 * Pro restriction is DISABLED until payment is implemented.
 * All modes are accessible regardless of Pro status.
 */
class ToggleFlashUseCase @Inject constructor(
    private val flashRepository: FlashRepository,
) {
    suspend operator fun invoke(mode: FlashMode, isPro: Boolean): Result<Unit> {
        // TODO: re-enable Pro restriction when billing is live
        // if (mode.isPro && !isPro) return Result.failure(ProRequiredException(mode))
        return flashRepository.activateMode(mode)
    }

    suspend fun turnOff() = flashRepository.turnOff()
}

// Kept for future use when billing is re-enabled
class ProRequiredException(val mode: FlashMode) : Exception("Mode ${mode.id} requires Pro")

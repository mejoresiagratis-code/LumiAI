package com.lumiai.flashlight.core.domain.usecase

import com.lumiai.flashlight.core.data.repository.FlashRepository
import com.lumiai.flashlight.core.domain.model.FlashMode
import javax.inject.Inject

/**
 * Activates or deactivates a flash mode.
 * Pro modes are gated: a non-Pro user requesting a Pro mode receives a
 * ProRequiredException so the UI can present the paywall.
 */
class ToggleFlashUseCase @Inject constructor(
    private val flashRepository: FlashRepository,
) {
    suspend operator fun invoke(mode: FlashMode, isPro: Boolean): Result<Unit> {
        if (mode.isPro && !isPro) return Result.failure(ProRequiredException(mode))
        return flashRepository.activateMode(mode)
    }

    suspend fun turnOff() = flashRepository.turnOff()
}

// Kept for future use when billing is re-enabled
class ProRequiredException(val mode: FlashMode) : Exception("Mode ${mode.id} requires Pro")

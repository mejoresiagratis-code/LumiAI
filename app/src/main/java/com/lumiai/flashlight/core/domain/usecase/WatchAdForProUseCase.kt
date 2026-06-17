package com.lumiai.flashlight.core.domain.usecase

import com.lumiai.flashlight.core.data.repository.RewardedProRepository
import com.lumiai.flashlight.core.data.repository.RewardedState
import javax.inject.Inject

/**
 * Called after a rewarded ad is fully watched.
 * Updates the escalation counters and grants Pro if the threshold is reached.
 * Returns the new [RewardedState] so the ViewModel can react immediately.
 */
class WatchAdForProUseCase @Inject constructor(
    private val rewardedProRepository: RewardedProRepository,
) {
    suspend operator fun invoke(): RewardedState =
        rewardedProRepository.onAdWatched()
}

package com.lumiai.flashlight.core.domain.usecase

import com.lumiai.flashlight.core.data.repository.BillingRepository
import com.lumiai.flashlight.core.domain.model.ProStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProStatusUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    operator fun invoke(): Flow<ProStatus> = billingRepository.proStatusFlow
}

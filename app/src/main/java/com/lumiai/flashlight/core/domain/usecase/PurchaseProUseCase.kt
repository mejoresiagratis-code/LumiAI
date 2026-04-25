package com.lumiai.flashlight.core.domain.usecase

import android.app.Activity
import com.lumiai.flashlight.core.data.repository.BillingRepository
import javax.inject.Inject

class PurchaseProUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    suspend operator fun invoke(activity: Activity): Result<Unit> =
        billingRepository.launchPurchaseFlow(activity)
}

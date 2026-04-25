package com.lumiai.flashlight.core.data.repository

import android.app.Activity
import com.lumiai.flashlight.core.domain.model.ProStatus
import kotlinx.coroutines.flow.Flow

interface BillingRepository {
    val proStatusFlow: Flow<ProStatus>
    suspend fun launchPurchaseFlow(activity: Activity): Result<Unit>
    suspend fun restorePurchases(): Result<Unit>
}

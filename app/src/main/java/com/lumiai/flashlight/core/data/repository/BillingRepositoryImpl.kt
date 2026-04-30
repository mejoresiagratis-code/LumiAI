package com.lumiai.flashlight.core.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.lumiai.flashlight.core.domain.model.ProStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val PRO_PRODUCT_ID = "pro_unlock"

@Singleton
class BillingRepositoryImpl constructor(
    @ApplicationContext private val context: Context,
) : BillingRepository, PurchasesUpdatedListener {

    private val _proStatus = MutableStateFlow<ProStatus>(ProStatus.Loading)
    override val proStatusFlow: StateFlow<ProStatus> = _proStatus

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init { connectAndCheck() }

    private fun connectAndCheck() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    checkExistingPurchases()
                } else {
                    _proStatus.value = ProStatus.Free
                }
            }
            override fun onBillingServiceDisconnected() {
                _proStatus.value = ProStatus.Free
            }
        })
    }

    private fun checkExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            val hasPro = result.responseCode == BillingClient.BillingResponseCode.OK &&
                    purchases.any { it.products.contains(PRO_PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            _proStatus.value = if (hasPro) ProStatus.Pro else ProStatus.Free
        }
    }

    override suspend fun launchPurchaseFlow(activity: Activity): Result<Unit> = runCatching {
        // Query product details first
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        suspendCancellableCoroutine { cont ->
            billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                    val productDetail = productDetailsList.first()
                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetail)
                                    .build()
                            )
                        )
                        .build()
                    billingClient.launchBillingFlow(activity, billingFlowParams)
                }
                cont.resume(Unit)
            }
        }
    }

    override suspend fun restorePurchases(): Result<Unit> = runCatching {
        if (billingClient.isReady) {
            // Client connected — query immediately
            checkExistingPurchases()
        } else {
            // Client disconnected (app resumed after long background, etc.)
            // Re-establish connection then query. connectAndCheck() handles both.
            connectAndCheck()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            val hasPro = purchases.any {
                it.products.contains(PRO_PRODUCT_ID) &&
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (hasPro) {
                _proStatus.value = ProStatus.Pro
                // Acknowledge purchase (required by Play, else auto-refunded after 3 days)
                purchases.filter { !it.isAcknowledged }.forEach { purchase ->
                    billingClient.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                    ) { /* fire and forget */ }
                }
            }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // No-op
        } else {
            _proStatus.value = ProStatus.Error("Billing error: ${result.debugMessage}")
        }
    }
}

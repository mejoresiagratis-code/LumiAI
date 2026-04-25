package com.lumiai.flashlight.core.di

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.*
import com.lumiai.flashlight.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var interstitialAd: InterstitialAd? = null
    private var adsInitialized = false

    /**
     * Show UMP consent form (required for GDPR), then initialize AdMob.
     * Must be called from Activity context (UMP requires it).
     */
    suspend fun initWithConsent(activity: Activity) = suspendCancellableCoroutine { cont ->
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo.requestConsentInfoUpdate(activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // ConsentForm dismissed or not needed
                    if (consentInfo.canRequestAds()) {
                        initAdMob(activity)
                    }
                    if (cont.isActive) cont.resume(Unit)
                }
            },
            { _ ->
                // Error fetching consent — still allow ads if previous consent given
                if (consentInfo.canRequestAds()) initAdMob(activity)
                if (cont.isActive) cont.resume(Unit)
            }
        )
    }

    private fun initAdMob(activity: Activity) {
        if (adsInitialized) return
        MobileAds.initialize(activity) {
            adsInitialized = true
            preloadInterstitial()
        }
    }

    fun preloadInterstitial() {
        if (!adsInitialized) return
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) { interstitialAd = null }
            }
        )
    }

    /** Show interstitial if loaded. After dismiss, pre-load next one. */
    fun showInterstitialIfReady(activity: Activity) {
        interstitialAd?.apply {
            fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { preloadInterstitial() }
            }
            show(activity)
        }
        interstitialAd = null
    }

    fun isInitialized() = adsInitialized
}

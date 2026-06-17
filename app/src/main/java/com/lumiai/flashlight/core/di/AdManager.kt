package com.lumiai.flashlight.core.di

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
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
    private var rewardedAd: RewardedAd?         = null
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
                    if (consentInfo.canRequestAds()) initAdMob(activity)
                    if (cont.isActive) cont.resume(Unit)
                }
            },
            { _ ->
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
            preloadRewarded()
        }
    }

    // ── Interstitial ────────────────────────────────────────────────────────

    fun preloadInterstitial() {
        if (!adsInitialized) return
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd)              { interstitialAd = ad }
                override fun onAdFailedToLoad(e: LoadAdError) { interstitialAd = null }
            }
        )
    }

    fun showInterstitialIfReady(activity: Activity) {
        interstitialAd?.apply {
            fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { preloadInterstitial() }
            }
            show(activity)
        }
        interstitialAd = null
    }

    // ── Rewarded ────────────────────────────────────────────────────────────

    fun preloadRewarded() {
        if (!adsInitialized) return
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd)              { rewardedAd = ad }
                override fun onAdFailedToLoad(e: LoadAdError) { rewardedAd = null }
            }
        )
    }

    fun isRewardedReady(): Boolean = rewardedAd != null

    /**
     * Show rewarded ad. Calls [onRewarded] if the user earns the reward (fully watched).
     * Calls [onDismissed] either way (after reward or skip — use to unblock UI).
     * Pre-loads next ad automatically after dismiss.
     */
    fun showRewarded(
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissed: () -> Unit,
        onFailed: (String) -> Unit,
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onFailed("Anuncio no disponible todavía. Inténtalo de nuevo.")
            preloadRewarded()
            return
        }
        rewardedAd = null   // clear immediately — don't show twice

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadRewarded()
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) {
                preloadRewarded()
                onFailed(e.message)
                onDismissed()
            }
        }
        ad.show(activity) { _ ->
            // RewardItem received — user earned the reward
            onRewarded()
        }
    }

    fun isInitialized() = adsInitialized
}

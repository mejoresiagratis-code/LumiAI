package com.lumiai.flashlight.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.lumiai.flashlight.BuildConfig

/**
 * Adaptive banner ad — shown only on Free tier.
 * Uses AndroidView to bridge AdMob's View-based SDK into Compose.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory  = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        ctx,
                        getScreenWidthDp(ctx),
                    ))
                    adUnitId = BuildConfig.ADMOB_BANNER_ID
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = { adView ->
                // Re-load if view is recycled
                adView.loadAd(AdRequest.Builder().build())
            },
        )
    }
}

private fun getScreenWidthDp(context: Context): Int {
    val displayMetrics = context.resources.displayMetrics
    return (displayMetrics.widthPixels / displayMetrics.density).toInt()
}

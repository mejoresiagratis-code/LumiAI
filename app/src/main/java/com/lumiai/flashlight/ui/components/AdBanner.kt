package com.lumiai.flashlight.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
 * The AdView is created and loaded ONCE (remembered across recompositions)
 * and destroyed when it leaves composition. It must not be reloaded on every
 * recomposition — that wastes impressions and risks AdMob rate/policy limits.
 * Periodic refresh, if wanted, should be configured server-side in AdMob.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adView = remember {
        AdView(context).apply {
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    context,
                    getScreenWidthDp(context),
                )
            )
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(Unit) {
        onDispose { adView.destroy() }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory  = { adView },
        )
    }
}

private fun getScreenWidthDp(context: Context): Int {
    val displayMetrics = context.resources.displayMetrics
    return (displayMetrics.widthPixels / displayMetrics.density).toInt()
}

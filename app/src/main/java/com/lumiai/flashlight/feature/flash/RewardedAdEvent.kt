package com.lumiai.flashlight.feature.flash

/** Events emitted by FlashViewModel for the Activity to handle (rewarded ads need Activity). */
sealed class RewardedAdEvent {
    object ShowAd : RewardedAdEvent()
}

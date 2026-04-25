package com.lumiai.flashlight

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 * Hilt generates the DI component here.
 * AdMob is intentionally NOT initialized here — we wait for UMP consent.
 */
@HiltAndroidApp
class LumiAIApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Firebase init (Crashlytics + Analytics)
        FirebaseApp.initializeApp(this)
    }
}

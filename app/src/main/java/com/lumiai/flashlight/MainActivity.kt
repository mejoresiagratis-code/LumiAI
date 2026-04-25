package com.lumiai.flashlight

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.lumiai.flashlight.core.di.AdManager
import com.lumiai.flashlight.ui.theme.LumiAITheme
import com.lumiai.flashlight.feature.flash.FlashViewModel
import com.lumiai.flashlight.ui.navigation.LumiNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val flashViewModel: FlashViewModel by viewModels()

    @Inject lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // ── Splash screen (must be first) ──────────────────────────────────
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !flashViewModel.isReady.value }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on while app is in foreground (flashlight use case)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Consent + AdMob init (GDPR-compliant; must happen before any ad load)
        lifecycleScope.launch {
            adManager.initWithConsent(this@MainActivity)
        }

        setContent {
            LumiAITheme {
                LumiNavHost()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release CameraX / flash resources if activity is destroyed
        flashViewModel.releaseCamera()
    }
}

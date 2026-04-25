package com.lumiai.flashlight

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.lumiai.flashlight.core.data.repository.FlashRepositoryImpl
import com.lumiai.flashlight.core.di.AdManager
import com.lumiai.flashlight.core.util.ShakeDetector
import com.lumiai.flashlight.feature.flash.FlashViewModel
import com.lumiai.flashlight.ui.navigation.LumiNavHost
import com.lumiai.flashlight.ui.theme.LumiAITheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val flashViewModel: FlashViewModel by viewModels()

    @Inject lateinit var adManager: AdManager
    @Inject lateinit var flashRepository: FlashRepositoryImpl

    private lateinit var shakeDetector: ShakeDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !flashViewModel.isReady.value }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Shake detector — toggles flash if enabled in settings
        shakeDetector = ShakeDetector(this) {
            if (flashViewModel.uiState.value.shakeToToggle) {
                flashViewModel.toggleFlash()
            }
        }

        lifecycleScope.launch {
            flashRepository.bindCamera(this@MainActivity)
            adManager.initWithConsent(this@MainActivity)
        }

        setContent {
            LumiAITheme {
                LumiNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shakeDetector.register()
    }

    override fun onPause() {
        super.onPause()
        shakeDetector.unregister()
    }

    override fun onDestroy() {
        super.onDestroy()
        flashViewModel.releaseCamera()
    }
}

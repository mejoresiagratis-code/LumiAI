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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !flashViewModel.isReady.value }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lifecycleScope.launch {
            // Bind CameraX to this lifecycle — gives flash control
            flashRepository.bindCamera(this@MainActivity)
            // Init AdMob after UMP consent (GDPR)
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
        flashViewModel.releaseCamera()
    }
}

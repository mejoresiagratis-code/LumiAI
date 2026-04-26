package com.lumiai.flashlight

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.lumiai.flashlight.core.data.repository.FlashRepositoryImpl
import com.lumiai.flashlight.core.data.repository.SettingsRepository
import com.lumiai.flashlight.core.di.AdManager
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.core.util.ShakeDetector
import com.lumiai.flashlight.feature.flash.FlashViewModel
import com.lumiai.flashlight.ui.navigation.LumiNavHost
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lumiai.flashlight.ui.theme.LumiAITheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val flashViewModel: FlashViewModel by viewModels()

    @Inject lateinit var adManager: AdManager
    @Inject lateinit var flashRepository: FlashRepositoryImpl
    @Inject lateinit var settingsRepository: SettingsRepository

    private lateinit var shakeDetector: ShakeDetector

    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) bindCameraIfPermitted()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !flashViewModel.isReady.value }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        shakeDetector = ShakeDetector(context = this, onShake = {
            val state = flashViewModel.uiState.value
            if (state.shakeToToggle) {
                // Don't toggle steady AI modes (Read, Ambient) on shake —
                // these are designed to stay ON and accidental shake would kill them
                val isSteadyAiMode = state.currentMode is FlashMode.ReadingMode ||
                                     state.currentMode is FlashMode.AmbientSmart
                if (!isSteadyAiMode) {
                    flashViewModel.toggleFlash()
                }
            }
        })

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            bindCameraIfPermitted()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        // Mic permission — lazy on Music mode selection
        lifecycleScope.launch {
            flashViewModel.uiState.collect { state ->
                if (state.currentMode is FlashMode.Music &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }

        lifecycleScope.launch {
            adManager.initWithConsent(this@MainActivity)
        }

        setContent {
            // Collect dark theme preference from DataStore
            val settings by settingsRepository.settings
                .collectAsState(
                    initial = com.lumiai.flashlight.core.domain.model.UserSettings()
                )

            LumiAITheme(darkTheme = settings.isDarkTheme) {
                LumiNavHost()
            }
        }
    }

    private fun bindCameraIfPermitted() {
        lifecycleScope.launch { flashRepository.bindCamera(this@MainActivity) }
    }

    override fun onResume() {
        super.onResume()
        shakeDetector.register()
        if (!flashRepository.isCameraReady.value &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            bindCameraIfPermitted()
        }
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

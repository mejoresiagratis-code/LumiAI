package com.lumiai.flashlight

import android.Manifest
import com.lumiai.flashlight.core.domain.model.FlashMode
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

    // Runtime microphone permission (for Music beat detection mode)
    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Result handled by mode — if denied, Music mode will fail silently
        }

    // Runtime camera permission request
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
            if (flashViewModel.uiState.value.shakeToToggle) {
                flashViewModel.toggleFlash()
            }
        })

        // Request camera permission then bind
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            bindCameraIfPermitted()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        lifecycleScope.launch {
            adManager.initWithConsent(this@MainActivity)
        }

        // Observe mode changes to request mic permission lazily
        lifecycleScope.launch {
            flashViewModel.uiState.collect { state ->
                if (state.currentMode is FlashMode.Music) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.RECORD_AUDIO
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        }

        setContent {
            LumiAITheme {
                LumiNavHost()
            }
        }
    }

    private fun bindCameraIfPermitted() {
        lifecycleScope.launch {
            flashRepository.bindCamera(this@MainActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        shakeDetector.register()
        // Re-bind camera if it was released (e.g. another app used camera)
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

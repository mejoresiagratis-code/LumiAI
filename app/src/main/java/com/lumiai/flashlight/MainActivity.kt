package com.lumiai.flashlight

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import com.lumiai.flashlight.widget.FlashWidgetReceiver
import com.lumiai.flashlight.core.util.FirebaseManager
import com.lumiai.flashlight.core.util.LanguageManager
import com.lumiai.flashlight.feature.flash.FlashViewModel
import com.lumiai.flashlight.ui.navigation.LumiNavHost
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lumiai.flashlight.ui.theme.LumiAITheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("lumi_lang", android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString("app_language_override", "system") ?: "system"
        super.attachBaseContext(LanguageManager.wrap(newBase, lang))
    }

    private val flashViewModel: FlashViewModel by viewModels()

    @Inject lateinit var adManager: AdManager
    @Inject lateinit var flashRepository: FlashRepositoryImpl
    @Inject lateinit var settingsRepository: SettingsRepository

    private lateinit var shakeDetector: ShakeDetector

    // ── Permission launchers ──────────────────────────────────────────────────

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) bindCameraIfPermitted()
        }

    // RECORD_AUDIO — lazy: only asked when Music or Voice mode is selected
    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // POST_NOTIFICATIONS — Android 13+ (API 33).
    // Required to post flash-alert notifications. App works without it (feature just won't fire).
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // ACTIVITY_RECOGNITION — Android 10+ (API 29).
    // Required for TYPE_STEP_DETECTOR (Walk mode). Walk degrades to timer fallback if denied.
    private val requestActivityRecognitionPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !flashViewModel.isReady.value }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Screen kept on via FLAG_KEEP_SCREEN_ON; WAKE_LOCK manifest permission not needed
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        shakeDetector = ShakeDetector(context = this, onShake = {
            val state = flashViewModel.uiState.value
            if (state.shakeToToggle) {
                // Don't shake-toggle steady AI modes — they are designed to stay ON
                val isSteadyAiMode = state.currentMode is FlashMode.ReadingMode ||
                                     state.currentMode is FlashMode.AmbientSmart
                if (!isSteadyAiMode) flashViewModel.toggleFlash()
            }
        })

        // ── Camera — request immediately; torch is the core feature ──────────
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            bindCameraIfPermitted()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        // ── POST_NOTIFICATIONS — Android 13+ (API 33) ─────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // ── ACTIVITY_RECOGNITION — Android 10+ (API 29) ───────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            requestActivityRecognitionPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        // ── RECORD_AUDIO — lazy: only when Music or Voice is selected ─────
        lifecycleScope.launch {
            flashViewModel.uiState.collect { state ->
                val needsMic = state.currentMode is FlashMode.Music ||
                               state.currentMode is FlashMode.Voice
                if (needsMic && ContextCompat.checkSelfPermission(
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

        // ── Interstitial — show every 5 mode changes for Free users ──────────
        lifecycleScope.launch {
            flashViewModel.showInterstitialEvent.collect {
                adManager.showInterstitialIfReady(this@MainActivity)
            }
        }

        FirebaseManager.init(this)
        setContent {
            val settings by settingsRepository.settings
                .collectAsState(initial = com.lumiai.flashlight.core.domain.model.UserSettings())
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
        // Keep widget SharedPrefs in sync so widget toggle reads the correct state
        FlashWidgetReceiver.syncState(this, flashViewModel.uiState.value.isFlashOn)
        if (!flashRepository.isCameraReady.value &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            bindCameraIfPermitted()
        }
        val state = flashViewModel.uiState.value
        if (state.isFlashOn) {
            lifecycleScope.launch { flashRepository.restoreTorchIfNeeded() }
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

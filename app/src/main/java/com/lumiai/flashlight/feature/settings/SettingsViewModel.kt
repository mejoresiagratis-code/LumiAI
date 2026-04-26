package com.lumiai.flashlight.feature.settings

import com.lumiai.flashlight.BuildConfig

import android.app.Activity
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiai.flashlight.core.data.repository.BillingRepositoryImpl
import com.lumiai.flashlight.core.data.repository.FlashRepositoryImpl
import com.lumiai.flashlight.core.data.repository.SettingsRepository
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.core.domain.model.ProStatus
import com.lumiai.flashlight.core.domain.model.UserSettings
import com.lumiai.flashlight.core.domain.usecase.GetProStatusUseCase
import com.lumiai.flashlight.core.domain.usecase.PurchaseProUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val proStatus: ProStatus   = ProStatus.Loading,
    val appVersion: String     = BuildConfig.VERSION_NAME,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val flashRepository: FlashRepositoryImpl,
    private val billingRepository: BillingRepositoryImpl,
    private val getProStatusUseCase: GetProStatusUseCase,
    private val purchaseProUseCase: PurchaseProUseCase,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        getProStatusUseCase(),
    ) { settings, proStatus ->
        SettingsUiState(settings = settings, proStatus = proStatus)
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    // ── Strobe Hz ──────────────────────────────────────────────────────────
    fun setStrobeHz(hz: Float) {
        viewModelScope.launch {
            settingsRepository.updateStrobeHz(hz)
            // If currently in strobe mode, apply live
            if (flashRepository.currentMode.value is FlashMode.Strobe &&
                flashRepository.isFlashOn.value) {
                flashRepository.activateMode(FlashMode.Strobe(hz))
            }
        }
    }

    // ── Disco BPM ──────────────────────────────────────────────────────────
    fun setDiscoBpm(bpm: Float) {
        viewModelScope.launch {
            settingsRepository.updateDiscoBpm(bpm)
            if (flashRepository.currentMode.value is FlashMode.Disco &&
                flashRepository.isFlashOn.value) {
                flashRepository.activateMode(FlashMode.Disco(bpm))
            }
        }
    }

    // ── Screen brightness ──────────────────────────────────────────────────
    fun setScreenBrightness(activity: Activity?, brightness: Float) {
        viewModelScope.launch {
            settingsRepository.updateScreenBrightness(brightness)
            // Apply to window immediately if activity is available
            activity?.window?.attributes?.let { lp ->
                lp.screenBrightness = brightness
                activity.window.attributes = lp
            }
        }
    }

    // ── Shake to toggle ────────────────────────────────────────────────────
    fun setShakeToToggle(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShakeToToggle(enabled) }
    }

    // ── Keep screen on ─────────────────────────────────────────────────────
    fun setKeepScreenOn(activity: Activity?, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(enabled)
            activity?.window?.apply {
                if (enabled) addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // ── Dark theme ─────────────────────────────────────────────────────────
    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkTheme(dark) }
    }

    // ── Pro: purchase & restore ────────────────────────────────────────────
    fun purchasePro(activity: Activity) {
        viewModelScope.launch { purchaseProUseCase(activity) }
    }

    fun restorePurchases() {
        viewModelScope.launch { billingRepository.restorePurchases() }
    }
}

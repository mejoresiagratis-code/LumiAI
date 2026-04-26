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
import com.lumiai.flashlight.feature.flash.AutoOffOption
import com.lumiai.flashlight.service.NotificationFlashController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val notificationFlashController: NotificationFlashController,
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


    // ── Notification flash state ────────────────────────────────────────────
    private val _notifFlashEnabled  = MutableStateFlow(false)
    private val _notifFlashCalls    = MutableStateFlow(true)
    private val _notifFlashMessages = MutableStateFlow(true)
    private val _notifFlashOther    = MutableStateFlow(false)
    val notifFlashEnabled:  StateFlow<Boolean> = _notifFlashEnabled.asStateFlow()
    val notifFlashCalls:    StateFlow<Boolean> = _notifFlashCalls.asStateFlow()
    val notifFlashMessages: StateFlow<Boolean> = _notifFlashMessages.asStateFlow()
    val notifFlashOther:    StateFlow<Boolean> = _notifFlashOther.asStateFlow()

    fun setNotifFlashEnabled(v: Boolean)  { _notifFlashEnabled.value  = v; notificationFlashController.isEnabled  = v }
    fun setNotifFlashCalls(v: Boolean)    { _notifFlashCalls.value    = v; notificationFlashController.enabledForCalls    = v }
    fun setNotifFlashMessages(v: Boolean) { _notifFlashMessages.value = v; notificationFlashController.enabledForMessages = v }
    fun setNotifFlashOther(v: Boolean)    { _notifFlashOther.value    = v; notificationFlashController.enabledForOther    = v }

    // ── Auto-off timer ────────────────────────────────────────────────────
    private val _currentAutoOff = MutableStateFlow(AutoOffOption.NONE)
    val currentAutoOff: StateFlow<AutoOffOption> = _currentAutoOff.asStateFlow()

    fun setAutoOffTimer(option: AutoOffOption) {
        _currentAutoOff.value = option
        viewModelScope.launch { settingsRepository.setAutoOffMinutes(option.minutes) }
    }

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

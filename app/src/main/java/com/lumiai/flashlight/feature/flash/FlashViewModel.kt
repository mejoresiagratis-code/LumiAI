package com.lumiai.flashlight.feature.flash

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiai.flashlight.core.data.repository.FlashRepositoryImpl
import com.lumiai.flashlight.core.data.repository.SettingsRepository
import com.lumiai.flashlight.core.domain.model.UserSettings
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.core.domain.model.ProStatus
import com.lumiai.flashlight.core.domain.usecase.GetProStatusUseCase
import com.lumiai.flashlight.core.domain.usecase.PurchaseProUseCase
import com.lumiai.flashlight.core.domain.usecase.ToggleFlashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlashUiState(
    val isFlashOn: Boolean           = false,
    val currentMode: FlashMode       = FlashMode.Steady,
    val proStatus: ProStatus         = ProStatus.Loading,
    val hasHardwareFlash: Boolean    = false,
    val errorMessage: String?        = null,
    val showProPaywall: Boolean      = false,
    // Settings — read from DataStore
    val strobeHz: Float              = 5f,
    val discoBpm: Float              = 120f,
    val shakeToToggle: Boolean       = true,
)

@HiltViewModel
class FlashViewModel @Inject constructor(
    private val flashRepository: FlashRepositoryImpl,
    private val toggleFlashUseCase: ToggleFlashUseCase,
    private val getProStatusUseCase: GetProStatusUseCase,
    private val purchaseProUseCase: PurchaseProUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    val uiState: StateFlow<FlashUiState> = combine(
        flashRepository.isFlashOn,
        flashRepository.currentMode,
        getProStatusUseCase(),
        flashRepository.hasHardwareFlash,
        settingsRepository.settings,
    ) { args ->
        val isOn      = args[0] as Boolean
        val mode      = args[1] as FlashMode
        val proStatus = args[2] as ProStatus
        val hasFlash  = args[3] as Boolean
        val settings  = args[4] as com.lumiai.flashlight.core.domain.model.UserSettings
        FlashUiState(
            isFlashOn        = isOn,
            currentMode      = mode,
            proStatus        = proStatus,
            hasHardwareFlash = hasFlash,
            strobeHz         = settings.strobeHz,
            discoBpm         = settings.discoBpm,
            shakeToToggle    = settings.shakeToToggle,
        )
    }.stateIn(
        scope         = viewModelScope,
        started       = SharingStarted.WhileSubscribed(5_000),
        initialValue  = FlashUiState(),
    )

    init {
        viewModelScope.launch {
            // Mark ready once billing state resolves (splash screen can dismiss)
            getProStatusUseCase().first { it !is ProStatus.Loading }
            _isReady.value = true
        }
    }

    fun toggleFlash() {
        viewModelScope.launch {
            val state = uiState.value
            if (state.isFlashOn) {
                toggleFlashUseCase.turnOff()
            } else {
                // Force activate current mode (bypass isFlashOn guard in activateMode)
                val isPro = state.proStatus == com.lumiai.flashlight.core.domain.model.ProStatus.Pro
                toggleFlashUseCase(state.currentMode, isPro)
            }
        }
    }

    fun activateMode(mode: FlashMode) {
        viewModelScope.launch {
            val state = uiState.value
            val isPro = state.proStatus == ProStatus.Pro
            if (state.isFlashOn) {
                // Flash is ON → switch to new mode immediately
                toggleFlashUseCase(mode, isPro)
            } else {
                // Flash is OFF → just preview the mode (no flash, no blink)
                flashRepository.setCurrentMode(mode)
            }
            settingsRepository.updateLastMode(mode.id)
        }
    }

    fun showPaywall() {
        // TODO: emit paywall event via SharedFlow
    }

    fun purchasePro(activity: Activity) {
        viewModelScope.launch {
            purchaseProUseCase(activity)
        }
    }


    /**
     * Update strobe Hz from the main screen slider.
     * Persists to DataStore AND re-applies to the flash controller if currently strobing.
     * Does NOT call activateMode() — avoids currentMode StateFlow emission
     * which would destroy/recreate the LiveSlider composable mid-drag.
     */
    fun updateStrobeHz(hz: Float) {
        viewModelScope.launch {
            settingsRepository.updateStrobeHz(hz)
            // Apply live if currently in strobe mode with flash on
            val state = uiState.value
            if (state.currentMode is FlashMode.Strobe && state.isFlashOn) {
                flashRepository.activateMode(FlashMode.Strobe(hz))
            }
        }
    }

    /**
     * Update disco BPM from the main screen slider.
     * Same pattern as updateStrobeHz — persist + apply without mode change.
     */
    fun updateDiscoBpm(bpm: Float) {
        viewModelScope.launch {
            settingsRepository.updateDiscoBpm(bpm)
            val state = uiState.value
            if (state.currentMode is FlashMode.Disco && state.isFlashOn) {
                flashRepository.activateMode(FlashMode.Disco(bpm))
            }
        }
    }

    fun releaseCamera() {
        flashRepository.release()
    }
}

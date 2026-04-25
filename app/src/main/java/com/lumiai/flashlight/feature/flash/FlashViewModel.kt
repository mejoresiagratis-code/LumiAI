package com.lumiai.flashlight.feature.flash

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiai.flashlight.core.data.repository.FlashRepositoryImpl
import com.lumiai.flashlight.core.data.repository.SettingsRepository
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
    ) { isOn, mode, proStatus, hasFlash ->
        FlashUiState(
            isFlashOn         = isOn,
            currentMode       = mode,
            proStatus         = proStatus,
            hasHardwareFlash  = hasFlash,
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
                activateMode(state.currentMode)
            }
        }
    }

    fun activateMode(mode: FlashMode) {
        viewModelScope.launch {
            val isPro = uiState.value.proStatus == ProStatus.Pro
            toggleFlashUseCase(mode, isPro).onFailure { e ->
                if (e is com.lumiai.flashlight.core.domain.usecase.ProRequiredException) {
                    // Show paywall instead of error
                    showPaywall()
                }
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

    fun releaseCamera() {
        flashRepository.release()
    }
}

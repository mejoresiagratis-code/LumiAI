package com.lumiai.flashlight.feature.flash

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiai.flashlight.core.data.repository.FlashRepositoryImpl
import com.lumiai.flashlight.core.data.repository.SettingsRepository
import com.lumiai.flashlight.core.domain.model.UserSettings
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.feature.flash.AutoOffOption
import com.lumiai.flashlight.feature.flash.ScreenColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    // Settings — read from DataStore
    val strobeHz: Float              = 5f,
    val discoBpm: Float              = 120f,
    val shakeToToggle: Boolean       = true,
    val screenColor: ScreenColor     = ScreenColor.WHITE,
    val autoOffOption: AutoOffOption = AutoOffOption.NONE,
    val screenBrightness: Float    = 1f,
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

    // Auto-off timer
    private val _morseText = MutableStateFlow("")
    val morseText: StateFlow<String> = _morseText.asStateFlow()

    // Config bottom sheet
    private val _showConfigSheet = MutableStateFlow(false)
    val showConfigSheet: StateFlow<Boolean> = _showConfigSheet.asStateFlow()


    // ── AI mode config params ──────────────────────────────────────────────
    // Smart: pulse speed multiplier (0.5x slow … 2.0x fast)
    private val _smartSpeed = MutableStateFlow(1.0f)
    val smartSpeed: StateFlow<Float> = _smartSpeed.asStateFlow()

    // Sleep: fade duration in minutes (1, 3, 5, 10)
    private val _sleepMinutes = MutableStateFlow(3)
    val sleepMinutes: StateFlow<Int> = _sleepMinutes.asStateFlow()

    // Music / Voice: sensitivity (0.5 = less sensitive, 2.0 = very sensitive)
    private val _micSensitivity = MutableStateFlow(1.0f)
    val micSensitivity: StateFlow<Float> = _micSensitivity.asStateFlow()

    fun setSmartSpeed(v: Float) { _smartSpeed.value = v.coerceIn(0.5f, 2.0f) }
    fun setSleepMinutes(v: Int)  { _sleepMinutes.value = v }
    fun setMicSensitivity(v: Float) { _micSensitivity.value = v.coerceIn(0.5f, 2.0f) }

    fun openConfigSheet()  { _showConfigSheet.value = true  }
    fun closeConfigSheet() { _showConfigSheet.value = false }
    private val _currentScreenColor = MutableStateFlow(ScreenColor.WHITE)
    val screenColor: StateFlow<ScreenColor> = _currentScreenColor.asStateFlow()
    private val _autoOff = MutableStateFlow(AutoOffOption.NONE) // synced from DataStore on first settings load
    val autoOff: StateFlow<AutoOffOption> = _autoOff.asStateFlow()
    private var autoOffJob: Job? = null

    val uiState: StateFlow<FlashUiState> = combine(
        flashRepository.isFlashOn,
        flashRepository.currentMode,
        getProStatusUseCase(),
        settingsRepository.settings,
    ) { args ->
        val isOn      = args[0] as Boolean
        val mode      = args[1] as FlashMode
        val proStatus = args[2] as ProStatus
        val settings  = args[3] as com.lumiai.flashlight.core.domain.model.UserSettings
        FlashUiState(
            isFlashOn        = isOn,
            currentMode      = mode,
            proStatus        = proStatus,
            strobeHz         = settings.strobeHz,
            discoBpm         = settings.discoBpm,
            shakeToToggle    = settings.shakeToToggle,
            screenBrightness = settings.screenBrightness,
            screenColor      = _currentScreenColor.value,
            autoOffOption    = AutoOffOption.entries.firstOrNull { it.minutes == settings.autoOffMinutes } ?: AutoOffOption.NONE,
        )
    }.stateIn(
        scope         = viewModelScope,
        started       = SharingStarted.WhileSubscribed(5_000),
        initialValue  = FlashUiState(),
    )

    init {
        // Wire AI config providers to repository
        flashRepository.smartSpeedProvider     = { _smartSpeed.value }
        flashRepository.sleepMinutesProvider   = { _sleepMinutes.value }
        flashRepository.micSensitivityProvider = { _micSensitivity.value }

        viewModelScope.launch {
            getProStatusUseCase().first { it !is ProStatus.Loading }
            _isReady.value = true
        }
        // Restore last used mode from DataStore on startup
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val lastMode = FlashMode.all().firstOrNull { it.id == settings.lastMode }
            if (lastMode != null && lastMode !is FlashMode.Steady) {
                flashRepository.setCurrentMode(lastMode)
            }
        }
        // Sync auto-off timer from DataStore on startup
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val option = AutoOffOption.entries.firstOrNull {
                    it.minutes == settings.autoOffMinutes
                } ?: AutoOffOption.NONE
                if (_autoOff.value != option) {
                    _autoOff.value = option
                }
            }
        }
    }

    fun toggleFlash() {
        viewModelScope.launch {
            val state = uiState.value
            if (state.isFlashOn) {
                autoOffJob?.cancel()
                toggleFlashUseCase.turnOff()
            } else {
                val isPro = state.proStatus == com.lumiai.flashlight.core.domain.model.ProStatus.Pro
                toggleFlashUseCase(state.currentMode, isPro)
                scheduleAutoOff(_autoOff.value)
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


    /** Set auto-off timer. Restarts whenever flash is turned on. */
    fun setAutoOff(option: AutoOffOption) {
        _autoOff.value = option
        scheduleAutoOff(option)
    }

    private fun scheduleAutoOff(option: AutoOffOption) {
        autoOffJob?.cancel()
        autoOffJob = null
        if (option == AutoOffOption.NONE || !uiState.value.isFlashOn) return
        autoOffJob = viewModelScope.launch {
            delay(option.minutes * 60_000L)
            toggleFlashUseCase.turnOff()
        }
    }

    fun updateMorseText(text: String) {
        _morseText.value = text
        // Re-activate if Morse mode is currently on
        val state = uiState.value
        if (state.currentMode is com.lumiai.flashlight.core.domain.model.FlashMode.MorseCustom
            && state.isFlashOn) {
            viewModelScope.launch {
                flashRepository.activateMode(
                    com.lumiai.flashlight.core.domain.model.FlashMode.MorseCustom(text)
                )
            }
        }
    }

    fun setScreenColor(color: ScreenColor) {
        // Stored in memory only — no DataStore needed for session preference
        _currentScreenColor.value = color
    }

    fun setAutoOffFromSettings(option: AutoOffOption) {
        setAutoOff(option)
    }

    fun releaseCamera() {
        flashRepository.release()
    }
}

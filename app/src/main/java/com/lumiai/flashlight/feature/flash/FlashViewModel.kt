package com.lumiai.flashlight.feature.flash

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiai.flashlight.core.data.repository.FlashRepositoryImpl
import com.lumiai.flashlight.core.data.repository.SettingsRepository
import com.lumiai.flashlight.core.domain.model.UserSettings
import com.lumiai.flashlight.core.domain.model.FlashMode
import com.lumiai.flashlight.feature.flash.AutoOffOption
import com.lumiai.flashlight.core.util.StrobePattern
import com.lumiai.flashlight.feature.flash.ScreenColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.lumiai.flashlight.core.domain.model.ProStatus
import com.lumiai.flashlight.core.domain.usecase.GetProStatusUseCase
import com.lumiai.flashlight.core.domain.usecase.PurchaseProUseCase
import com.lumiai.flashlight.core.domain.usecase.ToggleFlashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Active animated effect in Screen mode. Null = static color. */
enum class ScreenEffect { CANDELA, POLICE, RAINBOW, STROBE }

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

    private val _showPaywallEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showPaywallEvent: SharedFlow<Unit> = _showPaywallEvent.asSharedFlow()

    // Auto-off timer
    private val _morseText = MutableStateFlow("")
    val morseText: StateFlow<String> = _morseText.asStateFlow()

    // Config bottom sheet
    private val _showConfigSheet = MutableStateFlow(false)
    val showConfigSheet: StateFlow<Boolean> = _showConfigSheet.asStateFlow()


    // ── Torch intensity (all modes) ────────────────────────────────────────────
    private val _torchIntensity = MutableStateFlow(1.0f)   // 0.1f (dim) .. 1.0f (full)
    val torchIntensity: StateFlow<Float> = _torchIntensity.asStateFlow()

    fun setTorchIntensity(v: Float) {
        val clamped = v.coerceIn(0.1f, 1.0f)
        _torchIntensity.value = clamped
        viewModelScope.launch {
            settingsRepository.setTorchIntensity(clamped)
            // Live-apply if Steady is currently ON
            val state = uiState.value
            if (state.currentMode is FlashMode.Steady && state.isFlashOn) {
                flashRepository.activateMode(FlashMode.Steady)
            }
        }
    }

    fun setScreenBrightness(brightness: Float) {
        viewModelScope.launch {
            settingsRepository.updateScreenBrightness(brightness)
            // Live-apply: update window brightness immediately if Screen mode is ON
            // The actual WindowManager update happens reactively via uiState.screenBrightness
            // which FlashScreen observes — no explicit hardware call needed here
        }
    }

    // ── Morse speed (WPM multiplier) ────────────────────────────────────────
    private val _morseSpeed = MutableStateFlow(1.0f)  // 0.5× slow .. 4.0× fast
    val morseSpeed: StateFlow<Float> = _morseSpeed.asStateFlow()
    fun setMorseSpeed(v: Float) {
        val clamped = v.coerceIn(0.5f, 4.0f)
        _morseSpeed.value = clamped
        viewModelScope.launch { settingsRepository.setMorseSpeed(clamped) }
    }

    // ── Screen text (LED scroller) ──────────────────────────────────────────
    private val _screenText = MutableStateFlow("")
    val screenText: StateFlow<String> = _screenText.asStateFlow()
    fun setScreenText(text: String) {
        _screenText.value = text
        viewModelScope.launch { settingsRepository.setScreenText(text) }
    }

    // ── Strobe pattern ──────────────────────────────────────────────────────
    private val _strobePattern = MutableStateFlow(StrobePattern.SINGLE)
    val strobePattern: StateFlow<StrobePattern> = _strobePattern.asStateFlow()
    fun setStrobePattern(p: StrobePattern) {
        _strobePattern.value = p
        // Re-apply immediately if currently strobing
        val state = uiState.value
        if (state.currentMode is FlashMode.Strobe && state.isFlashOn) {
            viewModelScope.launch {
                flashRepository.activateMode(FlashMode.Strobe(state.strobeHz))
            }
        }
    }

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
    fun setSleepMinutes(v: Int) {
        _sleepMinutes.value = v
        viewModelScope.launch { settingsRepository.setSleepMinutes(v) }
    }
    fun setMicSensitivity(v: Float) {
        val clamped = v.coerceIn(0.5f, 2.0f)
        _micSensitivity.value = clamped
        viewModelScope.launch { settingsRepository.setMicSensitivity(clamped) }
    }

    fun openConfigSheet()  { _showConfigSheet.value = true  }
    fun closeConfigSheet() { _showConfigSheet.value = false }
    private val _currentScreenColor = MutableStateFlow(ScreenColor.WHITE)
    val screenColor: StateFlow<ScreenColor> = _currentScreenColor.asStateFlow()

    // Screen mode — active animated effect (null = static)
    private val _screenEffect = MutableStateFlow<ScreenEffect?>(null)
    val screenEffect: StateFlow<ScreenEffect?> = _screenEffect.asStateFlow()

    // Screen mode — active tab index (0=Solid,1=Hue,2=Temp,3=FX)
    private val _screenTab = MutableStateFlow(0)
    val screenTab: StateFlow<Int> = _screenTab.asStateFlow()

    // Hue (0..360) and temperature (0..1 = 2700K..6500K)
    private val _screenHue = MutableStateFlow(180f)
    val screenHue: StateFlow<Float> = _screenHue.asStateFlow()
    private val _screenTemp = MutableStateFlow(0f)   // 0=2700K, 1=6500K
    val screenTemp: StateFlow<Float> = _screenTemp.asStateFlow()
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
        flashRepository.torchIntensityProvider  = { _torchIntensity.value }
        flashRepository.strobePatternProvider   = { _strobePattern.value }
        flashRepository.morseSpeedProvider     = { _morseSpeed.value }
        flashRepository.smartSpeedProvider     = { _smartSpeed.value }
        flashRepository.sleepMinutesProvider   = { _sleepMinutes.value }
        flashRepository.micSensitivityProvider = { _micSensitivity.value }

        viewModelScope.launch {
            getProStatusUseCase().first { it !is ProStatus.Loading }
            _isReady.value = true
        }
        // Restore last used mode from DataStore on startup (with saved config values)
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val lastMode: FlashMode? = when (settings.lastMode) {
                "strobe"       -> FlashMode.Strobe(settings.strobeHz)
                "disco"        -> FlashMode.Disco(settings.discoBpm)
                "morse_custom" -> FlashMode.MorseCustom(settings.morseText)  // restore with saved text
                else           -> FlashMode.all().firstOrNull { it.id == settings.lastMode }
            }
            if (lastMode != null && lastMode !is FlashMode.Steady) {
                flashRepository.setCurrentMode(lastMode)
            }
        }
        // Sync auto-off timer + intensity from DataStore on startup
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val option = AutoOffOption.entries.firstOrNull {
                    it.minutes == settings.autoOffMinutes
                } ?: AutoOffOption.NONE
                if (_autoOff.value != option) {
                    _autoOff.value = option
                }
                // Restore all persisted config values (don't trigger re-save)
                if (_torchIntensity.value != settings.torchIntensity)
                    _torchIntensity.value = settings.torchIntensity
                if (_morseText.value != settings.morseText)
                    _morseText.value = settings.morseText
                if (_morseSpeed.value != settings.morseSpeed)
                    _morseSpeed.value = settings.morseSpeed
                if (_sleepMinutes.value != settings.sleepMinutes)
                    _sleepMinutes.value = settings.sleepMinutes
                if (_micSensitivity.value != settings.micSensitivity)
                    _micSensitivity.value = settings.micSensitivity
                // Restore screen color by ID
                val restoredColor = ScreenColor.entries.firstOrNull {
                    it.name.lowercase() == settings.screenColorId
                } ?: ScreenColor.WHITE
                if (_currentScreenColor.value != restoredColor)
                    _currentScreenColor.value = restoredColor
                if (_screenText.value != settings.screenText)
                    _screenText.value = settings.screenText
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
        _showPaywallEvent.tryEmit(Unit)
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
        viewModelScope.launch { settingsRepository.setMorseText(text) }
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
        _currentScreenColor.value = color
        _screenEffect.value = null   // cancel any active effect when solid color chosen
        viewModelScope.launch { settingsRepository.setScreenColorId(color.name.lowercase()) }
    }

    fun setScreenEffect(effect: ScreenEffect?) {
        // Toggle: same effect → deactivate. Different effect → switch instantly.
        _screenEffect.value = if (_screenEffect.value == effect) null else effect
    }

    fun setScreenTab(tab: Int) { _screenTab.value = tab }

    fun setScreenHue(hue: Float) {
        _screenHue.value = hue.coerceIn(0f, 360f)
        _screenEffect.value = null
        // Convert HSV to Color and update screenColor for persistence
        val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        _currentScreenColor.value = ScreenColor.entries.firstOrNull { it.color.value == color.toLong() }
            ?: _currentScreenColor.value  // keep current if no exact match
    }

    fun setScreenTemp(temp: Float) {
        _screenTemp.value = temp.coerceIn(0f, 1f)
        _screenEffect.value = null
    }

    fun setAutoOffFromSettings(option: AutoOffOption) {
        setAutoOff(option)
    }

    fun releaseCamera() {
        flashRepository.release()
    }
}

package com.lumiai.flashlight.service

import com.lumiai.flashlight.core.torch.TorchController
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls flash pulses for notification alerts.
 * Routes the LED through [TorchController] so pulses are serialized with the rest of
 * the app and never race the camera (REL-T1/T3). Still works in the background — the
 * controller actuates via camera2 setTorchMode.
 */
@Singleton
class NotificationFlashController @Inject constructor(
    private val torch: TorchController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var flashJob: Job? = null

    var isEnabled: Boolean = false
    var enabledForCalls: Boolean    = true
    var enabledForMessages: Boolean = true
    var enabledForOther: Boolean    = false

    fun flash(pattern: NotificationPattern) {
        // Check per-type toggle
        val allowed = when (pattern) {
            NotificationPattern.CALL    -> enabledForCalls
            NotificationPattern.MESSAGE -> enabledForMessages
            NotificationPattern.OTHER   -> enabledForOther
        }
        if (!allowed) return

        // Pulses: CALL=3 fast, MESSAGE=2 medium, OTHER=1 short
        val pulses: List<Pair<Long, Long>> = when (pattern) {
            NotificationPattern.CALL    -> listOf(150L to 150L, 150L to 150L, 150L to 300L)
            NotificationPattern.MESSAGE -> listOf(200L to 200L, 200L to 500L)
            NotificationPattern.OTHER   -> listOf(150L to 50L)
        }

        flashJob?.cancel()
        flashJob = scope.launch {
            try {
                pulses.forEach { (onMs, offMs) ->
                    torch.setEnabled(true);  delay(onMs)
                    torch.setEnabled(false); if (offMs > 0) delay(offMs)
                }
            } catch (e: Exception) {
                runCatching { torch.setEnabled(false) }
            }
        }
    }
}

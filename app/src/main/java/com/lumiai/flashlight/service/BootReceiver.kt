package com.lumiai.flashlight.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lumiai.flashlight.core.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationFlashController: NotificationFlashController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Re-apply notification flash settings from DataStore after reboot
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val pendingResult = goAsync()

        scope.launch {
            try {
                val settings = settingsRepository.settings.first()
                notificationFlashController.isEnabled          = settings.notifFlashEnabled
                notificationFlashController.enabledForCalls    = settings.notifFlashCalls
                notificationFlashController.enabledForMessages = settings.notifFlashMessages
                notificationFlashController.enabledForOther    = settings.notifFlashOther
            } finally {
                pendingResult.finish()
            }
        }
    }
}

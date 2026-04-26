package com.lumiai.flashlight.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Listens for incoming notifications and pulses the flash.
 * User enables this in Settings → Notification access.
 *
 * Patterns:
 *  - Phone call:    3 fast pulses (urgent)
 *  - SMS/messaging: 2 medium pulses
 *  - Other apps:    1 short pulse
 */
@AndroidEntryPoint
class FlashNotificationService : NotificationListenerService() {

    @Inject lateinit var notificationFlashController: NotificationFlashController

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (!notificationFlashController.isEnabled) return

        // Don't flash for our own notifications
        if (sbn.packageName == packageName) return

        val category = sbn.notification?.category
        val pkg      = sbn.packageName ?: ""

        val pattern = when {
            // Incoming call
            category == Notification.CATEGORY_CALL ||
            pkg.contains("dialer") || pkg.contains("phone") ->
                NotificationPattern.CALL

            // Messaging apps
            category == Notification.CATEGORY_MESSAGE ||
            category == Notification.CATEGORY_EMAIL ||
            pkg.contains("whatsapp") || pkg.contains("telegram") ||
            pkg.contains("messenger") || pkg.contains("sms") ||
            pkg.contains("mms") ->
                NotificationPattern.MESSAGE

            // Everything else
            else -> NotificationPattern.OTHER
        }

        notificationFlashController.flash(pattern)
    }

    companion object {
        /** Check if user has granted notification access */
        fun isPermissionGranted(context: Context): Boolean {
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val component = ComponentName(context, FlashNotificationService::class.java)
            return flat.split(":").any { it == component.flattenToString() }
        }
    }
}

enum class NotificationPattern { CALL, MESSAGE, OTHER }

package com.lumiai.flashlight.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lumiai.flashlight.R

class FlashWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            val toggleIntent = Intent(context, FlashWidgetReceiver::class.java).apply {
                action = "com.lumiai.flashlight.TOGGLE_FLASH"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val views = RemoteViews(context.packageName, R.layout.widget_flash).apply {
                setOnClickPendingIntent(R.id.widget_btn, pendingIntent)
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.lumiai.flashlight.TOGGLE_FLASH") {
            toggleTorchCamera2(context)
        }
    }

    private fun toggleTorchCamera2(context: Context) {
        val cm = context.getSystemService(Context.CAMERA_SERVICE)
                as android.hardware.camera2.CameraManager
        try {
            val backId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.LENS_FACING) ==
                android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
            } ?: return

            // Register a one-shot callback to read the ACTUAL current state
            // before toggling — avoids desync if app or another widget changed torch
            val prefs = context.getSharedPreferences("widget_prefs", 0)
            val currentlyOn = prefs.getBoolean("torch_on", false)
            val newState = !currentlyOn

            cm.setTorchMode(backId, newState)
            prefs.edit().putBoolean("torch_on", newState).apply()

            // Update widget appearance immediately
            val manager = android.appwidget.AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, FlashWidgetReceiver::class.java)
            )
            ids.forEach { id ->
                val views = android.widget.RemoteViews(
                    context.packageName, R.layout.widget_flash
                )
                // Tint the button to reflect state (amber=on, default=off)
                views.setInt(R.id.widget_btn, "setBackgroundResource",
                    if (newState) android.R.color.holo_orange_light
                    else android.R.color.transparent)
                manager.updateAppWidget(id, views)
            }
        } catch (e: Exception) { /* camera busy or no flash */ }
    }

    /**
     * Sync widget state with the actual torch state reported by CameraManager.
     * Called from MainActivity.onResume to keep widget in sync.
     */
    companion object {
        fun syncState(context: Context, isOn: Boolean) {
            context.getSharedPreferences("widget_prefs", 0)
                .edit().putBoolean("torch_on", isOn).apply()
        }
    }
}

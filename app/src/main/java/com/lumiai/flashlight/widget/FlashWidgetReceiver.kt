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
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        try {
            val backId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.LENS_FACING) ==
                android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
            } ?: return
            // Read current torch state and toggle
            val torchOn = context.getSharedPreferences("widget_prefs", 0)
                .getBoolean("torch_on", false)
            cm.setTorchMode(backId, !torchOn)
            context.getSharedPreferences("widget_prefs", 0).edit()
                .putBoolean("torch_on", !torchOn).apply()
        } catch (e: Exception) { /* camera busy or no flash */ }
    }
}

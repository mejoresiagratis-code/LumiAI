package com.lumiai.flashlight.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lumiai.flashlight.R
import com.lumiai.flashlight.core.di.TorchControllerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        if (intent.action != "com.lumiai.flashlight.TOGGLE_FLASH") return

        // Toggle the real torch via the single source of truth. goAsync() keeps the
        // broadcast alive while the suspend call runs.
        val pending = goAsync()
        val controller = EntryPointAccessors.fromApplication(
            context.applicationContext,
            TorchControllerEntryPoint::class.java,
        ).torchController()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val newState = !controller.torchState.value // REAL state, not a cached prefs bool
                controller.setEnabled(newState)
                renderWidget(context, newState)
            } finally {
                pending.finish()
            }
        }
    }

    /** Update every widget instance's button tint to reflect the torch state. */
    private fun renderWidget(context: Context, on: Boolean) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, FlashWidgetReceiver::class.java)
        )
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_flash)
            views.setInt(
                R.id.widget_btn, "setBackgroundResource",
                if (on) android.R.color.holo_orange_light else android.R.color.transparent,
            )
            manager.updateAppWidget(id, views)
        }
    }

    companion object {
        /**
         * Refresh widget tint from the real torch state. Called from MainActivity.onResume
         * to keep the widget in sync with what the app shows.
         */
        fun syncState(context: Context, isOn: Boolean) {
            FlashWidgetReceiver().renderWidget(context, isOn)
        }
    }
}

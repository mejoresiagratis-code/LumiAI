package com.lumiai.flashlight.core.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

data class BatteryState(
    val level: Float,        // 0.0 – 1.0
    val isCharging: Boolean,
)

@Singleton
class BatteryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Battery state as a Flow. Uses sticky ACTION_BATTERY_CHANGED broadcast —
     * no permission required. Emits immediately with the current state, then
     * on every change while subscribed.
     */
    val batteryState: Flow<BatteryState> = callbackFlow {
        fun readState(): BatteryState {
            val intent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = intent?.let {
                val cur = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val max = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (cur >= 0 && max > 0) cur.toFloat() / max.toFloat() else 1f
            } ?: 1f

            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL

            return BatteryState(level = level, isCharging = charging)
        }

        // Emit current state immediately
        trySend(readState())

        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: Intent) {
                trySend(readState())
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}

package com.lumiai.flashlight.service

import android.service.quicksettings.Tile
import android.annotation.SuppressLint
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.lumiai.flashlight.core.data.repository.FlashRepositoryImpl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Quick Settings tile — toggles the flashlight from the notification shade.
 * Requires android.permission.BIND_QUICK_SETTINGS_TILE.
 */
@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class FlashTileService : TileService() {

    @Inject lateinit var flashRepository: FlashRepositoryImpl

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onTileAdded() { updateTile() }
    override fun onStartListening() { updateTile() }

    override fun onClick() {
        scope.launch {
            if (flashRepository.isFlashOn.value) {
                flashRepository.turnOff()
            } else {
                flashRepository.activateMode(
                    com.lumiai.flashlight.core.domain.model.FlashMode.Steady
                )
            }
            updateTile()
        }
    }

    private fun updateTile() {
        qsTile?.apply {
            state = if (flashRepository.isFlashOn.value) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

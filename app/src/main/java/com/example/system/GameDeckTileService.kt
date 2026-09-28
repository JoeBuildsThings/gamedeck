package com.example.system

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.GameDeckApp
import com.example.R
import com.example.model.PerformanceMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameDeckTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val currentMode = GameDeckApp.instance.repository.currentModeFlow.first()
            val nextMode = currentMode.next()

            GameDeckApp.instance.repository.setMode(nextMode)

            val (dnd, rr, _) = GameDeckApp.instance.repository.getBaselineSnapshot()
            val whitelist = GameDeckApp.instance.repository.killWhitelistFlow.first()

            val plan = GameDeckApp.instance.modeEngine.buildActionPlan(
                targetMode = nextMode,
                allowedThirdPartyPackages = whitelist,
                baselineDnd = dnd,
                baselineRefreshRate = rr
            )

            for (action in plan) {
                val res = action.apply(this@GameDeckTileService, GameDeckApp.instance.shizukuBridge)
                GameDeckApp.instance.repository.logAction("Tile Action", "${action.description}: ${res.summary}")
            }

            updateTileState(nextMode)
        }
    }

    private fun updateTileState(forcedMode: PerformanceMode? = null) {
        val tile = qsTile ?: return
        serviceScope.launch {
            val mode = forcedMode ?: GameDeckApp.instance.repository.currentModeFlow.first()
            tile.label = "GameDeck: ${mode.shortLabel}"
            tile.contentDescription = "Current Mode: ${mode.title}. Tap to cycle."
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = mode.title
            }
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

package app.tote.feature.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.tote.core.data.platform.PickerIntents
import app.tote.core.data.repo.VaultSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Quick Settings tile (Plan §9.5): tap → picker sheet → pick → copied.
 *
 * The tile is a launcher, not a toggle, so it stays ACTIVE (lit) whenever it's visible.
 */
@AndroidEntryPoint
class ToteTileService : TileService() {

    @Inject lateinit var pickerIntents: PickerIntents
    @Inject lateinit var snapshot: VaultSnapshot

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listening: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        render(snapshot.state.value)
        // Follow the active profile while the shade is open, so switching it in the picker shows here.
        listening?.cancel()
        listening = scope.launch { snapshot.state.collect { render(it) } }
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
        super.onStopListening()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun render(vault: VaultSnapshot.Snapshot?) {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // The subtitle names the active profile (Plan §9.5) once profiles exist.
            tile.subtitle = when {
                vault == null || vault.profiles.isEmpty() -> getString(R.string.tile_subtitle)
                else -> vault.activeProfile?.name ?: getString(R.string.tile_subtitle_all)
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        // On the lock screen, ask for unlock first: the vault isn't for whoever holds the phone.
        if (isLocked) unlockAndRun { openPicker() } else openPicker()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openPicker() {
        val intent = pickerIntents.copyIntent(this).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                REQUEST_PICK,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private companion object {
        const val REQUEST_PICK = 1
    }
}

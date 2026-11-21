package app.tote.feature.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.tote.core.data.platform.PickerIntents
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

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Phase 5 replaces this with the active profile's name.
            tile.subtitle = getString(R.string.tile_subtitle)
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

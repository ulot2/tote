package app.tote.util

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import app.tote.feature.tile.ToteTileService

/** Outcome of asking the system to add Tuck's tile. */
enum class TileAddResult { ADDED, ALREADY_ADDED, NOT_ADDED, UNAVAILABLE }

/** Wraps the Android 13+ "add tile" prompt (Plan §8.4 onboarding step, also in Settings). */
object TilePrompt {

    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @SuppressLint("NewApi")
    fun request(context: Context, onResult: (TileAddResult) -> Unit) {
        if (!isSupported) return onResult(TileAddResult.UNAVAILABLE)
        val manager = context.getSystemService(StatusBarManager::class.java) ?: return onResult(TileAddResult.UNAVAILABLE)
        manager.requestAddTileService(
            ComponentName(context, ToteTileService::class.java),
            context.getString(app.tote.feature.tile.R.string.tile_label),
            Icon.createWithResource(context, app.tote.feature.tile.R.drawable.ic_tile),
            context.mainExecutor,
        ) { code ->
            onResult(
                when (code) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> TileAddResult.ADDED
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> TileAddResult.ALREADY_ADDED
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> TileAddResult.NOT_ADDED
                    else -> TileAddResult.UNAVAILABLE
                },
            )
        }
    }
}

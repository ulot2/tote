package app.tote.feature.tile

import android.content.Context
import android.content.Intent

/**
 * Builds the intent that opens the picker sheet. The picker lives in `:app` (it needs the
 * design system), so `:app` provides this through Hilt and the tile stays decoupled from it.
 */
fun interface PickerLauncher {
    fun copyIntent(context: Context): Intent
}

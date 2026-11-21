package app.tote.core.data.platform

import android.content.Context
import android.content.Intent
import android.os.Parcelable

/**
 * Builds the intents that open the picker sheet. The picker lives in `:app` (it needs the design
 * system); the tile and the autofill service only know this interface, which `:app` binds.
 */
interface PickerIntents {
    /** Picker that copies the chosen entry (Quick Settings tile). */
    fun copyIntent(context: Context): Intent

    /**
     * Picker that fills the chosen entry into [autofillId] ("All entries…" in autofill).
     * [urlField] makes links get a scheme when filled (Plan §6.5).
     */
    fun fillIntent(context: Context, autofillId: Parcelable, urlField: Boolean): Intent
}

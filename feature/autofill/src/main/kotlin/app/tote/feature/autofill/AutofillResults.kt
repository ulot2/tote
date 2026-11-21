package app.tote.feature.autofill

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import android.service.autofill.Dataset
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import app.tote.core.model.Entry
import app.tote.core.model.ValueFormatter

/** Helpers for the picker when it was opened by "All entries…" (dataset authentication). */
object AutofillResults {

    const val EXTRA_AUTOFILL_ID = "app.tote.extra.AUTOFILL_ID"
    const val EXTRA_URL_FIELD = "app.tote.extra.URL_FIELD"

    fun autofillId(intent: Intent): AutofillId? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_AUTOFILL_ID, AutofillId::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Parcelable>(EXTRA_AUTOFILL_ID) as? AutofillId
        }

    /** The result intent that fills [entry] into the field the picker was opened for. */
    @Suppress("DEPRECATION")
    fun resultFor(context: Context, autofillId: AutofillId, entry: Entry, urlField: Boolean): Intent {
        val value = AutofillValue.forText(ValueFormatter.forFill(entry.type, entry.value, urlField))
        // The presentation is required by the API but never shown: the value fills straight away.
        val presentation = RemoteViews(context.packageName, R.layout.tote_autofill_item).apply {
            setTextViewText(R.id.tote_af_label, entry.label)
        }
        val dataset = Dataset.Builder(presentation).setValue(autofillId, value).build()
        return Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset)
    }
}

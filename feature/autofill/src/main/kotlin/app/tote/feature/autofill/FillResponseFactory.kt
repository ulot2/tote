package app.tote.feature.autofill

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.view.inputmethod.InlineSuggestionsRequest
import app.tote.core.data.platform.PickerIntents
import app.tote.core.matching.FieldMatcher
import app.tote.core.matching.InputKind
import app.tote.core.matching.MatchContext
import app.tote.core.model.Entry
import app.tote.core.model.ValueFormatter

/**
 * Turns parsed fields + the vault into a [FillResponse] (Plan §9.4 steps 4–6).
 *
 * One dataset per suggestion per field, each filling just that field, so the system shows the
 * right rows whichever field is focused. Every field that gets suggestions also gets a final
 * "All entries…" dataset that opens the picker through dataset authentication.
 */
class FillResponseFactory(
    private val context: Context,
    private val matcher: FieldMatcher,
    private val presentations: Presentations,
    private val pickerIntents: PickerIntents,
) {

    data class Plan<ID>(val field: ParsedField<ID>, val entries: List<Entry>, val allEntries: Boolean, val urlField: Boolean)

    /**
     * Decides what to offer for each field, without touching Android's autofill classes, so the
     * decision logic is testable on its own.
     */
    fun <ID> plan(fields: List<ParsedField<ID>>, entries: List<Entry>, context: MatchContext): List<Plan<ID>> {
        // A lone text box (a chat or search bar) only gets suggestions for real matches; the
        // most-used fallback is for screens that look like forms (Plan §7.4, DECISIONS.md).
        val looksLikeForm = fields.size >= 2 || fields.any { it.signals.webDomain != null }
        return fields
            .sortedByDescending { it.focused }
            .take(MAX_FIELDS)
            .mapNotNull { field ->
                val result = matcher.match(field.signals, entries, context)
                when {
                    result.ignored != null -> null
                    result.isFallback && !looksLikeForm -> null
                    result.suggestions.isEmpty() && !result.showAllEntries -> null
                    else -> Plan(
                        field = field,
                        entries = result.suggestions.map { it.entry },
                        allEntries = result.showAllEntries,
                        urlField = field.signals.effectiveKind == InputKind.URI,
                    )
                }
            }
    }

    @Suppress("DEPRECATION") // The pre-API-33 Dataset builders still work everywhere and cover API 26+.
    fun build(plans: List<Plan<AutofillId>>, inlineRequest: InlineSuggestionsRequest?, responseId: String): FillResponse? {
        if (plans.isEmpty()) return null
        val response = FillResponse.Builder()
        var datasets = 0

        val inlineSpecs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) inlineRequest?.inlinePresentationSpecs.orEmpty() else emptyList()
        val maxInline = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) inlineRequest?.maxSuggestionCount ?: 0 else 0
        val attribution by lazy { attributionIntent() }

        plans.forEachIndexed { index, plan ->
            // Inline chips go on every field, not just the focused one. When the user moves to another
            // field on the same screen, Android reuses this response instead of asking again, and while
            // the keyboard shows chips it hides the dropdown: a field without chips shows nothing.
            var inlineUsed = 0
            val inlineAllowed = inlineSpecs.isNotEmpty()
            fun nextSpec() = if (inlineAllowed && inlineUsed < maxInline) inlineSpecs[minOf(inlineUsed, inlineSpecs.lastIndex)] else null

            // Keep a slot for "All entries…" among the inline chips.
            val entryInlineBudget = if (plan.allEntries) maxInline - 1 else maxInline
            for (entry in plan.entries) {
                val value = AutofillValue.forText(ValueFormatter.forFill(entry.type, entry.value, plan.urlField))
                val menu = presentations.dropdown(entry)
                val spec = if (inlineUsed < entryInlineBudget) nextSpec() else null
                val inline = if (spec != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) presentations.inline(entry, spec, attribution) else null
                val builder = Dataset.Builder(menu)
                if (inline != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    inlineUsed++
                    builder.setInlinePresentation(inline)
                }
                response.addDataset(
                    builder.setId(datasetId(entry)).setValue(plan.field.id, value).build(),
                )
                datasets++
            }

            if (plan.allEntries) {
                val menu = presentations.allEntries()
                val spec = nextSpec()
                val inline = if (spec != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) presentations.inlineAllEntries(spec, attribution) else null
                val builder = Dataset.Builder(menu)
                if (inline != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) builder.setInlinePresentation(inline)
                response.addDataset(
                    builder
                        .setId(ALL_ENTRIES_ID)
                        .setValue(plan.field.id, null)
                        .setAuthentication(pickerSender(plan, index))
                        .build(),
                )
                datasets++
            }
        }
        if (datasets == 0) return null
        response.setClientState(Bundle().apply { putString(KEY_RESPONSE_ID, responseId) })
        return response.build()
    }

    private fun pickerSender(plan: Plan<AutofillId>, index: Int) = PendingIntent.getActivity(
        context,
        REQUEST_PICKER_BASE + index, // distinct per field: extras don't make PendingIntents distinct
        pickerIntents.fillIntent(context, plan.field.id, plan.urlField),
        // Mutable: the system adds the screen's structure to the intent when it launches the picker.
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0),
    ).intentSender

    /** Long-pressing an inline chip opens Tote. */
    private fun attributionIntent(): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        return PendingIntent.getActivity(context, REQUEST_ATTRIBUTION, launch, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val MAX_FIELDS = 15
        const val ALL_ENTRIES_ID = "all"
        const val KEY_RESPONSE_ID = "tote.responseId"
        private const val REQUEST_ATTRIBUTION = 900
        private const val REQUEST_PICKER_BASE = 1000

        /** "e:<entryId>": read back from the fill event history to count uses. No values, no field text. */
        fun datasetId(entry: Entry) = "e:${entry.id}"

        fun entryIdOf(datasetId: String?): Long? = datasetId?.takeIf { it.startsWith("e:") }?.removePrefix("e:")?.toLongOrNull()
    }
}

package app.tote.feature.autofill

import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillEventHistory
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.util.Log
import app.tote.core.data.di.ApplicationScope
import app.tote.core.data.platform.PickerIntents
import app.tote.core.data.repo.EntryRepository
import app.tote.core.data.repo.LearningRepository
import app.tote.core.data.repo.VaultSnapshot
import app.tote.core.matching.FieldMatcher
import app.tote.core.matching.MatchContext
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject

/**
 * Tote's autofill service (Plan §9.4).
 *
 * Fast path: the vault comes from [VaultSnapshot] (memory, kept warm from Room); parsing and
 * matching run off the main thread; nothing from the screen is kept after the request.
 * Save requests are out of scope for v1.
 */
@AndroidEntryPoint
class ToteAutofillService : AutofillService() {

    @Inject lateinit var snapshot: VaultSnapshot
    @Inject lateinit var repository: EntryRepository
    @Inject lateinit var learning: LearningRepository
    @Inject lateinit var pickerIntents: PickerIntents
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val matcher by lazy { FieldMatcher() }
    private val factory by lazy { FillResponseFactory(this, matcher, Presentations(this, IconRenderer(this)), pickerIntents) }

    /** Responses whose selections we've already counted (history can be read more than once). */
    private val countedResponses = ArrayDeque<String>()

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        countSelections(fillEventHistory)

        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null || isToteItself(structure.activityComponent?.packageName, structure.activityComponent?.className)) {
            callback.onSuccess(null)
            return
        }
        val inlineRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) request.inlineSuggestionsRequest else null

        val job = scope.launch {
            val started = System.nanoTime()
            fun elapsedMs() = (System.nanoTime() - started) / 1_000_000
            val response = try {
                val vault = withTimeout(VAULT_TIMEOUT_MS) { snapshot.current() }
                val (built, fieldCount, suggestionCount) = withContext(Dispatchers.Default) {
                    val fields = StructureParser.parse(AssistNodeView.windows(structure), structure.activityComponent?.packageName)
                    val plans = factory.plan(fields, vault.entries, MatchContext(activeProfileId = vault.activeProfileId)) { key ->
                        vault.learnedFor(key.source, key.signature)
                    }
                    Triple(factory.build(plans, inlineRequest, UUID.randomUUID().toString()), fields.size, plans.sumOf { it.entries.size })
                }
                AutofillDiagnostics.record(AutofillDiagnostics.LastRequest(System.currentTimeMillis(), fieldCount, suggestionCount, elapsedMs()))
                if (BuildConfigCompat.isDebuggable(this@ToteAutofillService)) {
                    // Counts and timing only. Never entry values or field text.
                    Log.d(TAG, "fill request: $fieldCount fields, $suggestionCount suggestions, ${elapsedMs()} ms")
                }
                built
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AutofillDiagnostics.record(AutofillDiagnostics.LastRequest(System.currentTimeMillis(), 0, 0, elapsedMs(), e.javaClass.simpleName))
                Log.w(TAG, "fill request failed: ${e.javaClass.simpleName}")
                null
            }
            callback.onSuccess(response)
        }
        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess() // Saving typed values is out of scope for v1 (Plan §9.4).
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Counts suggestions the user picked last time and remembers them for that field (Plan §9.4
     * step 7, §6.3). "All entries…" picks are handled by the picker.
     */
    private fun countSelections(history: FillEventHistory?) {
        history ?: return
        val state = history.clientState
        val responseId = state?.getString(FillResponseFactory.KEY_RESPONSE_ID) ?: return
        if (responseId in countedResponses) return
        countedResponses.addLast(responseId)
        if (countedResponses.size > 20) countedResponses.removeFirst()
        val picked = history.events.orEmpty()
            .filter { it.type == FillEventHistory.Event.TYPE_DATASET_SELECTED }
            .mapNotNull { event ->
                FillResponseFactory.entryIdOf(event.datasetId)?.let { it to FillResponseFactory.learningKeyOf(state, event.datasetId) }
            }
        if (picked.isEmpty()) return
        appScope.launch {
            picked.forEach { (entryId, key) ->
                repository.recordUse(entryId)
                key?.let { learning.record(it.source, it.signature, entryId) }
            }
        }
    }

    /** Don't offer Tote's own entries inside Tote, except on its autofill test screen. */
    private fun isToteItself(pkg: String?, className: String?): Boolean =
        pkg == packageName && className?.endsWith(PLAYGROUND_CLASS_SUFFIX) != true

    companion object {
        private const val TAG = "ToteAutofill"
        private const val VAULT_TIMEOUT_MS = 2_000L
        const val PLAYGROUND_CLASS_SUFFIX = ".AutofillPlaygroundActivity"
    }
}

internal object BuildConfigCompat {
    fun isDebuggable(context: android.content.Context): Boolean =
        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

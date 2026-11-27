package app.tote.feature.onboarding

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tote.core.data.backup.BackupRepository
import app.tote.core.data.backup.ReadResult
import app.tote.core.data.repo.EntryRepository
import app.tote.core.data.settings.SettingsRepository
import app.tote.core.model.Entry
import app.tote.core.model.SeedTemplate
import app.tote.core.model.SeedTemplates
import app.tote.util.BackupFiles
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep { WELCOME, PICK, FILL, AUTOFILL }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    /** Template keys, in [SeedTemplates.all] order when shown. */
    val selected: Set<String> = DEFAULT_PICKS,
    val values: Map<String, String> = emptyMap(),
    val busy: Boolean = false,
    /** Shown under the welcome buttons when a restore didn't work. */
    val restoreError: String? = null,
    val finished: Boolean = false,
) {
    val templates: List<SeedTemplate> get() = SeedTemplates.all.filter { it.key in ONBOARDING_KEYS }
    val selectedTemplates: List<SeedTemplate> get() = templates.filter { it.key in selected }
    val filledCount: Int get() = selectedTemplates.count { !values[it.key].isNullOrBlank() }

    companion object {
        /** The design's ten tiles; "Years of experience" stays in the empty vault and the editor. */
        val ONBOARDING_KEYS = listOf("linkedin", "portfolio", "github", "email", "phone", "cv", "name", "location", "bio", "why")
        val DEFAULT_PICKS = setOf("linkedin", "portfolio", "github", "email", "cv")
    }
}

/**
 * First run (Plan §8.4): welcome, pick the fields you type most, paste in their values, then
 * turn on autofill and the tile. Every step can be skipped; nothing is saved until "fill".
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val entries: EntryRepository,
    private val settings: SettingsRepository,
    private val backups: BackupRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state

    /** Template key → id it was saved as, so going back and forth edits instead of duplicating. */
    private val saved = HashMap<String, Long>()

    fun go(step: OnboardingStep) = _state.update { it.copy(step = step, restoreError = null) }

    /** Back within the flow; false when already on the first step. */
    fun back(): Boolean {
        val prev = OnboardingStep.entries.getOrNull(_state.value.step.ordinal - 1) ?: return false
        go(prev)
        return true
    }

    fun toggle(key: String) = _state.update {
        it.copy(selected = if (key in it.selected) it.selected - key else it.selected + key)
    }

    fun setValue(key: String, value: String) = _state.update { it.copy(values = it.values + (key to value)) }

    /** Saves the filled-in values as entries, then moves on to autofill. Blank ones are skipped. */
    fun saveValues() {
        val s = _state.value
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            for (t in s.templates) {
                val value = s.values[t.key]?.trim().orEmpty()
                val existing = saved[t.key]
                if (t.key !in s.selected || value.isEmpty()) {
                    // Deselected or cleared after an earlier save: take it back out.
                    if (existing != null) { entries.delete(existing); saved.remove(t.key) }
                    continue
                }
                val entry = Entry(id = existing ?: 0, label = t.label, value = value, type = t.type, category = t.category, icon = t.icon)
                saved[t.key] = entries.save(entry)
            }
            _state.update { it.copy(busy = false, step = OnboardingStep.AUTOFILL) }
        }
    }

    fun restore(uri: Uri) {
        _state.update { it.copy(busy = true, restoreError = null) }
        viewModelScope.launch {
            val text = BackupFiles.read(context, uri)
            val error = when (val read = text?.let { backups.read(it) }) {
                is ReadResult.Ok -> {
                    backups.restore(read.backup)
                    null
                }
                is ReadResult.TooNew -> "That backup is from a newer Tuck. Update Tuck, then try again."
                else -> "That isn't a Tuck backup. Pick the .json file Tuck saved."
            }
            if (error == null) finish() else _state.update { it.copy(busy = false, restoreError = error) }
        }
    }

    fun finish() {
        viewModelScope.launch {
            settings.setOnboardingDone(true)
            _state.update { it.copy(busy = false, finished = true) }
        }
    }
}

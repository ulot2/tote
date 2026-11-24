package app.tote.feature.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tote.core.data.di.ApplicationScope
import app.tote.core.data.platform.ClipboardWriter
import kotlinx.coroutines.CoroutineScope
import app.tote.core.data.repo.EntryRepository
import app.tote.core.data.repo.LearningRepository
import app.tote.core.data.repo.ProfileRepository
import app.tote.core.data.settings.SettingsRepository
import app.tote.core.model.Profile
import app.tote.core.model.Entry
import app.tote.core.model.EntryRanking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PickerUiState(
    val loading: Boolean = true,
    val query: String = "",
    val entries: List<Entry> = emptyList(),
    val totalCount: Int = 0,
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: Long? = null,
)

@HiltViewModel
class PickerViewModel @Inject constructor(
    private val repository: EntryRepository,
    private val clipboard: ClipboardWriter,
    @ApplicationScope private val appScope: CoroutineScope,
    private val learning: LearningRepository,
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<PickerUiState> = combine(repository.entries, query, profiles.profiles, settings.settings) { entries, q, profileList, s ->
        val active = s.activeProfileId?.takeIf { id -> profileList.any { it.id == id } }
        val visible = if (active == null) entries else entries.filter { it.isInAllProfiles || active in it.profileIds }
        PickerUiState(
            loading = false,
            query = q,
            entries = EntryRanking.search(visible, q),
            totalCount = entries.size,
            profiles = profileList,
            activeProfileId = active,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PickerUiState())

    /** Switching here switches everywhere: vault, autofill and the tile follow the same setting. */
    fun selectProfile(id: Long?) {
        viewModelScope.launch { settings.setActiveProfile(id) }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Copies the entry and counts the use. Runs before the sheet animates away. */
    fun copy(entry: Entry) {
        clipboard.copy(entry.label, entry.value)
        recordUse(entry)
    }

    /** Remembers that [entry] was chosen for this field, so it's suggested first next time. */
    fun learn(source: String, signature: String, entry: Entry) {
        appScope.launch { learning.record(source, signature, entry.id) }
    }

    /** Counts a use without copying (autofill fills the field directly). */
    fun recordUse(entry: Entry) {
        // Outlives the ViewModel: the activity finishes right after a fill.
        appScope.launch { repository.recordUse(entry.id) }
    }
}

package app.tote.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tote.core.data.platform.ClipboardWriter
import app.tote.core.data.repo.EntryRepository
import app.tote.core.data.repo.ProfileRepository
import app.tote.core.model.EntryRanking
import app.tote.core.model.Profile
import app.tote.core.data.settings.SettingsRepository
import kotlinx.coroutines.flow.map
import app.tote.core.model.Category
import app.tote.core.model.Entry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultSection(val category: Category, val entries: List<Entry>)

data class VaultUiState(
    val loading: Boolean = true,
    val totalCount: Int = 0,
    val query: String = "",
    val pinned: List<Entry> = emptyList(),
    val sections: List<VaultSection> = emptyList(),
    val reordering: Boolean = false,
    val profiles: List<Profile> = emptyList(),
    /** null = "All": every entry shows. */
    val activeProfileId: Long? = null,
    /** Entries that belong only to other profiles, hidden by the active one. */
    val hiddenByProfile: Int = 0,
) {
    val activeProfile: Profile? get() = profiles.firstOrNull { it.id == activeProfileId }

    val isVaultEmpty: Boolean get() = !loading && totalCount == 0
    val noResults: Boolean get() = !loading && totalCount > 0 && sections.isEmpty()
}

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: EntryRepository,
    private val clipboard: ClipboardWriter,
    private val settings: SettingsRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {

    val autofillBannerDismissed: StateFlow<Boolean> = settings.settings
        .map { it.autofillBannerDismissed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun dismissAutofillBanner() {
        viewModelScope.launch { settings.setAutofillBannerDismissed(true) }
    }


    private val query = MutableStateFlow("")
    private val reordering = MutableStateFlow(false)

    val state: StateFlow<VaultUiState> = combine(
        repository.entries,
        query,
        reordering,
        profiles.profiles,
        settings.settings,
    ) { entries, q, reorder, profileList, s ->
        // A deleted profile id means "All". Reorder mode always shows everything, so a manual order
        // is never saved for a partial list.
        val active = s.activeProfileId?.takeIf { id -> profileList.any { it.id == id } }
        val inProfile = if (active == null || reorder) entries else entries.filter { it.isInAllProfiles || active in it.profileIds }
        val filtered = if (q.isBlank()) inProfile else inProfile.filter { EntryRanking.matches(it, q) }
        VaultUiState(
            loading = false,
            totalCount = entries.size,
            query = q,
            pinned = if (q.isBlank() && !reorder) inProfile.filter { it.pinned } else emptyList(),
            sections = Category.entries.mapNotNull { cat ->
                filtered.filter { it.category == cat }.takeIf { it.isNotEmpty() }?.let { VaultSection(cat, it) }
            },
            reordering = reorder,
            profiles = profileList,
            activeProfileId = active,
            hiddenByProfile = entries.size - inProfile.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultUiState())

    fun selectProfile(id: Long?) {
        viewModelScope.launch { settings.setActiveProfile(id) }
    }

    fun onQueryChange(value: String) {
        query.value = value
        if (value.isNotBlank()) reordering.value = false
    }

    fun setReordering(enabled: Boolean) {
        reordering.value = enabled
        if (enabled) query.value = ""
    }

    fun copy(entry: Entry) {
        clipboard.copy(entry.label, entry.value)
        viewModelScope.launch { repository.recordUse(entry.id) }
    }

    fun togglePin(entry: Entry) {
        viewModelScope.launch { repository.setPinned(entry.id, !entry.pinned) }
    }

    fun delete(entry: Entry) {
        viewModelScope.launch { repository.delete(entry.id) }
    }

    fun restore(entry: Entry) {
        viewModelScope.launch { repository.restore(entry) }
    }

    /** Persists the order of one category after a drag finishes. */
    fun commitOrder(orderedIds: List<Long>) {
        viewModelScope.launch { repository.reorder(orderedIds) }
    }

}

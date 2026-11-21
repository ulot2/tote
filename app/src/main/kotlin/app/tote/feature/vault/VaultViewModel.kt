package app.tote.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tote.core.data.platform.ClipboardWriter
import app.tote.core.data.repo.EntryRepository
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
) {
    val isVaultEmpty: Boolean get() = !loading && totalCount == 0
    val noResults: Boolean get() = !loading && totalCount > 0 && sections.isEmpty()
}

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: EntryRepository,
    private val clipboard: ClipboardWriter,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val reordering = MutableStateFlow(false)

    val state: StateFlow<VaultUiState> = combine(repository.entries, query, reordering) { entries, q, reorder ->
        val filtered = if (q.isBlank()) entries else entries.filter { it.matches(q) }
        VaultUiState(
            loading = false,
            totalCount = entries.size,
            query = q,
            pinned = if (q.isBlank() && !reorder) entries.filter { it.pinned } else emptyList(),
            sections = Category.entries.mapNotNull { cat ->
                filtered.filter { it.category == cat }.takeIf { it.isNotEmpty() }?.let { VaultSection(cat, it) }
            },
            reordering = reorder,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultUiState())

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

    private fun Entry.matches(q: String): Boolean {
        val needle = q.trim()
        return label.contains(needle, ignoreCase = true) ||
            value.contains(needle, ignoreCase = true) ||
            keywords.any { it.contains(needle, ignoreCase = true) }
    }
}

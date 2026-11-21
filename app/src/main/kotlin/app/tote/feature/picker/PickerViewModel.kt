package app.tote.feature.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tote.core.data.di.ApplicationScope
import app.tote.core.data.platform.ClipboardWriter
import kotlinx.coroutines.CoroutineScope
import app.tote.core.data.repo.EntryRepository
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
)

@HiltViewModel
class PickerViewModel @Inject constructor(
    private val repository: EntryRepository,
    private val clipboard: ClipboardWriter,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<PickerUiState> = combine(repository.entries, query) { entries, q ->
        PickerUiState(loading = false, query = q, entries = EntryRanking.search(entries, q), totalCount = entries.size)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PickerUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Copies the entry and counts the use. Runs before the sheet animates away. */
    fun copy(entry: Entry) {
        clipboard.copy(entry.label, entry.value)
        recordUse(entry)
    }

    /** Counts a use without copying (autofill fills the field directly). */
    fun recordUse(entry: Entry) {
        // Outlives the ViewModel: the activity finishes right after a fill.
        appScope.launch { repository.recordUse(entry.id) }
    }
}

package app.tote.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.tote.core.data.platform.ClipboardWriter
import app.tote.core.data.repo.EntryRepository
import app.tote.core.model.Category
import app.tote.core.model.Entry
import app.tote.core.model.EntryClassifier
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType
import app.tote.core.model.EntryValidator
import app.tote.core.model.SeedTemplates
import app.tote.ui.nav.EditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val label: String = "",
    val value: String = "",
    val placeholder: String = "",
    val type: EntryType = EntryType.SHORT_TEXT,
    val category: Category = Category.ANSWERS,
    val icon: EntryIcon = EntryIcon.TEXT,
    /** While true, type/category/icon follow what the value looks like. Cleared by a manual pick. */
    val typeAuto: Boolean = true,
    val categoryAuto: Boolean = true,
    val iconAuto: Boolean = true,
    val keywords: List<String> = emptyList(),
    val keywordDraft: String = "",
    val pinned: Boolean = false,
    /** Errors are only shown after a save attempt, so a fresh form isn't red. */
    val showErrors: Boolean = false,
    val validation: EntryValidator.Result = EntryValidator.Result(emptyList(), emptyList()),
    val dirty: Boolean = false,
    val finished: Boolean = false,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EntryRepository,
    private val clipboard: ClipboardWriter,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<EditorRoute>()
    private var original: Entry? = null

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = if (route.id != 0L) repository.get(route.id) else null
            original = existing
            _state.value = if (existing != null) {
                EditorUiState(
                    loading = false,
                    isNew = false,
                    label = existing.label,
                    value = existing.value,
                    type = existing.type,
                    category = existing.category,
                    icon = existing.icon,
                    typeAuto = false,
                    categoryAuto = false,
                    iconAuto = !existing.iconOverridden,
                    keywords = existing.keywords,
                    pinned = existing.pinned,
                ).validated()
            } else {
                val template = SeedTemplates.byKey(route.template)
                if (template != null) {
                    EditorUiState(
                        loading = false,
                        label = template.label,
                        placeholder = template.placeholder,
                        type = template.type,
                        category = template.category,
                        icon = template.icon,
                        // The template knows what it is; only the icon keeps auto-detecting.
                        typeAuto = false,
                        categoryAuto = false,
                    ).validated()
                } else {
                    EditorUiState(loading = false).validated()
                }
            }
        }
    }

    fun onLabelChange(v: String) = edit { copy(label = v.take(EntryValidator.MAX_LABEL_LENGTH + 10)).reclassify() }
    fun onValueChange(v: String) = edit { copy(value = v).reclassify() }

    fun onTypeSelected(t: EntryType) = edit {
        // Keep icon/category in step with the chosen type unless the user picked those by hand.
        val suggested = EntryClassifier.suggest(value, label)
        copy(
            type = t,
            typeAuto = false,
            icon = if (iconAuto) EntryClassifier.inferIcon(value, label, t) else icon,
            category = when {
                !categoryAuto -> category
                suggested.type == t -> suggested.category
                else -> defaultCategory(t)
            },
        )
    }

    fun onCategorySelected(c: Category) = edit { copy(category = c, categoryAuto = false) }

    /** null = back to automatic detection. */
    fun onIconSelected(i: EntryIcon?) = edit {
        if (i == null) copy(iconAuto = true).reclassify() else copy(icon = i, iconAuto = false)
    }

    fun onKeywordDraftChange(v: String) {
        // A comma or newline commits the keyword, like a tag input.
        if (v.endsWith(",") || v.endsWith("\n")) {
            _state.update { it.copy(keywordDraft = v.dropLast(1)) }
            addKeyword()
        } else {
            _state.update { it.copy(keywordDraft = v) }
        }
    }

    fun addKeyword() = edit {
        val k = keywordDraft.trim().lowercase()
        if (k.isEmpty() || k in keywords) copy(keywordDraft = "") else copy(keywords = keywords + k, keywordDraft = "")
    }

    fun removeKeyword(k: String) = edit { copy(keywords = keywords - k) }

    fun pasteFromClipboard() {
        clipboard.readText()?.let { text -> onValueChange(text.trim()) }
    }

    fun save() {
        val s = _state.value.let { if (it.keywordDraft.isNotBlank()) it.copy(keywords = (it.keywords + it.keywordDraft.trim().lowercase()).distinct(), keywordDraft = "") else it }
        if (!s.validation.canSave) {
            _state.value = s.copy(showErrors = true)
            return
        }
        viewModelScope.launch {
            val base = original
            val entry = (base ?: Entry(label = "", value = "", type = s.type, category = s.category, icon = s.icon)).copy(
                label = s.label.trim(),
                value = s.value.trim(),
                type = s.type,
                category = s.category,
                icon = s.icon,
                iconOverridden = !s.iconAuto,
                keywords = s.keywords,
                pinned = s.pinned,
            )
            repository.save(entry)
            _state.update { it.copy(finished = true) }
        }
    }

    fun delete() {
        val id = original?.id ?: return
        viewModelScope.launch {
            repository.delete(id)
            _state.update { it.copy(finished = true) }
        }
    }

    private inline fun edit(crossinline f: EditorUiState.() -> EditorUiState) {
        _state.update { it.f().copy(dirty = true).validated() }
    }

    private fun EditorUiState.reclassify(): EditorUiState {
        if (value.isBlank() && !isNew) return this
        val s = EntryClassifier.suggest(value, label)
        val newType = if (typeAuto && value.isNotBlank()) s.type else type
        return copy(
            type = newType,
            category = if (categoryAuto && value.isNotBlank()) s.category else category,
            icon = if (iconAuto) EntryClassifier.inferIcon(value, label, newType) else icon,
        )
    }

    private fun EditorUiState.validated() = copy(validation = EntryValidator.validate(label, value, type))

    private fun defaultCategory(t: EntryType) = when (t) {
        EntryType.LINK -> Category.LINKS
        EntryType.EMAIL, EntryType.PHONE -> Category.CONTACT
        EntryType.SHORT_TEXT, EntryType.LONG_TEXT -> Category.ANSWERS
    }
}

package app.tote.feature.learned

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.tote.core.data.repo.EntryRepository
import app.tote.core.data.repo.LearningRepository
import app.tote.core.matching.FieldSignature
import app.tote.core.model.Entry
import app.tote.feature.editor.ToteDialog
import app.tote.ui.components.PillButton
import app.tote.ui.components.PillStyle
import app.tote.ui.components.ToteIconButton
import app.tote.ui.icons.EntryIconBadge
import app.tote.ui.theme.Tote
import app.tote.ui.theme.ToteShapes
import app.tote.ui.theme.ToteSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One remembered field: where it is, what it's called, and what the user picked there. */
data class LearnedField(val source: String, val signature: String, val description: String, val picks: List<Pair<Entry, Int>>)

data class LearnedUiState(val loading: Boolean = true, val fields: List<LearnedField> = emptyList()) {
    /** Grouped by website or app, in recency order. */
    val bySource: Map<String, List<LearnedField>> get() = fields.groupBy { it.source }
}

@HiltViewModel
class LearnedViewModel @Inject constructor(
    private val learning: LearningRepository,
    entries: EntryRepository,
) : ViewModel() {

    val state: StateFlow<LearnedUiState> = combine(learning.mappings, entries.entries) { mappings, all ->
        val byId = all.associateBy { it.id }
        LearnedUiState(
            loading = false,
            fields = mappings.groupBy { it.source to it.signature }.map { (key, rows) ->
                LearnedField(
                    source = key.first,
                    signature = key.second,
                    description = FieldSignature.describe(key.second),
                    picks = rows.sortedByDescending { it.hits }.mapNotNull { r -> byId[r.entryId]?.let { it to r.hits } },
                )
            }.filter { it.picks.isNotEmpty() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearnedUiState())

    fun forget(field: LearnedField) {
        viewModelScope.launch { learning.forget(field.source, field.signature) }
    }

    fun clearAll() {
        viewModelScope.launch { learning.clear() }
    }
}

@Composable
fun LearnedRoute(onBack: () -> Unit, viewModel: LearnedViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LearnedScreen(state, onBack, viewModel::forget, viewModel::clearAll)
}

/** "Learned suggestions" (Plan §8.4 Settings): see and clear what Tote remembered. */
@Composable
fun LearnedScreen(
    state: LearnedUiState,
    onBack: () -> Unit,
    onForget: (LearnedField) -> Unit,
    onClearAll: () -> Unit,
) {
    val c = Tote.colors
    var confirmClear by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .widthIn(max = ToteSpacing.maxContentWidth)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = ToteSpacing.screen, end = ToteSpacing.screen, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column {
                    ToteIconButton(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", onClick = onBack, modifier = Modifier.padding(top = 4.dp))
                    Text("Learned", style = Tote.type.display, color = c.textPrimary, modifier = Modifier.padding(top = 12.dp).semantics { heading() })
                    Text(
                        "When you pick an entry for a field, Tote suggests it first there next time. It's all kept on this phone.",
                        style = Tote.type.bodySmall,
                        color = c.textSecondary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                    )
                }
            }
            if (!state.loading && state.fields.isEmpty()) {
                item {
                    Text(
                        "Nothing learned yet. Fill a few forms with Tote and your picks will show up here.",
                        style = Tote.type.body,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    )
                }
            }
            state.bySource.forEach { (source, fields) ->
                item(key = "h-$source") {
                    Text(
                        source,
                        style = Tote.type.overline,
                        color = c.textSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp).semantics { heading() },
                    )
                }
                items(fields, key = { "${it.source}|${it.signature}" }) { field -> LearnedRow(field, onForget = { onForget(field) }) }
            }
            if (state.fields.isNotEmpty()) {
                item {
                    PillButton(
                        "Forget everything",
                        onClick = { confirmClear = true },
                        style = PillStyle.Danger,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    )
                }
            }
        }
    }
    if (confirmClear) {
        ToteDialog(
            title = "Forget everything learned?",
            text = "Suggestions go back to Tote's own matching. Your entries stay.",
            confirm = "Forget",
            onConfirm = { confirmClear = false; onClearAll() },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun LearnedRow(field: LearnedField, onForget: () -> Unit) {
    val c = Tote.colors
    val top = field.picks.first().first
    Row(
        Modifier
            .fillMaxWidth()
            .clip(ToteShapes.card)
            .background(c.surface)
            .border(1.dp, c.outline, ToteShapes.card)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("“${field.description}”", style = Tote.type.titleSmall, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                EntryIconBadge(top.icon, size = 22.dp)
                Spacer(Modifier.width(8.dp))
                val others = field.picks.size - 1
                Text(
                    top.label + if (others > 0) " and $others more" else "",
                    style = Tote.type.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        ToteIconButton(Icons.Rounded.Close, contentDescription = "Forget “${field.description}”", onClick = onForget, tint = c.textSecondary)
    }
}

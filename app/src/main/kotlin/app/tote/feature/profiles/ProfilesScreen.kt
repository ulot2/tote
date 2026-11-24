package app.tote.feature.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.tote.core.data.repo.EntryRepository
import app.tote.core.data.repo.ProfileRepository
import app.tote.core.data.settings.SettingsRepository
import app.tote.core.model.Profile
import app.tote.feature.editor.ToteDialog
import app.tote.ui.components.PillButton
import app.tote.ui.components.PillStyle
import app.tote.ui.components.ToteIconButton
import app.tote.ui.components.ToteTextField
import app.tote.ui.theme.Tuck
import app.tote.ui.theme.ToteShapes
import app.tote.ui.theme.ToteSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileRow(val profile: Profile, val entryCount: Int, val active: Boolean)

data class ProfilesUiState(val loading: Boolean = true, val rows: List<ProfileRow> = emptyList(), val allCount: Int = 0)

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository,
    entries: EntryRepository,
) : ViewModel() {

    val state: StateFlow<ProfilesUiState> = combine(profiles.profiles, entries.entries, settings.settings) { list, all, s ->
        ProfilesUiState(
            loading = false,
            rows = list.map { p -> ProfileRow(p, all.count { p.id in it.profileIds }, s.activeProfileId == p.id) },
            allCount = all.count { it.isInAllProfiles },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilesUiState())

    fun add(name: String) {
        viewModelScope.launch { profiles.add(name) }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { profiles.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { profiles.delete(id) }
    }

    fun activate(id: Long?) {
        viewModelScope.launch { settings.setActiveProfile(id) }
    }
}

@Composable
fun ProfilesRoute(onBack: () -> Unit, viewModel: ProfilesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfilesScreen(state, onBack, viewModel::add, viewModel::rename, viewModel::delete, viewModel::activate)
}

/** Manage profiles (Plan §8.4 Settings, §6.2). */
@Composable
fun ProfilesScreen(
    state: ProfilesUiState,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onActivate: (Long?) -> Unit,
) {
    val c = Tuck.colors
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Profile?>(null) }
    var deleting by remember { mutableStateOf<Profile?>(null) }
    Box(Modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.fillMaxSize().widthIn(max = ToteSpacing.maxContentWidth).statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(start = ToteSpacing.screen, end = ToteSpacing.screen, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column {
                    ToteIconButton(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", onClick = onBack, modifier = Modifier.padding(top = 4.dp))
                    Text("Profiles", style = Tuck.type.display, color = c.textPrimary, modifier = Modifier.padding(top = 12.dp).semantics { heading() })
                    Text(
                        "Keep different versions apart, like a Tech CV and a Non-tech CV. The active profile decides what Tuck suggests; entries in \"All\" always show.",
                        style = Tuck.type.bodySmall,
                        color = c.textSecondary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                    )
                }
            }
            item {
                ProfileCard(
                    name = "All",
                    detail = "${state.allCount} ${if (state.allCount == 1) "entry" else "entries"} in every profile",
                    active = state.rows.none { it.active },
                    onClick = { onActivate(null) },
                )
            }
            items(state.rows, key = { it.profile.id }) { row ->
                ProfileCard(
                    name = row.profile.name,
                    detail = "${row.entryCount} ${if (row.entryCount == 1) "entry" else "entries"} of its own",
                    active = row.active,
                    onClick = { onActivate(row.profile.id) },
                    onRename = { renaming = row.profile },
                    onDelete = { deleting = row.profile },
                )
            }
            item {
                PillButton("New profile", onClick = { adding = true }, icon = Icons.Rounded.Add, style = PillStyle.Secondary, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
    if (adding) NameDialog("New profile", "", onDone = { onAdd(it); adding = false }, onDismiss = { adding = false })
    renaming?.let { p -> NameDialog("Rename profile", p.name, onDone = { onRename(p.id, it); renaming = null }, onDismiss = { renaming = null }) }
    deleting?.let { p ->
        ToteDialog(
            title = "Delete “${p.name}”?",
            text = "Its entries stay and move to All.",
            confirm = "Delete",
            onConfirm = { onDelete(p.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun ProfileCard(
    name: String,
    detail: String,
    active: Boolean,
    onClick: () -> Unit,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val c = Tuck.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(ToteShapes.card)
            .background(if (active) c.accentSoft else c.surface)
            .border(1.dp, if (active) c.accent else c.outline, ToteShapes.card)
            .clickable(role = Role.RadioButton, onClickLabel = "Make active", onClick = onClick)
            .defaultMinSize(minHeight = 64.dp)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = Tuck.type.titleSmall, color = c.textPrimary)
                if (active) {
                    Spacer(Modifier.width(8.dp))
                    // A word, not just the colour, says which profile is active.
                    Text(
                        "Active",
                        style = Tuck.type.overline,
                        color = c.onAccent,
                        modifier = Modifier.background(c.accent, ToteShapes.pill).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(detail, style = Tuck.type.bodySmall, color = c.textSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        if (onRename != null) ToteIconButton(Icons.Rounded.Edit, contentDescription = "Rename $name", onClick = onRename, tint = c.textSecondary)
        if (onDelete != null) ToteIconButton(Icons.Rounded.DeleteOutline, contentDescription = "Delete $name", onClick = onDelete, tint = c.textSecondary)
    }
}

@Composable
fun NameDialog(title: String, initial: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    val c = Tuck.colors
    var name by remember { mutableStateOf(initial) }
    val valid = name.isNotBlank() && name.trim().length <= 24
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = ToteShapes.cardLarge,
        title = { Text(title, style = Tuck.type.title, color = c.textPrimary) },
        text = {
            ToteTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = "Name",
                placeholder = "e.g. Tech",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) onDone(name.trim()) }),
            )
        },
        confirmButton = { PillButton("Save", onClick = { onDone(name.trim()) }, enabled = valid) },
        dismissButton = { PillButton("Cancel", onClick = onDismiss, style = PillStyle.Ghost) },
    )
}

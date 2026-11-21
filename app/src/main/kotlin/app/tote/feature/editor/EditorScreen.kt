package app.tote.feature.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tote.core.model.Category
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType
import app.tote.core.model.EntryValidator.Issue
import app.tote.core.model.ValueFormatter
import app.tote.feature.vault.SheetHandle
import app.tote.feature.vault.title
import app.tote.ui.components.FieldMessage
import app.tote.ui.components.FieldMessageKind
import app.tote.ui.components.KeywordChip
import app.tote.ui.components.PillButton
import app.tote.ui.components.PillStyle
import app.tote.ui.components.ToteChip
import app.tote.ui.components.ToteIconButton
import app.tote.ui.components.ToteTextField
import app.tote.ui.icons.EntryIconBadge
import app.tote.ui.icons.displayName
import app.tote.ui.nav.EditorRoute
import app.tote.ui.nav.SharedKeys
import app.tote.ui.nav.sharedContainer
import app.tote.ui.theme.Tote
import app.tote.ui.theme.ToteShapes
import app.tote.ui.theme.ToteSpacing

@Composable
fun EditorRoute(
    route: EditorRoute,
    onDone: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onDone() }
    EditorScreen(
        state = state,
        sharedKey = SharedKeys.entry(route.id, route.origin),
        onClose = onDone,
        onLabelChange = viewModel::onLabelChange,
        onValueChange = viewModel::onValueChange,
        onPaste = viewModel::pasteFromClipboard,
        onTypeSelected = viewModel::onTypeSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onIconSelected = viewModel::onIconSelected,
        onKeywordDraftChange = viewModel::onKeywordDraftChange,
        onAddKeyword = viewModel::addKeyword,
        onRemoveKeyword = viewModel::removeKeyword,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@Composable
fun EditorScreen(
    state: EditorUiState,
    sharedKey: String,
    onClose: () -> Unit,
    onLabelChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onPaste: () -> Unit,
    onTypeSelected: (EntryType) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onIconSelected: (EntryIcon?) -> Unit,
    onKeywordDraftChange: (String) -> Unit,
    onAddKeyword: () -> Unit,
    onRemoveKeyword: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = Tote.colors
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val close = { if (state.dirty) confirmDiscard = true else onClose() }
    BackHandler(enabled = state.dirty) { confirmDiscard = true }

    Box(
        Modifier
            .fillMaxSize()
            .sharedContainer(sharedKey, ToteShapes.card)
            .background(c.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = ToteSpacing.maxContentWidth)
                .statusBarsPadding()
                .imePadding(),
        ) {
            // Top bar.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = ToteSpacing.screen, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToteIconButton(Icons.Rounded.Close, contentDescription = "Close", onClick = close)
                Text(
                    if (state.isNew) "New entry" else "Edit entry",
                    style = Tote.type.title,
                    color = c.textPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .semantics { heading() },
                )
                PillButton(
                    text = "Save",
                    onClick = onSave,
                    enabled = !state.loading,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                )
            }

            if (state.loading) return@Column

            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ToteSpacing.screen)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                AutofillPreview(state, Modifier.padding(top = 12.dp))

                val errors = if (state.showErrors) state.validation.errors else emptyList()
                ToteTextField(
                    value = state.label,
                    onValueChange = onLabelChange,
                    label = "Label",
                    placeholder = "e.g. LinkedIn",
                    textStyle = Tote.type.titleSmall,
                    message = when {
                        Issue.LabelBlank in errors -> FieldMessage("Give it a name you'll recognise.", FieldMessageKind.Error)
                        Issue.LabelTooLong in state.validation.errors -> FieldMessage("Keep it under 40 characters.", FieldMessageKind.Error)
                        else -> null
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )

                ValueField(state, errors, onValueChange, onPaste)

                Group("Type", hint = if (state.typeAuto && state.value.isNotBlank()) "Detected from value" else null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EntryType.entries.forEach { t ->
                            ToteChip(t.title(), selected = state.type == t, onClick = { onTypeSelected(t) })
                        }
                    }
                }

                Group("Category", hint = if (state.categoryAuto && state.value.isNotBlank()) "Suggested" else null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Category.entries.forEach { cat ->
                            ToteChip(cat.title(), selected = state.category == cat, onClick = { onCategorySelected(cat) })
                        }
                    }
                }

                Group("Icon", hint = if (state.iconAuto) "Automatic" else null) {
                    IconPicker(state.icon, state.iconAuto, onIconSelected)
                }

                Group("Keywords", hint = "Extra words that should suggest this entry") {
                    if (state.keywords.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 10.dp),
                        ) {
                            state.keywords.forEach { k -> KeywordChip(k, onRemove = { onRemoveKeyword(k) }) }
                        }
                    }
                    ToteTextField(
                        value = state.keywordDraft,
                        onValueChange = onKeywordDraftChange,
                        label = "Add keyword",
                        placeholder = "e.g. personal site",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onAddKeyword() }),
                        trailing = {
                            AnimatedVisibility(state.keywordDraft.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
                                PillButton(
                                    "Add",
                                    onClick = onAddKeyword,
                                    style = PillStyle.Secondary,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        },
                    )
                }

                if (!state.isNew) {
                    PillButton(
                        "Delete entry",
                        onClick = { confirmDelete = true },
                        style = PillStyle.Danger,
                        icon = Icons.Rounded.DeleteOutline,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (confirmDiscard) {
        ToteDialog(
            title = "Discard changes?",
            text = "Your edits to this entry won't be saved.",
            confirm = "Discard",
            onConfirm = { confirmDiscard = false; onClose() },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (confirmDelete) {
        ToteDialog(
            title = "Delete “${state.label}”?",
            text = "This removes it from your vault.",
            confirm = "Delete",
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ValueField(state: EditorUiState, errors: List<Issue>, onValueChange: (String) -> Unit, onPaste: () -> Unit) {
    val isLong = state.type == EntryType.LONG_TEXT
    val mono = state.type == EntryType.LINK || state.type == EntryType.EMAIL || state.type == EntryType.PHONE
    val warning = state.validation.warnings.firstOrNull()
    ToteTextField(
        value = state.value,
        onValueChange = onValueChange,
        label = "Value",
        placeholder = state.placeholder.ifEmpty {
            when (state.type) {
                EntryType.LINK -> "yourname.com"
                EntryType.EMAIL -> "you@example.com"
                EntryType.PHONE -> "+1 555 010 0000"
                EntryType.LONG_TEXT -> "Write it once, reuse it everywhere…"
                EntryType.SHORT_TEXT -> "The text to insert"
            }
        },
        textStyle = if (mono) Tote.type.monoLarge else Tote.type.body,
        singleLine = !isLong,
        minLines = if (isLong) 5 else 1,
        message = when {
            Issue.ValueBlank in errors -> FieldMessage("Add the value you want to reuse.", FieldMessageKind.Error)
            warning == Issue.EmailLooksWrong -> FieldMessage("That doesn't look like an email address.", FieldMessageKind.Warning)
            warning == Issue.LinkLooksWrong -> FieldMessage("That doesn't look like a link.", FieldMessageKind.Warning)
            warning == Issue.PhoneLooksWrong -> FieldMessage("That looks short for a phone number.", FieldMessageKind.Warning)
            else -> null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = when (state.type) {
                EntryType.LINK -> KeyboardType.Uri
                EntryType.EMAIL -> KeyboardType.Email
                EntryType.PHONE -> KeyboardType.Phone
                else -> KeyboardType.Text
            },
            capitalization = if (mono) KeyboardCapitalization.None else KeyboardCapitalization.Sentences,
            autoCorrectEnabled = !mono,
            imeAction = if (isLong) ImeAction.Default else ImeAction.Next,
        ),
        trailing = {
            PillButton(
                "Paste",
                onClick = onPaste,
                style = PillStyle.Secondary,
                icon = Icons.Rounded.ContentPaste,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            )
        },
    )
}

/** Live preview of how this entry will look as an autofill suggestion (Plan §8.4). */
@Composable
private fun AutofillPreview(state: EditorUiState, modifier: Modifier = Modifier) {
    val c = Tote.colors
    val label = state.label.ifBlank { "Label" }
    val value = if (state.value.isBlank()) state.placeholder.ifBlank { "value" } else ValueFormatter.display(state.type, state.value)
    Column(
        modifier
            .fillMaxWidth()
            .clip(ToteShapes.cardLarge)
            .background(c.surface)
            .border(1.dp, c.outline, ToteShapes.cardLarge)
            .padding(16.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Autofill preview: $label, $value" },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = c.accentContent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("AUTOFILL PREVIEW", style = Tote.type.overline, color = c.textSecondary)
        }
        // Dropdown row.
        Row(
            Modifier
                .fillMaxWidth()
                .background(c.surfaceRaised, ToteShapes.field)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EntryIconBadge(state.icon, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = Tote.type.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    value,
                    style = if (state.type == EntryType.LINK || state.type == EntryType.EMAIL || state.type == EntryType.PHONE) Tote.type.mono else Tote.type.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Inline chip, as shown above the keyboard on Android 11+.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .background(c.surfaceRaised, ToteShapes.pill)
                    .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EntryIconBadge(state.icon, size = 22.dp)
                Spacer(Modifier.width(8.dp))
                Text(label, style = Tote.type.label, color = c.textPrimary, maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            Text("above the keyboard", style = Tote.type.bodySmall, color = c.textTertiary)
        }
    }
}

@Composable
private fun Group(title: String, hint: String? = null, content: @Composable () -> Unit) {
    val c = Tote.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = Tote.type.label, color = c.textSecondary)
            if (hint != null) {
                Spacer(Modifier.width(8.dp))
                Text("· $hint", style = Tote.type.bodySmall, color = c.textTertiary)
            }
        }
        content()
    }
}

@Composable
private fun IconPicker(selected: EntryIcon, auto: Boolean, onSelect: (EntryIcon?) -> Unit) {
    val c = Tote.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // "Auto" option.
        IconOption(
            selected = auto,
            description = "Automatic icon",
            onClick = { onSelect(null) },
        ) {
            Box(Modifier.size(40.dp).background(c.accentSoft, ToteShapes.badge), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = c.accentContent, modifier = Modifier.size(20.dp))
            }
        }
        EntryIcon.entries.forEach { icon ->
            IconOption(
                selected = !auto && selected == icon,
                description = icon.displayName(),
                onClick = { onSelect(icon) },
            ) { EntryIconBadge(icon, size = 40.dp) }
        }
    }
}

@Composable
private fun IconOption(selected: Boolean, description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = Tote.colors
    val ring by animateColorAsState(if (selected) c.accentContent else Color.Transparent, label = "iconRing")
    Box(
        Modifier
            .size(52.dp)
            .clip(ToteShapes.field)
            .border(2.dp, ring, ToteShapes.field)
            .semantics {
                contentDescription = description
                this.selected = selected
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun ToteDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = Tote.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = ToteShapes.cardLarge,
        title = { Text(title, style = Tote.type.title, color = c.textPrimary) },
        text = { Text(text, style = Tote.type.body, color = c.textSecondary) },
        confirmButton = { PillButton(confirm, onClick = onConfirm, style = PillStyle.Danger) },
        dismissButton = { PillButton("Cancel", onClick = onDismiss, style = PillStyle.Ghost) },
    )
}

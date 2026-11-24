package app.tote.feature.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tote.core.model.Category
import app.tote.core.model.Entry
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType
import app.tote.core.model.EntryValidator.Issue
import app.tote.core.model.ValueFormatter
import app.tote.feature.vault.SheetHandle
import app.tote.feature.vault.title
import app.tote.ui.components.GroupDivider
import app.tote.ui.components.KeywordChip
import app.tote.ui.components.PillButton
import app.tote.ui.components.PillStyle
import app.tote.ui.components.Segmented
import app.tote.ui.components.ToteChip
import app.tote.ui.components.ToteIconButton
import app.tote.ui.components.brandTint
import app.tote.ui.icons.EntryIconBadge
import app.tote.ui.icons.displayName
import app.tote.ui.nav.EditorRoute
import app.tote.ui.nav.SharedKeys
import app.tote.ui.nav.sharedContainer
import app.tote.ui.theme.ToteShapes
import app.tote.ui.theme.ToteSpacing
import app.tote.ui.theme.Tuck

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
        onProfileToggled = viewModel::onProfileToggled,
    )
}

/** Short labels for the type control; "Long" is long text (bios, answers). */
private fun EntryType.short(): String = when (this) {
    EntryType.LINK -> "Link"
    EntryType.EMAIL -> "Email"
    EntryType.PHONE -> "Phone"
    EntryType.SHORT_TEXT -> "Text"
    EntryType.LONG_TEXT -> "Long"
}

/**
 * The entry editor (Plan §8.4, redesign): a hero card that is also the autofill preview (tap its
 * icon to change it), the fields as one group, type as a segmented control, then section,
 * profiles and the keywords Tuck already matches.
 */
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
    onProfileToggled: (Long?) -> Unit = {},
) {
    val c = Tuck.colors
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickingIcon by remember { mutableStateOf(false) }
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = ToteSpacing.screen, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToteIconButton(Icons.Rounded.Close, contentDescription = "Close", onClick = close)
                Text(
                    if (state.isNew) "New entry" else "Edit entry",
                    style = Tuck.type.title,
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
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                )
            }

            if (state.loading) return@Column

            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ToteSpacing.screen)
                    .navigationBarsPadding()
                    .padding(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Hero(state, onChangeIcon = { pickingIcon = true })

                val errors = if (state.showErrors) state.validation.errors else emptyList()
                DetailsGroup(state, errors, onLabelChange, onValueChange, onPaste)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GroupLabel("Type", if (state.typeAuto && state.value.isNotBlank()) "Detected from the value" else null)
                    Segmented(
                        options = EntryType.entries,
                        selected = state.type,
                        label = { it.short() },
                        onSelect = onTypeSelected,
                        scrollAfter = 5,
                    )
                }

                SectionAndProfiles(state, onCategorySelected, onProfileToggled)

                Keywords(state, onKeywordDraftChange, onAddKeyword, onRemoveKeyword)

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

    if (pickingIcon) IconSheet(state, onDismiss = { pickingIcon = false }, onSelect = { onIconSelected(it); pickingIcon = false })
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

private fun EditorUiState.previewEntry() =
    Entry(label = label.ifBlank { "Label" }, value = value, type = type, category = category, icon = icon)

/** The entry as it will look: its card, and its chip in forms (Plan §8.4 live preview). */
@Composable
private fun Hero(state: EditorUiState, onChangeIcon: () -> Unit) {
    val c = Tuck.colors
    val entry = state.previewEntry()
    val (tint, border) = brandTint(entry)
    val valueText = if (state.value.isBlank()) state.placeholder.ifBlank { "Value" } else ValueFormatter.display(state.type, state.value)
    val mono = state.type == EntryType.LINK || state.type == EntryType.EMAIL || state.type == EntryType.PHONE
    Column(
        Modifier
            .fillMaxWidth()
            .clip(ToteShapes.cardLarge)
            .background(tint)
            .border(1.dp, border, ToteShapes.cardLarge)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(60.dp)
                    .clip(ToteShapes.field)
                    .clickable(role = Role.Button, onClickLabel = "Change icon", onClick = onChangeIcon)
                    .semantics { contentDescription = "Icon: ${state.icon.displayName()}. Change icon" },
            ) {
                EntryIconBadge(state.icon, size = 56.dp, modifier = Modifier.align(Alignment.TopStart))
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .background(c.textPrimary, ToteShapes.pill)
                        .border(2.dp, tint, ToteShapes.pill),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Edit, contentDescription = null, tint = c.background, modifier = Modifier.size(12.dp)) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.label.ifBlank { if (state.isNew) "New entry" else "Label" },
                    style = Tuck.type.headline,
                    color = if (state.label.isBlank()) c.textTertiary else c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    valueText,
                    style = if (mono) Tuck.type.mono else Tuck.type.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(Modifier.padding(vertical = 14.dp).fillMaxWidth().height(1.dp).background(border))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("IN FORMS", style = Tuck.type.overline, color = c.textSecondary, modifier = Modifier.weight(1f))
            Row(
                Modifier
                    .background(c.background.copy(alpha = 0.6f), ToteShapes.pill)
                    .border(1.dp, border, ToteShapes.pill)
                    .padding(start = 5.dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
                    .semantics(mergeDescendants = true) { contentDescription = "Autofill preview: ${entry.label}" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                EntryIconBadge(state.icon, size = 22.dp)
                Text(entry.label, style = Tuck.type.label, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
                Text(valueText, style = Tuck.type.mono.copy(fontSize = Tuck.type.mono.fontSize * 0.9f), color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 110.dp))
            }
        }
    }
}

@Composable
private fun GroupLabel(title: String, hint: String? = null) {
    val c = Tuck.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Tuck.type.label, color = c.textSecondary, modifier = Modifier.weight(1f).semantics { heading() })
        if (hint != null) Text(hint, style = Tuck.type.bodySmall, color = c.textTertiary)
    }
}

/** A labelled field inside a group row: label above, borderless input. */
@Composable
private fun GroupField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    isError: Boolean,
    errorText: String?,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val c = Tuck.colors
    Column(modifier) {
        Text(label, style = Tuck.type.bodySmall, color = if (isError) c.danger else c.textSecondary)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = textStyle.copy(color = c.textPrimary),
            cursorBrush = SolidColor(c.accentContent),
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (isError && errorText != null) error(errorText)
                },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = textStyle, color = c.textTertiary, maxLines = if (singleLine) 1 else Int.MAX_VALUE)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun DetailsGroup(
    state: EditorUiState,
    errors: List<Issue>,
    onLabelChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onPaste: () -> Unit,
) {
    val c = Tuck.colors
    val isLong = state.type == EntryType.LONG_TEXT
    val mono = state.type == EntryType.LINK || state.type == EntryType.EMAIL || state.type == EntryType.PHONE
    val labelError = when {
        Issue.LabelBlank in errors -> "Give it a name you'll recognise."
        Issue.LabelTooLong in state.validation.errors -> "Keep it under 40 characters."
        else -> null
    }
    val valueError = if (Issue.ValueBlank in errors) "Add the value you want to reuse." else null
    val warning = when (state.validation.warnings.firstOrNull()) {
        Issue.EmailLooksWrong -> "That doesn't look like an email address."
        Issue.LinkLooksWrong -> "That doesn't look like a link."
        Issue.PhoneLooksWrong -> "That looks short for a phone number."
        else -> null
    }
    Column {
        Column(Modifier.fillMaxWidth().clip(ToteShapes.card).background(c.surface)) {
            GroupField(
                label = "Label",
                value = state.label,
                onValueChange = onLabelChange,
                placeholder = "e.g. LinkedIn",
                textStyle = Tuck.type.titleSmall,
                isError = labelError != null,
                errorText = labelError,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            GroupDivider(inset = 16)
            Row(Modifier.padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                GroupField(
                    label = "Value",
                    value = state.value,
                    onValueChange = onValueChange,
                    placeholder = state.placeholder.ifEmpty {
                        when (state.type) {
                            EntryType.LINK -> "yourname.com"
                            EntryType.EMAIL -> "you@example.com"
                            EntryType.PHONE -> "+1 555 010 0000"
                            EntryType.LONG_TEXT -> "Write it once, reuse it everywhere…"
                            EntryType.SHORT_TEXT -> "The text to insert"
                        }
                    },
                    textStyle = if (mono) Tuck.type.monoLarge else Tuck.type.body,
                    isError = valueError != null,
                    errorText = valueError,
                    singleLine = !isLong,
                    minLines = if (isLong) 4 else 1,
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
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                PillButton(
                    "Paste",
                    onClick = onPaste,
                    style = PillStyle.Secondary,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        val message = labelError ?: valueError ?: warning
        AnimatedVisibility(message != null) {
            Text(
                message.orEmpty(),
                style = Tuck.type.bodySmall,
                color = if (labelError != null || valueError != null) c.danger else c.warning,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun SectionAndProfiles(state: EditorUiState, onCategorySelected: (Category) -> Unit, onProfileToggled: (Long?) -> Unit) {
    val c = Tuck.colors
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(ToteShapes.card).background(c.surface)) {
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClickLabel = "Change section") { menu = true }
                    .defaultMinSize(minHeight = 56.dp)
                    .padding(start = 16.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Section", style = Tuck.type.body, color = c.textPrimary, modifier = Modifier.weight(1f))
                Text(state.category.title(), style = Tuck.type.body, color = c.textSecondary)
                if (state.categoryAuto && state.value.isNotBlank()) {
                    Text(" · suggested", style = Tuck.type.bodySmall, color = c.textTertiary)
                }
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = c.textTertiary, modifier = Modifier.padding(start = 4.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.surfaceRaised, shape = ToteShapes.field) {
                Category.entries.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.title(), style = Tuck.type.body, color = c.textPrimary) },
                        onClick = { onCategorySelected(cat); menu = false },
                        modifier = Modifier.semantics { selected = cat == state.category },
                    )
                }
            }
        }
        if (state.profiles.isNotEmpty()) {
            GroupDivider(inset = 16)
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
                Text("Profiles", style = Tuck.type.body, color = c.textPrimary)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToteChip("All profiles", selected = state.profileIds.isEmpty(), onClick = { onProfileToggled(null) })
                    state.profiles.forEach { p ->
                        ToteChip(p.name, selected = p.id in state.profileIds, onClick = { onProfileToggled(p.id) }, role = Role.Checkbox)
                    }
                }
            }
        }
    }
}

/**
 * What Tuck already matches for this kind of entry (muted, read-only), then the user's own
 * keywords (removable), then a way to add one. The user only adds what's missing.
 */
@Composable
private fun Keywords(
    state: EditorUiState,
    onKeywordDraftChange: (String) -> Unit,
    onAddKeyword: () -> Unit,
    onRemoveKeyword: (String) -> Unit,
) {
    val c = Tuck.colors
    var expanded by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(adding) { if (adding) focus.requestFocus() }
    val builtIns = state.builtInKeywords
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupLabel(
            "Matches fields like",
            if (builtIns.isEmpty()) null else "${builtIns.size} built in",
        )
        if (builtIns.isEmpty()) {
            Text(
                "Tuck doesn't recognise this kind of entry yet, so add the words forms use for it.",
                style = Tuck.type.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val shown = if (expanded) builtIns else builtIns.take(COLLAPSED_BUILT_INS)
            shown.forEach { phrase ->
                Text(
                    phrase,
                    style = Tuck.type.bodySmall,
                    color = c.textSecondary,
                    modifier = Modifier
                        .background(c.surface, ToteShapes.pill)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            val hidden = builtIns.size - COLLAPSED_BUILT_INS
            if (hidden > 0) {
                Text(
                    if (expanded) "Show fewer" else "+$hidden",
                    style = Tuck.type.label,
                    color = c.accentContent,
                    modifier = Modifier
                        .clip(ToteShapes.pill)
                        .clickable(role = Role.Button, onClickLabel = if (expanded) "Show fewer" else "Show all built-in keywords") { expanded = !expanded }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            state.keywords.forEach { k -> KeywordChip(k, onRemove = { onRemoveKeyword(k) }) }
            if (!adding) {
                Text(
                    "+ Add your own",
                    style = Tuck.type.label,
                    color = c.accentContent,
                    modifier = Modifier
                        .clip(ToteShapes.pill)
                        .border(1.dp, c.outlineStrong, ToteShapes.pill)
                        .clickable(role = Role.Button) { adding = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        AnimatedVisibility(adding) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface, ToteShapes.pill)
                    .border(1.dp, c.accentContent, ToteShapes.pill)
                    .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = state.keywordDraft,
                    onValueChange = onKeywordDraftChange,
                    singleLine = true,
                    textStyle = Tuck.type.body.copy(color = c.textPrimary),
                    cursorBrush = SolidColor(c.accentContent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAddKeyword() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focus)
                        .semantics { contentDescription = "Add keyword" },
                    decorationBox = { inner ->
                        Box {
                            if (state.keywordDraft.isEmpty()) Text("e.g. personal site", style = Tuck.type.body, color = c.textTertiary)
                            inner()
                        }
                    },
                )
                PillButton(
                    "Add",
                    onClick = { onAddKeyword() },
                    enabled = state.keywordDraft.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private const val COLLAPSED_BUILT_INS = 6

/** Icon choice, opened by tapping the hero's icon (replaces the always-visible icon grid). */
@Composable
private fun IconSheet(state: EditorUiState, onDismiss: () -> Unit, onSelect: (EntryIcon?) -> Unit) {
    val c = Tuck.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = ToteShapes.sheet,
        containerColor = c.surface,
        dragHandle = { SheetHandle() },
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Icon", style = Tuck.type.title, color = c.textPrimary, modifier = Modifier.padding(vertical = 8.dp).semantics { heading() })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IconOption(selected = state.iconAuto, description = "Automatic icon", onClick = { onSelect(null) }) {
                    Box(Modifier.size(44.dp).background(c.accentSoft, ToteShapes.badge), contentAlignment = Alignment.Center) {
                        Text("Auto", style = Tuck.type.overline.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified), color = c.accentContent)
                    }
                }
                EntryIcon.entries.forEach { icon ->
                    IconOption(selected = !state.iconAuto && state.icon == icon, description = icon.displayName(), onClick = { onSelect(icon) }) {
                        EntryIconBadge(icon, size = 44.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun IconOption(selected: Boolean, description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = Tuck.colors
    val ring by animateColorAsState(if (selected) c.accentContent else Color.Transparent, label = "iconRing")
    Box(
        Modifier
            .size(56.dp)
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
    val c = Tuck.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = ToteShapes.cardLarge,
        title = { Text(title, style = Tuck.type.title, color = c.textPrimary) },
        text = { Text(text, style = Tuck.type.body, color = c.textSecondary) },
        confirmButton = { PillButton(confirm, onClick = onConfirm, style = PillStyle.Danger) },
        dismissButton = { PillButton("Cancel", onClick = onDismiss, style = PillStyle.Ghost) },
    )
}

package app.tote.feature.vault

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tote.core.model.Category
import app.tote.core.model.Entry
import app.tote.core.model.SeedTemplate
import app.tote.ui.components.CopiedPill
import app.tote.ui.components.EmptyVault
import app.tote.ui.components.EntryActions
import app.tote.ui.components.EntryCard
import app.tote.ui.components.PillButton
import app.tote.ui.components.PinnedCard
import app.tote.ui.components.ToteIconButton
import app.tote.ui.components.ToteSnackbar
import app.tote.ui.components.pressScale
import app.tote.ui.nav.EditorRoute
import app.tote.ui.nav.SharedKeys
import app.tote.ui.nav.sharedContainer
import app.tote.ui.icons.ToteIcons
import app.tote.ui.theme.Tote
import app.tote.ui.theme.ToteShapes
import app.tote.ui.theme.TotePalette
import app.tote.ui.theme.ToteSpacing
import app.tote.util.AutofillSetup
import app.tote.util.Haptics
import app.tote.util.rememberAutofillStatus
import app.tote.ui.components.compositeOn
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private data class CopyEvent(val entryId: Long, val label: String, val nonce: Long)

@Composable
fun VaultRoute(
    onOpenEditor: (EditorRoute) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bannerDismissed by viewModel.autofillBannerDismissed.collectAsStateWithLifecycle()
    val autofill = rememberAutofillStatus()
    val context = LocalContext.current
    VaultScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onCopy = viewModel::copy,
        onTogglePin = viewModel::togglePin,
        onDelete = viewModel::delete,
        onRestore = viewModel::restore,
        onSetReordering = viewModel::setReordering,
        onCommitOrder = viewModel::commitOrder,
        onOpenEditor = onOpenEditor,
        onOpenSettings = onOpenSettings,
        showAutofillBanner = autofill == AutofillSetup.Status.DISABLED && !bannerDismissed,
        onEnableAutofill = { context.startActivity(AutofillSetup.requestIntent(context)) },
        onDismissAutofillBanner = viewModel::dismissAutofillBanner,
    )
}

@Composable
fun VaultScreen(
    state: VaultUiState,
    onQueryChange: (String) -> Unit,
    onCopy: (Entry) -> Unit,
    onTogglePin: (Entry) -> Unit,
    onDelete: (Entry) -> Unit,
    onRestore: (Entry) -> Unit,
    onSetReordering: (Boolean) -> Unit,
    onCommitOrder: (List<Long>) -> Unit,
    onOpenEditor: (EditorRoute) -> Unit,
    onOpenSettings: () -> Unit,
    showAutofillBanner: Boolean = false,
    onEnableAutofill: () -> Unit = {},
    onDismissAutofillBanner: () -> Unit = {},
) {
    val c = Tote.colors
    val view = LocalView.current
    val context = LocalContext.current
    val focus = LocalFocusManager.current

    var lastCopy by remember { mutableStateOf<CopyEvent?>(null) }
    var copiedVisible by remember { mutableStateOf(false) }
    var nonce by remember { mutableLongStateOf(0L) }
    var sheetFor by remember { mutableStateOf<Entry?>(null) }
    var undoFor by remember { mutableStateOf<Entry?>(null) }

    LaunchedEffect(lastCopy) {
        if (lastCopy != null) {
            copiedVisible = true
            delay(1_400)
            copiedVisible = false
        }
    }
    LaunchedEffect(undoFor) {
        if (undoFor != null) {
            delay(4_000)
            undoFor = null
        }
    }

    fun copy(entry: Entry) {
        Haptics.tick(view)
        onCopy(entry)
        nonce += 1
        lastCopy = CopyEvent(entry.id, entry.label, nonce)
    }

    fun share(entry: Entry) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, entry.value)
        context.startActivity(Intent.createChooser(send, entry.label))
    }

    fun delete(entry: Entry) {
        onDelete(entry)
        undoFor = entry
    }

    fun actionsFor(entry: Entry, origin: String) = EntryActions(
        onCopy = { copy(entry) },
        onMore = { Haptics.longPress(view); sheetFor = entry },
        onEdit = { onOpenEditor(EditorRoute(id = entry.id, origin = origin)) },
        onTogglePin = { onTogglePin(entry) },
        onShare = { share(entry) },
        onDelete = { delete(entry) },
    )

    fun pulseFor(entry: Entry) = lastCopy?.takeIf { it.entryId == entry.id }?.nonce ?: 0L

    // Local copy of the list while reordering, so drags feel instant; persisted on drop.
    var localSections by remember { mutableStateOf(state.sections) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(state.sections) { if (!dragging) localSections = state.sections }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromId = (from.key as? String)?.removePrefix("e-")?.toLongOrNull() ?: return@rememberReorderableLazyListState
        val toId = (to.key as? String)?.removePrefix("e-")?.toLongOrNull() ?: return@rememberReorderableLazyListState
        localSections = localSections.map { section ->
            val fromIndex = section.entries.indexOfFirst { it.id == fromId }
            val toIndex = section.entries.indexOfFirst { it.id == toId }
            // Entries only move within their own category.
            if (fromIndex < 0 || toIndex < 0) section
            else section.copy(entries = section.entries.toMutableList().apply { add(toIndex, removeAt(fromIndex)) })
        }
        Haptics.segment(view)
    }

    val sections = if (state.reordering) localSections else state.sections
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24 } }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = ToteSpacing.maxContentWidth)
                .imePadding(),
            contentPadding = PaddingValues(
                start = ToteSpacing.screen,
                end = ToteSpacing.screen,
                bottom = 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header", contentType = "header") {
                VaultHeader(
                    count = state.totalCount,
                    reordering = state.reordering,
                    showReorder = state.totalCount > 1,
                    onReorder = { onSetReordering(!state.reordering) },
                    onSettings = onOpenSettings,
                )
            }

            if (state.isVaultEmpty) {
                item(key = "empty", contentType = "empty") {
                    EmptyVault(
                        onAdd = { onOpenEditor(EditorRoute(origin = EditorRoute.ORIGIN_FAB)) },
                        onTemplate = { t: SeedTemplate -> onOpenEditor(EditorRoute(template = t.key, origin = EditorRoute.ORIGIN_FAB)) },
                    )
                }
                return@LazyColumn
            }

            if (!state.reordering && !state.loading) {
                item(key = "search", contentType = "search") {
                    SearchField(state.query, onQueryChange, onClear = { onQueryChange(""); focus.clearFocus() })
                }
            }

            if (showAutofillBanner && !state.reordering) {
                item(key = "autofill-banner", contentType = "banner") {
                    AutofillBanner(onEnableAutofill, onDismissAutofillBanner, Modifier.animateItem())
                }
            }

            if (state.pinned.isNotEmpty()) {
                item(key = "pinned-title", contentType = "section") { SectionTitle("Pinned", state.pinned.size) }
                item(key = "pinned-row", contentType = "pinned") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = ToteSpacing.screen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp)
                            .layoutBleed(ToteSpacing.screen),
                    ) {
                        items(state.pinned, key = { it.id }) { entry ->
                            PinnedCard(
                                entry = entry,
                                actions = actionsFor(entry, EditorRoute.ORIGIN_PINNED),
                                pulseKey = pulseFor(entry),
                                modifier = Modifier
                                    .animateItem()
                                    .sharedContainer(SharedKeys.entry(entry.id, EditorRoute.ORIGIN_PINNED), ToteShapes.cardLarge),
                            )
                        }
                    }
                }
            }

            if (state.noResults) {
                item(key = "no-results", contentType = "empty") { NoResults(state.query) }
            }

            if (state.reordering) {
                item(key = "reorder-hint", contentType = "hint") {
                    Text(
                        "Drag the handles to reorder within each section.",
                        style = Tote.type.bodySmall,
                        color = c.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }

            sections.forEach { section ->
                item(key = "h-${section.category}", contentType = "section") {
                    SectionTitle(section.category.title(), section.entries.size, Modifier.animateItem())
                }
                items(section.entries, key = { "e-${it.id}" }, contentType = { "entry" }) { entry ->
                    ReorderableItem(reorderState, key = "e-${entry.id}", enabled = state.reordering) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 12.dp else 0.dp, label = "dragElevation")
                        EntryCard(
                            entry = entry,
                            actions = actionsFor(entry, EditorRoute.ORIGIN_LIST),
                            pulseKey = pulseFor(entry),
                            dragging = isDragging,
                            modifier = Modifier
                                .shadow(elevation, ToteShapes.card, clip = false)
                                .then(if (state.reordering) Modifier else Modifier.sharedContainer(SharedKeys.entry(entry.id, EditorRoute.ORIGIN_LIST))),
                            trailing = if (state.reordering) {
                                {
                                    Icon(
                                        Icons.Rounded.DragIndicator,
                                        contentDescription = "Reorder ${entry.label}",
                                        tint = c.textSecondary,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .draggableHandle(
                                                onDragStarted = { dragging = true; Haptics.segment(view) },
                                                onDragStopped = {
                                                    dragging = false
                                                    localSections.firstOrNull { s -> s.entries.any { it.id == entry.id } }
                                                        ?.let { s -> onCommitOrder(s.entries.map { it.id }) }
                                                },
                                            )
                                            .padding(12.dp),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }

        // Bottom overlays: FAB / Done, copied pill, undo snackbar.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = ToteSpacing.maxContentWidth)
                .navigationBarsPadding()
                .padding(horizontal = ToteSpacing.screen, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CopiedPill(if (copiedVisible) "Copied · ${lastCopy?.label.orEmpty()}" else null)
            AnimatedVisibility(
                visible = undoFor != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                val entry = undoFor
                ToteSnackbar(
                    message = "Deleted “${entry?.label.orEmpty()}”",
                    actionLabel = "Undo",
                    onAction = { entry?.let(onRestore); undoFor = null },
                )
            }
            if (!state.isVaultEmpty) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    AnimatedContent(
                        targetState = state.reordering,
                        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.9f)) },
                        label = "fab",
                    ) { reordering ->
                        if (reordering) {
                            PillButton("Done", onClick = { onSetReordering(false) }, icon = Icons.Rounded.Check)
                        } else {
                            AddFab(expanded = !scrolled, onClick = { onOpenEditor(EditorRoute(origin = EditorRoute.ORIGIN_FAB)) })
                        }
                    }
                }
            }
        }
    }

    sheetFor?.let { entry ->
        EntryActionsSheet(
            entry = entry,
            onDismiss = { sheetFor = null },
            onCopy = { copy(entry) },
            onEdit = { onOpenEditor(EditorRoute(id = entry.id, origin = EditorRoute.ORIGIN_LIST)) },
            onTogglePin = { onTogglePin(entry) },
            onShare = { share(entry) },
            onDelete = { delete(entry) },
        )
    }
}

/** Lets a horizontally scrolling row run edge to edge inside padded list content. */
private fun Modifier.layoutBleed(bleed: androidx.compose.ui.unit.Dp): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val extra = bleed.roundToPx() * 2
        val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = constraints.minWidth + extra))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed.roundToPx(), 0) }
    },
)

@Composable
private fun VaultHeader(
    count: Int,
    reordering: Boolean,
    showReorder: Boolean,
    onReorder: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = Tote.colors
    Column(Modifier.statusBarsPadding().padding(top = 8.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ToteWordmark()
            Spacer(Modifier.weight(1f))
            if (showReorder) {
                ToteIconButton(
                    icon = if (reordering) Icons.Rounded.Check else Icons.Rounded.SwapVert,
                    contentDescription = if (reordering) "Finish reordering" else "Reorder entries",
                    onClick = onReorder,
                )
            }
            ToteIconButton(icon = Icons.Rounded.Tune, contentDescription = "Settings", onClick = onSettings)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            if (reordering) "Reorder" else "Vault",
            style = Tote.type.display,
            color = c.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        if (count > 0) {
            Text(
                if (reordering) "Drag to rearrange" else "$count ${if (count == 1) "entry" else "entries"} · tap to copy",
                style = Tote.type.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp),
            )
        }
    }
}

@Composable
private fun ToteWordmark() {
    val c = Tote.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Tote" }) {
        Box(
            Modifier
                .size(30.dp)
                .clip(ToteShapes.badge)
                .background(if (c.isDark) c.surfaceRaised else TotePalette.Ink),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(app.tote.R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.requiredSize(48.dp),
            )
        }
        Spacer(Modifier.size(10.dp))
        Text("tote", style = Tote.type.title, color = c.textPrimary)
    }
}

@Composable
private fun SectionTitle(title: String, count: Int, modifier: Modifier = Modifier) {
    val c = Tote.colors
    Row(
        modifier
            .padding(top = 18.dp, bottom = 2.dp, start = 4.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), style = Tote.type.overline, color = c.textSecondary)
        Spacer(Modifier.size(8.dp))
        Text(count.toString(), style = Tote.type.overline, color = c.textTertiary)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    val c = Tote.colors
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = Tote.type.body.copy(color = c.textPrimary),
        cursorBrush = SolidColor(c.accentContent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .semantics { contentDescription = "Search entries" },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(c.surface, ToteShapes.pill)
                    .border(1.dp, c.outline, ToteShapes.pill)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search your vault", style = Tote.type.body, color = c.textTertiary)
                    inner()
                }
                if (query.isNotEmpty()) {
                    ToteIconButton(Icons.Rounded.Close, contentDescription = "Clear search", onClick = onClear, tint = c.textSecondary)
                }
            }
        },
    )
}

/** Nudges the user to turn on autofill, the main surface (Plan §2). Dismissible for good. */
@Composable
private fun AutofillBanner(onEnable: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val c = Tote.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(ToteShapes.card)
            .background(c.accentSoft.compositeOn(c.background))
            .border(1.dp, c.accent.copy(alpha = 0.5f), ToteShapes.card)
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(ToteIcons.FieldFill, contentDescription = null, tint = c.accentContent, modifier = Modifier.padding(top = 1.dp).size(22.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Fill forms in one tap", style = Tote.type.titleSmall, color = c.textPrimary)
            Text(
                "Turn on autofill and Tote suggests the right entry when you tap a field.",
                style = Tote.type.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
            PillButton(
                "Turn on",
                onClick = onEnable,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            )
        }
        ToteIconButton(Icons.Rounded.Close, contentDescription = "Dismiss", onClick = onDismiss, tint = c.textSecondary)
    }
}

@Composable
private fun NoResults(query: String) {
    val c = Tote.colors
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nothing matches “$query”", style = Tote.type.title, color = c.textPrimary)
        Text(
            "Try a label, a bit of the value, or a keyword.",
            style = Tote.type.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AddFab(expanded: Boolean, onClick: () -> Unit) {
    val c = Tote.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .sharedContainer(SharedKeys.entry(0, EditorRoute.ORIGIN_FAB), ToteShapes.pill)
            .pressScale(interaction, 0.94f)
            .shadow(16.dp, ToteShapes.pill, ambientColor = c.accent, spotColor = c.accent)
            .clip(ToteShapes.pill)
            .background(c.accent)
            .clickable(interaction, ripple(), role = Role.Button, onClickLabel = "Add entry", onClick = onClick)
            .height(60.dp)
            .padding(horizontal = 18.dp)
            .semantics { contentDescription = "New entry" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(26.dp))
        AnimatedVisibility(visible = expanded) {
            Text(
                "New entry",
                style = Tote.type.label,
                color = c.onAccent,
                modifier = Modifier.padding(start = 8.dp, end = 4.dp),
            )
        }
    }
}

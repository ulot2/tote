package app.tote.feature.picker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.tote.core.model.Entry
import app.tote.feature.vault.SheetHandle
import app.tote.ui.components.EntryValueText
import app.tote.ui.components.PillButton
import app.tote.ui.components.PillStyle
import app.tote.ui.components.ToteChip
import app.tote.ui.components.ToteIconButton
import androidx.compose.foundation.lazy.LazyRow
import app.tote.ui.icons.EntryIconBadge
import app.tote.ui.theme.Tuck
import app.tote.ui.theme.ToteShapes
import app.tote.util.Haptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** What a pick does: COPY for the tile, FILL for autofill's "All entries…". */
enum class PickerMode { COPY, FILL }

/**
 * Picker sheet (Plan §8.4): translucent, sheet-style, search at the top, pinned first then most used.
 *
 * The sheet animates itself in and out; [onFinished] is called once it has left, so the activity can
 * finish without a jarring window animation.
 */
@Composable
fun PickerScreen(
    state: PickerUiState,
    onQueryChange: (String) -> Unit,
    onPick: (Entry) -> Unit,
    onOpenApp: () -> Unit,
    onFinished: () -> Unit,
    startVisible: Boolean = false,
    mode: PickerMode = PickerMode.COPY,
    onSelectProfile: (Long?) -> Unit = {},
) {
    val c = Tuck.colors
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var visible by remember { mutableStateOf(startVisible) }
    var picked by remember { mutableStateOf<Entry?>(null) }
    var leaving by remember { mutableStateOf(false) }
    val dragOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) { visible = true }
    // Once the exit animation has run, tell the host to finish.
    LaunchedEffect(leaving) {
        if (leaving) {
            visible = false
            delay(260)
            onFinished()
        }
    }

    fun dismiss() {
        if (!leaving) leaving = true
    }

    fun pick(entry: Entry) {
        if (picked != null || leaving) return
        Haptics.tick(view)
        onPick(entry)
        picked = entry
        scope.launch {
            delay(420) // long enough to see the check mark
            dismiss()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxSheetHeight = maxHeight * 0.82f

        // Scrim.
        AnimatedVisibility(visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(200))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(c.scrim.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Close",
                        onClick = ::dismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .statusBarsPadding(),
            enter = slideInVertically(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) { it },
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(220)),
        ) {
            Column(
                Modifier
                    .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .clip(ToteShapes.sheet)
                    .background(c.surface)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                // Header doubles as the drag-to-dismiss area.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                scope.launch { dragOffset.snapTo((dragOffset.value + delta).coerceAtLeast(0f)) }
                            },
                            onDragStopped = { velocity ->
                                if (dragOffset.value > with(density) { 96.dp.toPx() } || velocity > 1800f) {
                                    dismiss()
                                } else {
                                    dragOffset.animateTo(0f, spring(dampingRatio = 0.7f))
                                }
                            },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SheetHandle()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 12.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (mode == PickerMode.FILL) "Fill from Tuck" else "Copy from Tuck",
                                style = Tuck.type.title,
                                color = c.textPrimary,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                if (mode == PickerMode.FILL) "Tap one to fill the field" else "Tap one, then paste anywhere",
                                style = Tuck.type.bodySmall,
                                color = c.textSecondary,
                            )
                        }
                        ToteIconButton(Icons.Rounded.Close, contentDescription = "Close", onClick = ::dismiss, tint = c.textSecondary, filled = true)
                    }
                }

                if (state.totalCount > 0) {
                    // Search and profile share one row (redesign).
                    Row(
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PickerSearch(state.query, onQueryChange, Modifier.weight(1f))
                        if (state.profiles.isNotEmpty()) ProfileMenu(state, onSelectProfile)
                    }
                    Text(
                        if (state.query.isBlank()) "MOST USED" else "RESULTS",
                        style = Tuck.type.overline,
                        color = c.textSecondary,
                        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp).semantics { heading() },
                    )
                }

                when {
                    state.loading -> Spacer(Modifier.height(160.dp))
                    state.totalCount == 0 -> EmptyPicker(onOpenApp)
                    state.entries.isEmpty() -> Text(
                        "Nothing matches “${state.query}”",
                        style = Tuck.type.body,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(state.entries, key = { it.id }) { entry ->
                            PickerRow(entry, copied = picked?.id == entry.id, mode = mode, onClick = { pick(entry) }, modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(entry: Entry, copied: Boolean, mode: PickerMode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Tuck.colors
    val bg = if (copied) c.accentTint else c.surface
    Row(
        modifier
            .fillMaxWidth()
            .clip(ToteShapes.card)
            .background(bg)
            .clickable(role = Role.Button, onClickLabel = if (mode == PickerMode.FILL) "Fill" else "Copy", onClick = onClick)
            .defaultMinSize(minHeight = 64.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryIconBadge(entry.icon, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.label,
                    style = Tuck.type.titleSmall,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (entry.pinned) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.PushPin, contentDescription = "Pinned", tint = c.accentContent, modifier = Modifier.size(14.dp))
                }
            }
            EntryValueText(entry)
        }
        AnimatedContent(
            targetState = copied,
            transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith fadeOut() },
            label = "copied",
        ) { done ->
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                if (done) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(c.accent, ToteShapes.pill)
                            .semantics { contentDescription = if (mode == PickerMode.FILL) "Filled" else "Copied" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = lerp(c.textTertiary, c.textSecondary, 0.3f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** The active profile as a small dropdown pill beside search. */
@Composable
private fun ProfileMenu(state: PickerUiState, onSelect: (Long?) -> Unit) {
    val c = Tuck.colors
    var open by remember { mutableStateOf(false) }
    val active = state.profiles.firstOrNull { it.id == state.activeProfileId }
    Box {
        Row(
            Modifier
                .heightIn(min = 48.dp)
                .clip(ToteShapes.pill)
                .background(if (active != null) c.accentTint else c.background)
                .border(1.dp, if (active != null) c.accentTintBorder else c.outline, ToteShapes.pill)
                .clickable(role = Role.Button, onClickLabel = "Change profile") { open = true }
                .padding(start = 14.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (active != null) Box(Modifier.size(8.dp).background(c.accent, ToteShapes.pill))
            Text(active?.name ?: "All", style = Tuck.type.label, color = c.textPrimary, maxLines = 1)
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.surfaceRaised, shape = ToteShapes.field) {
            (listOf<Long?>(null) + state.profiles.map { it.id }).forEach { id ->
                val name = state.profiles.firstOrNull { it.id == id }?.name ?: "All"
                DropdownMenuItem(
                    text = { Text(name, style = Tuck.type.body, color = c.textPrimary) },
                    trailingIcon = { if (id == state.activeProfileId) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = c.accentContent) },
                    onClick = { onSelect(id); open = false },
                )
            }
        }
    }
}

@Composable
private fun PickerSearch(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Tuck.colors
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = Tuck.type.body.copy(color = c.textPrimary),
        cursorBrush = SolidColor(c.accentContent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Search entries" },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(c.background, ToteShapes.pill)
                    .border(1.dp, c.outline, ToteShapes.pill)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search", style = Tuck.type.body, color = c.textTertiary)
                    inner()
                }
            }
        },
    )
}

@Composable
private fun EmptyPicker(onOpenApp: () -> Unit) {
    val c = Tuck.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Nothing tucked away yet", style = Tuck.type.title, color = c.textPrimary)
        Text(
            "Add your links and answers in the app, then copy them from here.",
            style = Tuck.type.bodySmall,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
        )
        PillButton("Open Tuck", onClick = onOpenApp, style = PillStyle.Primary)
    }
}

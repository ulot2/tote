package app.tote.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tote.core.model.Entry
import app.tote.core.model.EntryIconPalette
import app.tote.core.model.EntryType
import app.tote.ui.icons.EntryIconBadge
import app.tote.ui.theme.Tuck
import app.tote.ui.theme.over
import kotlinx.coroutines.delay

/** Where a row sits in its grouped section; decides which corners are rounded. */
enum class RowPosition { Only, First, Middle, Last }

private const val GROUP_RADIUS = 22

fun RowPosition.shape(radius: Int = GROUP_RADIUS): Shape = when (this) {
    RowPosition.Only -> RoundedCornerShape(radius.dp)
    RowPosition.First -> RoundedCornerShape(topStart = radius.dp, topEnd = radius.dp)
    RowPosition.Middle -> RoundedCornerShape(0.dp)
    RowPosition.Last -> RoundedCornerShape(bottomStart = radius.dp, bottomEnd = radius.dp)
}

fun positionOf(index: Int, size: Int): RowPosition = when {
    size == 1 -> RowPosition.Only
    index == 0 -> RowPosition.First
    index == size - 1 -> RowPosition.Last
    else -> RowPosition.Middle
}

/** A hairline between rows of a group, inset past the icon (redesign: one group, not many cards). */
@Composable
fun GroupDivider(inset: Int = 68) {
    Box(Modifier.fillMaxWidth().background(Tuck.colors.surface)) {
        Box(Modifier.padding(start = inset.dp).fillMaxWidth().height(1.dp).background(Tuck.colors.outline))
    }
}

/**
 * True for ~1.4 s after [pulseKey] changes to a new non-zero value: the row's "Copied" moment.
 */
@Composable
fun rememberJustCopied(pulseKey: Long): Boolean {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(pulseKey) {
        if (pulseKey != 0L) {
            copied = true
            delay(1_400)
            copied = false
        }
    }
    return copied
}

/** Small "Copied" confirmation, shown in place of a row's copy button. */
@Composable
fun CopiedBadge(modifier: Modifier = Modifier) {
    val c = Tuck.colors
    Row(
        modifier
            .heightIn(min = 32.dp)
            .background(c.accent, RoundedCornerShape(16.dp))
            .padding(start = 8.dp, end = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(16.dp))
        Text("Copied", style = Tuck.type.label, color = c.onAccent)
    }
}

private fun Modifier.entrySemantics(entry: Entry, actions: EntryActions) = semantics {
    customActions = listOf(
        CustomAccessibilityAction("Edit") { actions.onEdit(); true },
        CustomAccessibilityAction(if (entry.pinned) "Unpin" else "Pin") { actions.onTogglePin(); true },
        CustomAccessibilityAction("Share") { actions.onShare(); true },
        CustomAccessibilityAction("Delete") { actions.onDelete(); true },
    )
}

/**
 * One entry as a row of a grouped section. Tap copies (the row confirms in place), long-press
 * opens actions. [trailing] replaces the copy affordance (e.g. a drag handle in reorder mode).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryRow(
    entry: Entry,
    actions: EntryActions,
    position: RowPosition,
    modifier: Modifier = Modifier,
    pulseKey: Long = 0L,
    dragging: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Tuck.colors
    val copied = rememberJustCopied(pulseKey)
    val bg by animateColorAsState(
        when {
            copied -> c.accentTint
            dragging -> c.surfaceRaised
            else -> c.surface
        },
        label = "rowBg",
    )
    val interaction = remember { MutableInteractionSource() }
    val shape = if (dragging) RoundedCornerShape(GROUP_RADIUS.dp) else position.shape()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .entrySemantics(entry, actions)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClickLabel = "Copy",
                onLongClickLabel = "More actions",
                onLongClick = actions.onMore,
                onClick = actions.onCopy,
            )
            .defaultMinSize(minHeight = 68.dp)
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryIconBadge(entry.icon, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(entry.label, style = Tuck.type.titleSmall, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            EntryValueText(entry, maxLines = if (entry.type == EntryType.LONG_TEXT) 2 else 1)
        }
        Spacer(Modifier.width(6.dp))
        if (trailing != null) {
            trailing()
        } else {
            AnimatedContent(
                targetState = copied,
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.85f)) togetherWith fadeOut() },
                label = "copied",
            ) { done ->
                if (done) {
                    CopiedBadge(Modifier.padding(end = 4.dp))
                } else {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }
    }
}

/** Brand-tinted surface for a pinned pill or hero card; neutral icons use the raised surface. */
@Composable
fun brandTint(entry: Entry): Pair<Color, Color> {
    val c = Tuck.colors
    val brand = EntryIconPalette.brand(entry.icon, c.isDark)
        // Monochrome brands (GitHub, X, Medium) tint too faintly to read as a colour: keep neutral.
        ?.takeUnless { entry.icon == app.tote.core.model.EntryIcon.GITHUB || entry.icon == app.tote.core.model.EntryIcon.X || entry.icon == app.tote.core.model.EntryIcon.MEDIUM }
    return if (brand == null) {
        c.surfaceRaised to c.outline
    } else {
        val color = Color(brand.container)
        color.copy(alpha = if (c.isDark) 0.16f else 0.12f).over(c.surface) to color.copy(alpha = if (c.isDark) 0.42f else 0.30f).over(c.surface)
    }
}

/** A pinned entry as a compact quick-copy pill (redesign: pinned row stays small). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedPill(entry: Entry, actions: EntryActions, modifier: Modifier = Modifier, pulseKey: Long = 0L) {
    val c = Tuck.colors
    val copied = rememberJustCopied(pulseKey)
    val (tint, border) = brandTint(entry)
    val bg by animateColorAsState(if (copied) c.accentTint else tint, label = "pillBg")
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .border(1.dp, if (copied) c.accentTintBorder else border, RoundedCornerShape(24.dp))
            .entrySemantics(entry, actions)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClickLabel = "Copy",
                onLongClickLabel = "More actions",
                onLongClick = actions.onMore,
                onClick = actions.onCopy,
            )
            .padding(start = 6.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        AnimatedContent(copied, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "pinCopied") { done ->
            if (done) {
                Box(
                    Modifier.size(36.dp).background(c.accent, RoundedCornerShape(18.dp)).semantics { liveRegion = LiveRegionMode.Polite },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, contentDescription = "Copied", tint = c.onAccent, modifier = Modifier.size(20.dp)) }
            } else {
                EntryIconBadge(entry.icon, size = 36.dp, round = true)
            }
        }
        Text(entry.label, style = Tuck.type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

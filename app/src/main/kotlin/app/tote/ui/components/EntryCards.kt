package app.tote.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tote.core.model.Entry
import app.tote.core.model.EntryType
import app.tote.core.model.ValueFormatter
import app.tote.ui.icons.EntryIconBadge
import app.tote.ui.theme.Tote
import app.tote.ui.theme.ToteShapes

/** Callbacks a card exposes, also surfaced as TalkBack custom actions. */
data class EntryActions(
    val onCopy: () -> Unit,
    val onMore: () -> Unit,
    val onEdit: () -> Unit,
    val onTogglePin: () -> Unit,
    val onShare: () -> Unit,
    val onDelete: () -> Unit,
)

/**
 * Drives the copy glow: whenever [pulseKey] changes to a non-zero value, the glow flashes in
 * and fades out. Returns the current glow amount, 0..1.
 */
@Composable
fun rememberCopyGlow(pulseKey: Long): Float {
    val glow = remember { Animatable(0f) }
    LaunchedEffect(pulseKey) {
        if (pulseKey != 0L) {
            glow.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
            glow.animateTo(0f, tween(700, easing = LinearOutSlowInEasing))
        }
    }
    return glow.value
}

private fun Modifier.entryActions(entry: Entry, actions: EntryActions, interaction: MutableInteractionSource) =
    this
        .semantics {
            customActions = listOf(
                CustomAccessibilityAction("Edit") { actions.onEdit(); true },
                CustomAccessibilityAction(if (entry.pinned) "Unpin" else "Pin") { actions.onTogglePin(); true },
                CustomAccessibilityAction("Share") { actions.onShare(); true },
                CustomAccessibilityAction("Delete") { actions.onDelete(); true },
            )
        }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryCard(
    entry: Entry,
    actions: EntryActions,
    modifier: Modifier = Modifier,
    pulseKey: Long = 0L,
    trailing: (@Composable () -> Unit)? = null,
    dragging: Boolean = false,
) {
    val c = Tote.colors
    val glow = rememberCopyGlow(pulseKey)
    val interaction = remember { MutableInteractionSource() }
    val bg = lerp(if (dragging) c.surfaceRaised else c.surface, c.accentSoft.compositeOn(c.surface), glow)
    val border = lerp(if (dragging) c.outlineStrong else c.outline, c.accent, glow)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.985f)
            .clip(ToteShapes.card)
            .background(bg)
            .border(if (glow > 0.01f) 1.5.dp else 1.dp, border, ToteShapes.card)
            .entryActions(entry, actions, interaction)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClickLabel = "Copy",
                onLongClickLabel = "More actions",
                onLongClick = actions.onMore,
                onClick = actions.onCopy,
            )
            .defaultMinSize(minHeight = 72.dp)
            .padding(start = 14.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryIconBadge(entry.icon, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.label, style = Tote.type.titleSmall, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            EntryValueText(entry, maxLines = if (entry.type == EntryType.LONG_TEXT) 2 else 1)
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = null,
                tint = lerp(c.textTertiary, c.accentContent, glow),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(18.dp),
            )
        }
    }
}

@Composable
fun EntryValueText(entry: Entry, maxLines: Int = 1, modifier: Modifier = Modifier) {
    val c = Tote.colors
    val isMono = entry.type == EntryType.LINK || entry.type == EntryType.EMAIL || entry.type == EntryType.PHONE
    Text(
        ValueFormatter.display(entry.type, entry.value),
        style = if (isMono) Tote.type.mono else Tote.type.bodySmall,
        color = c.textSecondary,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Bigger card for the pinned row at the top of the vault. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedCard(
    entry: Entry,
    actions: EntryActions,
    modifier: Modifier = Modifier,
    pulseKey: Long = 0L,
) {
    val c = Tote.colors
    val glow = rememberCopyGlow(pulseKey)
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .width(172.dp)
            .pressScale(interaction, 0.97f)
            .clip(ToteShapes.cardLarge)
            .background(lerp(c.surfaceRaised, c.accentSoft.compositeOn(c.surfaceRaised), glow))
            .border(1.dp, lerp(c.outline, c.accent, glow), ToteShapes.cardLarge)
            .entryActions(entry, actions, interaction)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClickLabel = "Copy",
                onLongClickLabel = "More actions",
                onLongClick = actions.onMore,
                onClick = actions.onCopy,
            )
            .defaultMinSize(minHeight = 136.dp)
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            EntryIconBadge(entry.icon, size = 40.dp)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(24.dp)
                    .background(c.accentSoft, ToteShapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PushPin, contentDescription = "Pinned", tint = c.accentContent, modifier = Modifier.size(13.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(entry.label, style = Tote.type.title, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        EntryValueText(entry)
    }
}

/** Alpha-composites a translucent colour over an opaque one so lerps stay opaque. */
internal fun androidx.compose.ui.graphics.Color.compositeOn(base: androidx.compose.ui.graphics.Color) =
    androidx.compose.ui.graphics.Color(
        red = red * alpha + base.red * (1 - alpha),
        green = green * alpha + base.green * (1 - alpha),
        blue = blue * alpha + base.blue * (1 - alpha),
        alpha = 1f,
    )

package app.tote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.tote.ui.theme.Tuck
import app.tote.ui.theme.ToteShapes

/** Selectable pill chip. Selection is shown by fill *and* a check mark, never colour alone. */
@Composable
fun ToteChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    role: Role = Role.RadioButton,
) {
    val c = Tuck.colors
    val bg by animateColorAsState(if (selected) c.accent else c.surface, label = "chipBg")
    val fg by animateColorAsState(if (selected) c.onAccent else c.textPrimary, label = "chipFg")
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .pressScale(interaction, 0.95f)
            .defaultMinSize(minHeight = 40.dp)
            .clip(ToteShapes.pill)
            .background(bg)
            .border(1.dp, if (selected) c.accent else c.outline, ToteShapes.pill)
            .clickable(interaction, ripple(), role = role, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        leading?.invoke()
        Text(text, style = Tuck.type.label, color = fg)
    }
}

/** Removable keyword chip. */
@Composable
fun KeywordChip(text: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val c = Tuck.colors
    Row(
        modifier = modifier
            .clip(ToteShapes.pill)
            .background(c.surfaceRaised)
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = Tuck.type.label, color = c.textPrimary)
        Icon(
            Icons.Rounded.Close,
            contentDescription = null,
            tint = c.textSecondary,
            modifier = Modifier
                .size(40.dp)
                .clip(ToteShapes.pill)
                .semantics { contentDescription = "Remove keyword $text" }
                .clickable(role = Role.Button, onClick = onRemove)
                .padding(11.dp),
        )
    }
}

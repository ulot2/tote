package app.tote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tote.ui.theme.ToteShapes
import app.tote.ui.theme.Tuck

/**
 * A pill-shaped segmented control (redesign: profile switcher, type, theme). Up to [scrollAfter]
 * options share the width equally; more scroll horizontally so long lists still fit.
 * Selection shows as fill and weight, and is announced as a selected radio button.
 */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp,
    track: Color = Tuck.colors.surface,
    scrollAfter: Int = 4,
) {
    val c = Tuck.colors
    val scrolls = options.size > scrollAfter
    Row(
        modifier
            .then(if (scrolls) Modifier else Modifier.fillMaxWidth())
            .clip(ToteShapes.pill)
            .background(track)
            .border(1.dp, c.outline, ToteShapes.pill)
            .then(if (scrolls) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val bg by animateColorAsState(if (isSelected) c.accent else Color.Transparent, label = "segBg")
            val fg by animateColorAsState(if (isSelected) c.onAccent else c.textSecondary, label = "segFg")
            Box(
                Modifier
                    .then(if (scrolls) Modifier else Modifier.weight(1f))
                    .clip(ToteShapes.pill)
                    .background(bg)
                    .selectable(isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                    .defaultMinSize(minHeight = height)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = Tuck.type.label.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

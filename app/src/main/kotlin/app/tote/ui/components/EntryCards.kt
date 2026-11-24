package app.tote.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.tote.core.model.Entry
import app.tote.core.model.EntryType
import app.tote.core.model.ValueFormatter
import app.tote.ui.theme.Tuck

/** Callbacks an entry exposes (rows, pinned pills), also surfaced as TalkBack custom actions. */
data class EntryActions(
    val onCopy: () -> Unit,
    val onMore: () -> Unit,
    val onEdit: () -> Unit,
    val onTogglePin: () -> Unit,
    val onShare: () -> Unit,
    val onDelete: () -> Unit,
)

/** An entry's value as shown in lists: links, emails and phones in mono, text in the body face. */
@Composable
fun EntryValueText(entry: Entry, maxLines: Int = 1, modifier: Modifier = Modifier) {
    val c = Tuck.colors
    val isMono = entry.type == EntryType.LINK || entry.type == EntryType.EMAIL || entry.type == EntryType.PHONE
    Text(
        ValueFormatter.display(entry.type, entry.value),
        style = if (isMono) Tuck.type.mono else Tuck.type.bodySmall,
        color = c.textSecondary,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

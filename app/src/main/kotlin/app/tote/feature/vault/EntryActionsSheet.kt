package app.tote.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tote.core.model.Entry
import app.tote.ui.components.EntryValueText
import app.tote.ui.icons.EntryIconBadge
import app.tote.ui.theme.Tuck
import app.tote.ui.theme.ToteShapes
import kotlinx.coroutines.launch

@Composable
fun SheetHandle() {
    Box(
        Modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .size(width = 36.dp, height = 4.dp)
            .background(Tuck.colors.outlineStrong, ToteShapes.pill),
    )
}

/** Long-press actions for an entry (Plan §8.4: edit, pin, share, delete). */
@Composable
fun EntryActionsSheet(
    entry: Entry,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = Tuck.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun dismissThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = ToteShapes.sheet,
        containerColor = c.surface,
        scrimColor = c.scrim.copy(alpha = 0.5f),
        dragHandle = { SheetHandle() },
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                EntryIconBadge(entry.icon, size = 48.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.label, style = Tuck.type.title, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    EntryValueText(entry, maxLines = 2)
                }
            }
            Spacer(Modifier.height(8.dp))
            ActionRow(Icons.Rounded.ContentCopy, "Copy") { dismissThen(onCopy) }
            ActionRow(Icons.Rounded.Edit, "Edit") { dismissThen(onEdit) }
            ActionRow(
                if (entry.pinned) Icons.Outlined.PushPin else Icons.Rounded.PushPin,
                if (entry.pinned) "Unpin" else "Pin to top",
            ) { dismissThen(onTogglePin) }
            ActionRow(Icons.Rounded.Share, "Share") { dismissThen(onShare) }
            ActionRow(Icons.Rounded.DeleteOutline, "Delete", tint = c.danger) { dismissThen(onDelete) }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, tint: Color = Tuck.colors.textPrimary, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(ToteShapes.field)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = Tuck.type.body, color = tint)
    }
}

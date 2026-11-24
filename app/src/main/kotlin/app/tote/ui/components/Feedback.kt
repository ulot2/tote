package app.tote.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tote.ui.theme.Tuck
import app.tote.ui.theme.ToteShapes

/** Themed snackbar with an optional action (used for "Deleted · Undo"). */
@Composable
fun ToteSnackbar(message: String, actionLabel: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val c = Tuck.colors
    Row(
        modifier = modifier
            .shadow(12.dp, ToteShapes.pill, clip = false)
            .background(c.surfaceHigh, ToteShapes.pill)
            .padding(start = 20.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, style = Tuck.type.label, color = c.textPrimary, modifier = Modifier.weight(1f, fill = false))
        if (actionLabel != null) {
            PillButton(
                text = actionLabel,
                onClick = onAction,
                style = PillStyle.Ghost,
                modifier = Modifier.padding(start = 12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

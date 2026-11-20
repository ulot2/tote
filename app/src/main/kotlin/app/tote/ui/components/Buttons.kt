package app.tote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.tote.ui.theme.Tote
import app.tote.ui.theme.ToteShapes

enum class PillStyle { Primary, Secondary, Ghost, Danger }

/** Subtle press-in scale shared by tappable surfaces. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, pressed: Float = 0.97f): Modifier {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
) {
    val c = Tote.colors
    val (bg, fg, border) = when (style) {
        PillStyle.Primary -> Triple(c.accent, c.onAccent, null)
        PillStyle.Secondary -> Triple(c.surfaceRaised, c.textPrimary, null)
        PillStyle.Ghost -> Triple(Color.Transparent, c.textPrimary, BorderStroke(1.dp, c.outlineStrong))
        PillStyle.Danger -> Triple(Color.Transparent, c.danger, BorderStroke(1.dp, c.danger.copy(alpha = 0.5f)))
    }
    val bgAnimated by animateColorAsState(if (enabled) bg else c.surfaceRaised, label = "pillBg")
    val fgAnimated by animateColorAsState(if (enabled) fg else c.textTertiary, label = "pillFg")
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .pressScale(interaction)
            .defaultMinSize(minHeight = 48.dp)
            .clip(ToteShapes.pill)
            .background(bgAnimated)
            .then(if (border != null) Modifier.border(border, ToteShapes.pill) else Modifier)
            .clickable(interaction, ripple(), enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fgAnimated, modifier = Modifier.size(20.dp))
        Text(text, style = Tote.type.label, color = fgAnimated)
    }
}

/** Round icon button with a 48dp touch target. */
@Composable
fun ToteIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    tint: Color = Tote.colors.textPrimary,
) {
    val c = Tote.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, 0.92f)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(ToteShapes.pill)
            .then(if (filled) Modifier.background(c.surfaceRaised) else Modifier)
            .clickable(interaction, ripple(bounded = false, radius = 24.dp), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

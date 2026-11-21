package app.tote.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tote.core.model.SeedTemplate
import app.tote.core.model.SeedTemplates
import app.tote.ui.icons.EntryIconBadge
import app.tote.ui.theme.Tote

/** A tote bag with cards peeking out: the empty-vault illustration. Purely decorative. */
@Composable
fun ToteIllustration(modifier: Modifier = Modifier) {
    val c = Tote.colors
    Canvas(modifier.size(width = 200.dp, height = 168.dp).clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val u = w / 200f
        // Cards peeking out of the bag, drawn first so the bag body covers their bottoms.
        fun card(cx: Float, angle: Float, color: androidx.compose.ui.graphics.Color, lift: Float) {
            rotate(angle, pivot = Offset(cx, h * 0.62f)) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cx - 34 * u, h * 0.62f - (78 + lift) * u),
                    size = Size(68 * u, 96 * u),
                    cornerRadius = CornerRadius(12 * u),
                )
                drawRoundRect(
                    color = c.background.copy(alpha = 0.35f),
                    topLeft = Offset(cx - 22 * u, h * 0.62f - (64 + lift) * u),
                    size = Size(30 * u, 6 * u),
                    cornerRadius = CornerRadius(3 * u),
                )
            }
        }
        card(w * 0.36f, -14f, c.surfaceHigh, 0f)
        card(w * 0.64f, 12f, c.textSecondary, 4f)
        card(w * 0.5f, -2f, c.accent, 18f)

        // Handle.
        drawArc(
            color = c.outlineStrong,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.5f - 46 * u, h * 0.42f - 40 * u),
            size = Size(92 * u, 84 * u),
            style = Stroke(width = 9 * u, cap = StrokeCap.Round),
        )
        // Bag body.
        drawRoundRect(
            color = c.surfaceRaised,
            topLeft = Offset(w * 0.5f - 76 * u, h * 0.5f),
            size = Size(152 * u, h * 0.5f),
            cornerRadius = CornerRadius(22 * u),
        )
        drawRoundRect(
            color = c.outlineStrong,
            topLeft = Offset(w * 0.5f - 76 * u, h * 0.5f),
            size = Size(152 * u, h * 0.5f),
            cornerRadius = CornerRadius(22 * u),
            style = Stroke(width = 1.5f * u),
        )
        // Stitch line.
        drawLine(
            color = c.outlineStrong,
            start = Offset(w * 0.5f - 58 * u, h * 0.5f + 18 * u),
            end = Offset(w * 0.5f + 58 * u, h * 0.5f + 18 * u),
            strokeWidth = 2 * u,
            cap = StrokeCap.Round,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6 * u, 6 * u)),
        )
    }
}

/** Empty vault: illustration, one clear action, and one-tap starter templates. */
@Composable
fun EmptyVault(
    onAdd: () -> Unit,
    onTemplate: (SeedTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Tote.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ToteIllustration()
        Text(
            "Your tote is empty",
            style = Tote.type.headline,
            color = c.textPrimary,
            modifier = Modifier.padding(top = 28.dp),
        )
        Text(
            "Keep the links and answers you type into every form. Tap one to copy it.",
            style = Tote.type.body,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp),
        )
        PillButton("Add your first entry", onClick = onAdd, icon = Icons.Rounded.Add, modifier = Modifier.padding(top = 24.dp))

        Text(
            "OR START FROM",
            style = Tote.type.overline,
            color = c.textTertiary,
            modifier = Modifier.padding(top = 36.dp, bottom = 12.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SeedTemplates.all.forEach { t ->
                ToteChip(
                    text = t.label,
                    selected = false,
                    onClick = { onTemplate(t) },
                    role = androidx.compose.ui.semantics.Role.Button,
                    leading = { EntryIconBadge(t.icon, size = 22.dp) },
                )
            }
        }
    }
}

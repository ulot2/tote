package app.tote.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Tuck's own glyphs, drawn on a 24dp grid with 1.8dp rounded strokes to sit beside Material Rounded icons. */
object ToteIcons {

    /** The Tuck mark: a card tucked into a pocket. One colour; the card is drawn at 45%. */
    val TuckMark: ImageVector by lazy {
        ImageVector.Builder(name = "TuckMark", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addGroup(name = "card", rotate = -12f, pivotX = 12f, pivotY = 8.5f)
            .path(fill = SolidColor(Color.Black), fillAlpha = 0.45f) {
                moveTo(9f, 2.5f); lineTo(15f, 2.5f); quadTo(17f, 2.5f, 17f, 4.5f); lineTo(17f, 12.5f)
                quadTo(17f, 14.5f, 15f, 14.5f); lineTo(9f, 14.5f); quadTo(7f, 14.5f, 7f, 12.5f); lineTo(7f, 4.5f)
                quadTo(7f, 2.5f, 9f, 2.5f); close()
            }
            .clearGroup()
            .path(fill = SolidColor(Color.Black)) {
                moveTo(3.5f, 10.5f); lineTo(20.5f, 10.5f); lineTo(20.5f, 16f)
                arcTo(6f, 6f, 0f, false, true, 14.5f, 22f); lineTo(9.5f, 22f)
                arcTo(6f, 6f, 0f, false, true, 3.5f, 16f); close()
            }
            .build()
    }

    /** A form field with a line of text and a typing cursor: "fill this field". Used for autofill. */
    val FieldFill: ImageVector by lazy {
        ImageVector.Builder(name = "FieldFill", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Field outline, rounded like Tuck's inputs.
                moveTo(6f, 6.5f)
                lineTo(18f, 6.5f)
                arcTo(3f, 3f, 0f, false, true, 21f, 9.5f)
                lineTo(21f, 14.5f)
                arcTo(3f, 3f, 0f, false, true, 18f, 17.5f)
                lineTo(6f, 17.5f)
                arcTo(3f, 3f, 0f, false, true, 3f, 14.5f)
                lineTo(3f, 9.5f)
                arcTo(3f, 3f, 0f, false, true, 6f, 6.5f)
                close()
                // Filled-in text.
                moveTo(6.8f, 12f)
                lineTo(11.6f, 12f)
                // Cursor.
                moveTo(14.6f, 9.4f)
                lineTo(14.6f, 14.6f)
            }
            .build()
    }
}

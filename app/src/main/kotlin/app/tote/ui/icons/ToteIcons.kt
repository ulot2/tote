package app.tote.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Tote's own glyphs, drawn on a 24dp grid with 1.8dp rounded strokes to sit beside Material Rounded icons. */
object ToteIcons {

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
                // Field outline, rounded like Tote's inputs.
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

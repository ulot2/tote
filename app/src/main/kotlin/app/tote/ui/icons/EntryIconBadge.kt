package app.tote.ui.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryIconPalette
import app.tote.ui.theme.Geist
import app.tote.ui.theme.Tuck
import app.tote.ui.theme.ToteShapes

private fun brandVector(name: String, pathData: String): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        .build()

private object BrandVectors {
    val GitHub by lazy { brandVector("GitHub", BrandPaths.GitHub) }
    val X by lazy { brandVector("X", BrandPaths.X) }
    val Behance by lazy { brandVector("Behance", BrandPaths.Behance) }
    val Dribbble by lazy { brandVector("Dribbble", BrandPaths.Dribbble) }
    val GoogleDrive by lazy { brandVector("GoogleDrive", BrandPaths.GoogleDrive) }
}

/** Human name of an icon, used for content descriptions and the icon picker. */
fun EntryIcon.displayName(): String = when (this) {
    EntryIcon.LINKEDIN -> "LinkedIn"
    EntryIcon.GITHUB -> "GitHub"
    EntryIcon.X -> "X"
    EntryIcon.BEHANCE -> "Behance"
    EntryIcon.DRIBBBLE -> "Dribbble"
    EntryIcon.MEDIUM -> "Medium"
    EntryIcon.GOOGLE_DRIVE -> "Google Drive"
    EntryIcon.GLOBE -> "Website"
    EntryIcon.MAIL -> "Email"
    EntryIcon.PHONE -> "Phone"
    EntryIcon.DOC -> "Document"
    EntryIcon.LOCATION -> "Location"
    EntryIcon.TEXT -> "Text"
}

/** [vector] null means a text monogram ([monogram]) is drawn instead. */
private data class BadgeStyle(val container: Color, val content: Color, val vector: ImageVector?, val monogram: String = "")

@Composable
private fun styleFor(icon: EntryIcon): BadgeStyle {
    val c = Tuck.colors
    val brand = EntryIconPalette.brand(icon, c.isDark)
    // Brand colours are shared with the autofill dropdown; neutral icons follow the live theme.
    val container = brand?.let { Color(it.container) } ?: c.surfaceHigh
    val content = brand?.let { Color(it.content) } ?: c.textPrimary
    EntryIconPalette.monogram(icon)?.let { return BadgeStyle(container, content, null, it) }
    val vector = when (icon) {
        EntryIcon.GITHUB -> BrandVectors.GitHub
        EntryIcon.X -> BrandVectors.X
        EntryIcon.BEHANCE -> BrandVectors.Behance
        EntryIcon.DRIBBBLE -> BrandVectors.Dribbble
        EntryIcon.GOOGLE_DRIVE -> BrandVectors.GoogleDrive
        EntryIcon.GLOBE -> Icons.Rounded.Language
        EntryIcon.MAIL -> Icons.Rounded.MailOutline
        EntryIcon.PHONE -> Icons.Rounded.Call
        EntryIcon.DOC -> Icons.Rounded.Description
        EntryIcon.LOCATION -> Icons.Rounded.Place
        else -> Icons.AutoMirrored.Rounded.Notes
    }
    return BadgeStyle(container, content, vector)
}

/**
 * The rounded icon tile used on cards, in the editor and in autofill previews.
 * Decorative by default ([contentDescription] null) because the label next to it says the same.
 */
@Composable
fun EntryIconBadge(
    icon: EntryIcon,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    contentDescription: String? = null,
    /** Circle instead of rounded square (pinned pills). */
    round: Boolean = false,
) {
    val style = styleFor(icon)
    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { if (contentDescription != null) this.contentDescription = contentDescription }
            .background(style.container, if (round) ToteShapes.pill else ToteShapes.badge),
        contentAlignment = Alignment.Center,
    ) {
        if (style.vector != null) {
            Icon(
                imageVector = style.vector,
                contentDescription = null,
                tint = style.content,
                modifier = Modifier.size(size * if (icon.isBrand) 0.46f else 0.5f),
            )
        } else {
            // Monogram for brands whose mark doesn't read at badge size.
            Text(
                text = style.monogram,
                color = style.content,
                style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.42f).sp, letterSpacing = (-0.5).sp),
            )
        }
    }
}

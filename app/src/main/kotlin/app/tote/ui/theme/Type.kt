package app.tote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.tote.R

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
)

private val trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

/** Type scale (Plan §8.2): display, title, body, label, mono. */
@Immutable
data class ToteType(
    val display: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.035).em, lineHeightStyle = trim),
    val headline: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.025).em),
    val title: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em),
    val titleSmall: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.005).em),
    val body: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    val bodySmall: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    val label: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.005.em),
    val overline: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.08.em),
    val mono: TextStyle = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    val monoLarge: TextStyle = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
)

val LocalToteType = staticCompositionLocalOf { ToteType() }

/** Material typography mapped onto Tuck's faces, so any M3 component we do use matches. */
internal fun ToteType.toMaterial(): Typography = Typography(
    displayLarge = display, displayMedium = display, displaySmall = headline,
    headlineLarge = headline, headlineMedium = headline, headlineSmall = title,
    titleLarge = title, titleMedium = titleSmall, titleSmall = label,
    bodyLarge = body, bodyMedium = bodySmall, bodySmall = bodySmall,
    labelLarge = label, labelMedium = label, labelSmall = overline,
)

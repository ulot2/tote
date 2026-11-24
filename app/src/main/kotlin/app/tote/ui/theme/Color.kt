package app.tote.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.tote.core.data.settings.Accent

/**
 * Tuck's colour tokens (Plan §8.2, redesign). Warm near-black and off-white grounds, one accent
 * the user picks (Iris by default).
 *
 * Contrast (WCAG AA) is checked for: [textPrimary]/[textSecondary] on every surface level,
 * [textTertiary] on [background] and [surface], [accentContent] on [background]/[surface], and
 * [onAccent] on [accent], for every accent in both themes.
 */
@Immutable
data class ToteColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val outlineStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    /** Fill colour: primary buttons, selected controls, the "Copied" pill, the app mark. */
    val accent: Color,
    /** Text and icons placed on [accent]. */
    val onAccent: Color,
    /** Accent used for text/icons directly on a surface. */
    val accentContent: Color,
    /** Low-emphasis translucent accent for selected backgrounds. */
    val accentSoft: Color,
    /** Opaque accent-tinted surface: the just-copied row, tinted pills, the autofill hero. */
    val accentTint: Color,
    /** Border for [accentTint] surfaces. */
    val accentTintBorder: Color,
    val danger: Color,
    val warning: Color,
    val scrim: Color,
)

/** One accent's values in each theme. */
data class AccentSpec(
    val darkFill: Color,
    val darkOn: Color,
    val lightFill: Color,
    val lightOn: Color,
    /** Accent text on the light background (the fill is too pale to read there). */
    val lightContent: Color,
)

object TotePalette {

    /** Every accent's fills and text colours, checked for AA in both themes. */
    val accents: Map<Accent, AccentSpec> = mapOf(
        Accent.IRIS to AccentSpec(Color(0xFFA99BFF), Color(0xFF170F3D), Color(0xFFA99BFF), Color(0xFF170F3D), Color(0xFF5644C8)),
        Accent.EMBER to AccentSpec(Color(0xFFFF7A59), Color(0xFF2B0D04), Color(0xFFFF7A59), Color(0xFF2B0D04), Color(0xFFB4401F)),
        Accent.MARIGOLD to AccentSpec(Color(0xFFF7B538), Color(0xFF2A1B00), Color(0xFFF7B538), Color(0xFF2A1B00), Color(0xFF8A5A00)),
        Accent.MINT to AccentSpec(Color(0xFF4FE0B0), Color(0xFF04291D), Color(0xFF4FE0B0), Color(0xFF04291D), Color(0xFF0B7452)),
        Accent.LIME to AccentSpec(Color(0xFFC8F23C), Color(0xFF16200A), Color(0xFFC8F23C), Color(0xFF16200A), Color(0xFF4A6300)),
        // No colour: cream on black, ink on paper. The brand badges become the colour.
        Accent.BONE to AccentSpec(Color(0xFFF2EDE3), Color(0xFF16150F), Color(0xFF1B1A16), Color(0xFFF6F3EC), Color(0xFF1B1A16)),
    )

    /** Brand iris, used where the user's accent doesn't apply (launcher icon, autofill mark). */
    val Iris = Color(0xFFA99BFF)
    val IrisInk = Color(0xFF170F3D)

    private val darkBase = ToteColors(
        isDark = true,
        background = Color(0xFF0F0E0C),
        surface = Color(0xFF191815),
        surfaceRaised = Color(0xFF22211D),
        surfaceHigh = Color(0xFF2C2B26),
        outline = Color(0xFF2E2D28),
        outlineStrong = Color(0xFF45433C),
        textPrimary = Color(0xFFF5F2EA),
        textSecondary = Color(0xFFA9A396),
        textTertiary = Color(0xFF8C877B),
        accent = Iris,
        onAccent = IrisInk,
        accentContent = Iris,
        accentSoft = Iris.copy(alpha = 0.16f),
        accentTint = Color(0xFF241F3D),
        accentTintBorder = Color(0xFF3C3470),
        danger = Color(0xFFFF7A6B),
        warning = Color(0xFFE8B34A),
        scrim = Color(0xCC000000),
    )

    private val lightBase = ToteColors(
        isDark = false,
        background = Color(0xFFF6F3EC),
        surface = Color(0xFFFFFDF8),
        surfaceRaised = Color(0xFFEEEAE0),
        surfaceHigh = Color(0xFFE5E0D4),
        outline = Color(0xFFE2DDD2),
        outlineStrong = Color(0xFFCBC5B8),
        textPrimary = Color(0xFF1B1A16),
        textSecondary = Color(0xFF5C584F),
        textTertiary = Color(0xFF6F6A60),
        accent = Iris,
        onAccent = IrisInk,
        accentContent = Color(0xFF5644C8),
        accentSoft = Iris.copy(alpha = 0.28f),
        accentTint = Color(0xFFE6E1FF),
        accentTintBorder = Color(0xFFC9C0FF),
        danger = Color(0xFFB3261E),
        warning = Color(0xFF8A5A00),
        scrim = Color(0x99000000),
    )

    fun colors(dark: Boolean, accent: Accent = Accent.IRIS): ToteColors {
        val spec = accents.getValue(accent)
        return if (dark) {
            darkBase.copy(
                accent = spec.darkFill,
                onAccent = spec.darkOn,
                accentContent = spec.darkFill,
                accentSoft = spec.darkFill.copy(alpha = 0.16f),
                accentTint = spec.darkFill.copy(alpha = 0.14f).over(darkBase.surface),
                accentTintBorder = spec.darkFill.copy(alpha = 0.38f).over(darkBase.surface),
            )
        } else {
            lightBase.copy(
                accent = spec.lightFill,
                onAccent = spec.lightOn,
                accentContent = spec.lightContent,
                accentSoft = spec.lightContent.copy(alpha = 0.14f),
                accentTint = spec.lightContent.copy(alpha = 0.12f).over(lightBase.background),
                accentTintBorder = spec.lightContent.copy(alpha = 0.32f).over(lightBase.background),
            )
        }
    }

    val Dark = colors(dark = true)
    val Light = colors(dark = false)

    /** Material You variant (opt-in in Settings): keep Tuck's structure, borrow the wallpaper hues. */
    fun fromDynamic(scheme: ColorScheme, isDark: Boolean): ToteColors {
        val base = colors(isDark)
        return base.copy(
            background = scheme.surface,
            surface = scheme.surfaceContainerLow,
            surfaceRaised = scheme.surfaceContainer,
            surfaceHigh = scheme.surfaceContainerHighest,
            outline = scheme.outlineVariant,
            outlineStrong = scheme.outline,
            textPrimary = scheme.onSurface,
            textSecondary = scheme.onSurfaceVariant,
            textTertiary = scheme.onSurfaceVariant,
            accent = scheme.primaryContainer,
            onAccent = scheme.onPrimaryContainer,
            accentContent = scheme.primary,
            accentSoft = scheme.primary.copy(alpha = 0.16f),
            accentTint = scheme.secondaryContainer,
            accentTintBorder = scheme.outlineVariant,
            danger = scheme.error,
        )
    }
}

/** Alpha-composites this (translucent) colour over an opaque [base], giving an opaque colour. */
fun Color.over(base: Color) = Color(
    red = red * alpha + base.red * (1 - alpha),
    green = green * alpha + base.green * (1 - alpha),
    blue = blue * alpha + base.blue * (1 - alpha),
    alpha = 1f,
)

val LocalToteColors = staticCompositionLocalOf { TotePalette.Dark }

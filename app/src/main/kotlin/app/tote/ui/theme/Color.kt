package app.tote.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tote's colour tokens (Plan §8.2). Electric lime is the single accent.
 *
 * Contrast (WCAG AA) is verified for: [textPrimary]/[textSecondary] on every surface level,
 * [textTertiary] on [background] and [surface], [accentContent] on [background]/[surface],
 * and [onAccent] on [accent].
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
    /** Fill colour: FAB, primary buttons, copy glow, selected chips. */
    val accent: Color,
    /** Text and icons placed on [accent]. */
    val onAccent: Color,
    /** Accent used for text/icons directly on a surface (lime is unreadable on light backgrounds). */
    val accentContent: Color,
    /** Low-emphasis accent tint for selected backgrounds. */
    val accentSoft: Color,
    val danger: Color,
    val warning: Color,
    val scrim: Color,
)

object TotePalette {
    val Lime = Color(0xFFC8F23C)
    val LimeDeep = Color(0xFF4A6300)
    val Ink = Color(0xFF16200A)

    val Dark = ToteColors(
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
        accent = Lime,
        onAccent = Ink,
        accentContent = Lime,
        accentSoft = Lime.copy(alpha = 0.14f),
        danger = Color(0xFFFF7A6B),
        warning = Color(0xFFE8B34A),
        scrim = Color(0xCC000000),
    )

    val Light = ToteColors(
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
        accent = Lime,
        onAccent = Ink,
        accentContent = LimeDeep,
        accentSoft = Color(0xFFC8F23C).copy(alpha = 0.32f),
        danger = Color(0xFFB3261E),
        warning = Color(0xFF8A5A00),
        scrim = Color(0x99000000),
    )

    /** Material You variant (opt-in in Settings): keep Tote's structure, borrow the wallpaper hues. */
    fun fromDynamic(scheme: ColorScheme, isDark: Boolean): ToteColors {
        val base = if (isDark) Dark else Light
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
            danger = scheme.error,
        )
    }
}

val LocalToteColors = staticCompositionLocalOf { TotePalette.Dark }
